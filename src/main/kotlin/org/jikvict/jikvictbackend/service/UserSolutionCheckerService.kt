package org.jikvict.jikvictbackend.service

import org.jikvict.jikvictbackend.entity.AssignmentResult
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.AssignmentResultRepository
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class UserSolutionCheckerService(
    private val userDetailsService: UserDetailsServiceImpl,
    private val taskStatusRepository: TaskStatusRepository,
    private val assignmentResultRepository: AssignmentResultRepository,
    private val assignmentRepository: AssignmentRepository,
) {
    fun rejectMultipleSubmissions() {
        val currentUser = userDetailsService.getCurrentUser()
        val task =
            taskStatusRepository.findByUser(currentUser).firstOrNull {
                it.status == PendingStatus.PENDING
            }
        require(task == null) {
            "You already have a pending task"
        }
    }

    fun getResults(
        assignmentId: Long,
        user: User,
    ): List<AssignmentResult> {
        val assignment =
            assignmentRepository.findById(assignmentId).orElseThrow {
                ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $assignmentId not found")
            }
        val submissions = assignmentResultRepository.findByUserAndAssignment(user, assignment)
        return submissions
    }

    fun getUsedAttempts(
        assignmentId: Long,
        user: User,
    ): Int {
        val submissions = getResults(assignmentId, user)
        val attempts = submissions.size
        return attempts
    }

    fun checkForAttemptsLimit(
        assignmentId: Long,
        user: User,
    ) {
        val attempts = getUsedAttempts(assignmentId, user)
        val maxAttempts =
            assignmentRepository
                .findById(assignmentId)
                .orElseThrow {
                    ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $assignmentId not found")
                }.maximumAttempts
        require(attempts < maxAttempts) {
            "You have reached the maximum number of attempts for this assignment"
        }
    }

    fun checkIsNotClosed(assignmentId: Long) {
        val assignment =
            assignmentRepository.findById(assignmentId).orElseThrow {
                ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $assignmentId not found")
            }
        require(assignment.isClosed == false) {
            "Assignment with ID $assignmentId is closed"
        }
    }
}
