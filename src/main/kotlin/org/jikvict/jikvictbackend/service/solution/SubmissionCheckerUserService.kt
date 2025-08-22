package org.jikvict.jikvictbackend.service.solution

import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.repository.AssignmentRepository
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
) {
    @Transactional
    suspend fun checkSubmission(
        assignmentId: Long,
        solutionBytes: ByteArray,
        user: User,
    ): TestSuiteResult {
        val assignment = assignmentUserService.getAssignmentByIdForUser(assignmentId, user)
        checkIsNotClosed(assignment.id)
        checkUserCanSubmit(user, assignment.id)
        checkForAttemptsLimit(assignment.id, user)
        return submissionCheckerService.checkSubmission(assignment, solutionBytes)
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
        require(assignment.isClosed == false) {
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
}
