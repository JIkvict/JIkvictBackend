package org.jikvict.jikvictbackend.service.assignment

import org.jikvict.jikvictbackend.entity.AssignmentResult
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.AssignmentResultRepository
import org.jikvict.problems.exception.contract.ServiceException
import org.jikvict.testing.model.TestSuiteResult
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AssignmentResultService(
    private val assignmentResultRepository: AssignmentResultRepository,
    private val assignmentRepository: AssignmentRepository,
) {
    @Transactional
    fun handleAssignmentResult(
        assignmentId: Long,
        result: TestSuiteResult?,
        user: User,
        solutionBytes: ByteArray,
        logs: String? = null,
    ): AssignmentResult {
        val assignment =
            assignmentRepository.findById(assignmentId).orElseThrow {
                ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $assignmentId not found")
            }
        val resultEntity =
            AssignmentResult().apply {
                this.points = result?.totalEarnedPoints ?: 0
                this.testSuiteResult = result
                this.user = user
                this.assignment = assignment
                this.timeStamp = LocalDateTime.now()
                this.zipFile = solutionBytes
                this.logs = logs
            }
        assignmentResultRepository.save(resultEntity)
        return resultEntity
    }

    @Transactional
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

    @Transactional
    fun getUsedAttempts(
        assignmentId: Long,
        user: User,
    ): Int {
        val submissions = getResults(assignmentId, user)
        val attempts = submissions.filter { it.testSuiteResult != null }.size
        return attempts
    }
}
