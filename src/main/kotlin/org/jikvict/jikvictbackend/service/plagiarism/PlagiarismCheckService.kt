package org.jikvict.jikvictbackend.service.plagiarism

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.model.response.PendingStatus
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
) {
    companion object {
        const val TASK_TYPE = "PLAGIARISM_CHECK"
    }

    fun startCheck(assignmentId: Long): Long {
        assignmentRepository.findById(assignmentId).orElseThrow {
            ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $assignmentId not found")
        }
        val currentUser = userDetailsService.getCurrentUser()
        val taskStatus = TaskStatus().apply {
            taskType = TASK_TYPE
            status = PendingStatus.PENDING
            user = currentUser
            parameters = assignmentId.toString()
            createdAt = LocalDateTime.now()
        }
        val saved = taskStatusRepository.save(taskStatus)
        log.info("Queued plagiarism check for assignment=$assignmentId taskId=${saved.id}")
        plagiarismCheckRunner.run(assignmentId, saved.id)
        return saved.id
    }

    fun getTaskStatus(taskId: Long): TaskStatus =
        taskStatusRepository.findById(taskId).orElseThrow {
            ServiceException(HttpStatus.NOT_FOUND, "Task $taskId not found")
        }

    @Transactional(readOnly = true)
    fun loadResultZip(resultId: Long): ByteArray {
        val result = plagiarismCheckResultRepository.findById(resultId).orElseThrow {
            ServiceException(HttpStatus.NOT_FOUND, "Result $resultId not found")
        }
        return result.resultZip ?: throw ServiceException(HttpStatus.NOT_FOUND, "Result zip not available")
    }
}
