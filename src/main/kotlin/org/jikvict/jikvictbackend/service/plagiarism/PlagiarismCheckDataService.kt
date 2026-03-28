package org.jikvict.jikvictbackend.service.plagiarism

import org.jikvict.jikvictbackend.entity.PlagiarismCheckResult
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.AssignmentResultRepository
import org.jikvict.jikvictbackend.repository.PlagiarismCheckResultRepository
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PlagiarismCheckDataService(
    private val assignmentRepository: AssignmentRepository,
    private val assignmentResultRepository: AssignmentResultRepository,
    private val plagiarismCheckResultRepository: PlagiarismCheckResultRepository,
    private val taskStatusRepository: TaskStatusRepository,
) {
    @Transactional(readOnly = true)
    fun loadSubmissions(assignmentId: Long): List<Pair<String, ByteArray>> {
        val assignment = assignmentRepository.findById(assignmentId).orElseThrow {
            ServiceException(HttpStatus.NOT_FOUND, "Assignment $assignmentId not found")
        }
        return assignmentResultRepository.findLatestSubmissionPerUserForAssignment(assignment)
            .mapNotNull { result ->
                result.zipFile?.let { result.user.userNameField to it }
            }
    }

    @Transactional
    fun saveResult(assignmentId: Long, taskId: Long, reportBytes: ByteArray) {
        val assignment = assignmentRepository.findById(assignmentId).orElseThrow {
            ServiceException(HttpStatus.NOT_FOUND, "Assignment $assignmentId not found")
        }
        val checkResult = PlagiarismCheckResult().apply {
            this.assignment = assignment
            this.resultZip = reportBytes
        }
        val saved = plagiarismCheckResultRepository.save(checkResult)

        val taskStatus = taskStatusRepository.findById(taskId).orElseThrow {
            IllegalStateException("TaskStatus $taskId not found")
        }
        taskStatus.status = PendingStatus.DONE
        taskStatus.resultId = saved.id
        taskStatus.completedAt = LocalDateTime.now()
        taskStatusRepository.save(taskStatus)
    }

    fun failTask(taskId: Long, message: String) {
        taskStatusRepository.findById(taskId).ifPresent { taskStatus ->
            taskStatus.status = PendingStatus.FAILED
            taskStatus.message = message
            taskStatus.completedAt = LocalDateTime.now()
            taskStatusRepository.save(taskStatus)
        }
    }
}
