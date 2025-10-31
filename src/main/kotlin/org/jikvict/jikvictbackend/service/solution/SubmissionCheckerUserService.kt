package org.jikvict.jikvictbackend.service.solution

import com.fasterxml.jackson.databind.ObjectMapper
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.entity.isClosed
import org.jikvict.jikvictbackend.model.dto.PendingSubmissionDto
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.service.UserDetailsServiceImpl
import org.jikvict.jikvictbackend.service.assignment.AssignmentInfoUserService
import org.jikvict.jikvictbackend.service.assignment.AssignmentResultService
import org.jikvict.problems.exception.contract.ServiceException
import org.jikvict.testing.model.TestSuiteResult
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SubmissionCheckerUserService(
    private val assignmentResultService: AssignmentResultService,
    private val assignmentRepository: AssignmentRepository,
    private val submissionCheckerService: SubmissionCheckerService,
    private val assignmentUserService: AssignmentInfoUserService,
    private val userDetailsService: UserDetailsServiceImpl,
    private val taskStatusRepository: TaskStatusRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    suspend fun checkSubmission(
        assignmentId: Long,
        solutionBytes: ByteArray,
        user: User,
        isActive: () -> Boolean,
    ): TestSuiteResult {
        val assignment = assignmentUserService.getAssignmentByIdForUser(assignmentId, user)
        checkIsNotClosed(assignment.id)
        checkUserCanSubmit(user, assignment.id)
        checkForAttemptsLimit(assignment.id, user)
        return submissionCheckerService.checkSubmission(assignment, solutionBytes, isActive)
    }

    fun checkForAttemptsLimit(
        assignmentId: Long,
        user: User,
    ) {
        val attempts = assignmentResultService.getUsedAttempts(assignmentId, user)
        val maxAttempts =
            assignmentRepository
                .findById(assignmentId)
                .orElseThrow {
                    ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $assignmentId not found")
                }.maximumAttempts
        require(attempts < maxAttempts) {
            throw ServiceException(HttpStatus.FORBIDDEN, "You have reached the maximum number of attempts")
        }
    }

    fun checkIsNotClosed(assignmentId: Long) {
        val assignment =
            assignmentRepository.findById(assignmentId).orElseThrow {
                ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $assignmentId not found")
            }
        require(!assignment.isClosed) {
            throw ServiceException(HttpStatus.FORBIDDEN, "Assignment with ID $assignmentId is closed")
        }
    }

    @Transactional
    fun checkUserCanSubmit(
        user: User,
        assignmentId: Long,
    ) {
        val assignment =
            assignmentRepository.findById(assignmentId).orElseThrow {
                ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $assignmentId not found")
            }
        require(assignment.assignmentGroups.map { it.id }.any { assignmentGroup -> assignmentGroup in user.assignmentGroups.map { it.id } }) {
            throw ServiceException(
                HttpStatus.FORBIDDEN,
                "${user.username} is not allowed to submit to assignment with ID $assignmentId",
            )
        }
    }

    fun getPendingSubmissions(): PendingSubmissionDto? {
        val user = userDetailsService.getCurrentUser()
        val tasks = taskStatusRepository.findAllByUserAndTaskTypeAndStatus(user, "SOLUTION_VERIFICATION", PendingStatus.PENDING)
        val task = tasks.firstOrNull() ?: return null
        val assignmentId = objectMapper.readTree(task.parameters)?.get("assignmentId")?.asLong()
        val createdAt = task.createdAt
        return assignmentId?.let {
            PendingSubmissionDto(task.id, it, createdAt)
        }
    }

    fun cancelPendingSubmission(taskId: Long) {
        val user = userDetailsService.getCurrentUser()
        val task = taskStatusRepository.findById(taskId).orElseThrow { ServiceException(HttpStatus.NOT_FOUND, "Task with ID $taskId not found") }
        if (task.user.id != user.id) {
            throw ServiceException(HttpStatus.FORBIDDEN, "You do not have permission to access this task")
        }
        if (task.status == PendingStatus.PENDING) {
            task.status = PendingStatus.CANCELLED
            task.message = "Submission cancelled by user"
            taskStatusRepository.save(task)
        }
    }
}
