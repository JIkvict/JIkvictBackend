package org.jikvict.jikvictbackend.service.plagiarism

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.model.request.PlagiarismCheckParameters
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.model.response.PlagiarismCheckSummaryResponse
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.PlagiarismCheckResultRepository
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.service.UserDetailsServiceImpl
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PlagiarismCheckService(
    private val log: Logger,
    private val assignmentRepository: AssignmentRepository,
    private val plagiarismCheckResultRepository: PlagiarismCheckResultRepository,
    private val taskStatusRepository: TaskStatusRepository,
    private val userDetailsService: UserDetailsServiceImpl,
    private val plagiarismCheckRunner: PlagiarismCheckRunner,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        const val TASK_TYPE = "PLAGIARISM_CHECK"
    }

    fun startCheck(assignmentId: Long, parameters: PlagiarismCheckParameters?): Long {
        assignmentRepository.findById(assignmentId).orElseThrow {
            ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $assignmentId not found")
        }
        val currentUser = userDetailsService.getCurrentUser()
        val configJson = parameters?.let { objectMapper.writeValueAsString(it) }
        val taskStatus = TaskStatus().apply {
            taskType = TASK_TYPE
            status = PendingStatus.PENDING
            user = currentUser
            this.parameters = assignmentId.toString()
            this.configuration = configJson
            createdAt = LocalDateTime.now()
        }
        val saved = taskStatusRepository.save(taskStatus)
        log.info("Queued plagiarism check for assignment=$assignmentId taskId=${saved.id} config=$configJson")
        plagiarismCheckRunner.run(assignmentId, saved.id, parameters)
        return saved.id
    }

    fun getTaskStatus(taskId: Long): TaskStatus =
        taskStatusRepository.findById(taskId).orElseThrow {
            ServiceException(HttpStatus.NOT_FOUND, "Task $taskId not found")
        }

    @Transactional(readOnly = true)
    fun listChecksForAssignment(assignmentId: Long): List<PlagiarismCheckSummaryResponse> {
        assignmentRepository.findById(assignmentId).orElseThrow {
            ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $assignmentId not found")
        }
        return taskStatusRepository
            .findAllByTaskTypeAndParametersOrderByCreatedAtDesc(TASK_TYPE, assignmentId.toString())
            .map {
                PlagiarismCheckSummaryResponse(
                    taskId = it.id,
                    assignmentId = assignmentId,
                    startedAt = it.createdAt,
                    initiatedBy = it.user.userNameField,
                    status = it.status,
                    parameters = it.configuration?.let { json ->
                        runCatching { objectMapper.readValue(json, PlagiarismCheckParameters::class.java) }.getOrNull()
                    },
                )
            }
    }

    @Transactional(readOnly = true)
    fun loadResultZip(resultId: Long): ByteArray {
        val result = plagiarismCheckResultRepository.findById(resultId).orElseThrow {
            ServiceException(HttpStatus.NOT_FOUND, "Result $resultId not found")
        }
        return result.resultZip ?: throw ServiceException(HttpStatus.NOT_FOUND, "Result zip not available")
    }
}
