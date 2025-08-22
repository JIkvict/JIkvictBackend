package org.jikvict.jikvictbackend.service

import org.jikvict.jikvictbackend.entity.AssignmentResult
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.AssignmentResultRepository
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserSolutionCheckerService(
    private val assignmentResultRepository: AssignmentResultRepository,
    private val assignmentRepository: AssignmentRepository,
) {
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

    @Transactional
    fun checkUserCanSubmit(user: User, assignmentId: Long) {
        val assignment = assignmentRepository.findById(assignmentId).orElseThrow {
            ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $assignmentId not found")
        }
        require(assignment.assignmentGroups.map { it.id }.any { assignmentGroup -> assignmentGroup in user.assignmentGroups.map { it.id } }) {
            "You are not allowed to submit to this assignment"
        }
    }
}
