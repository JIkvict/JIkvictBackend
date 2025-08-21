package org.jikvict.jikvictbackend.service

import org.jikvict.jikvictbackend.entity.AssignmentResult
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.AssignmentResultRepository
import org.jikvict.problems.exception.contract.ServiceException
import org.jikvict.testing.model.TestSuiteResult
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrElse

@Service
class AssignmentResultService(
    private val assignmentResultRepository: AssignmentResultRepository,
    private val assignmentRepository: AssignmentRepository,
) {
    fun handleAssignmentResult(
        result: TestSuiteResult,
        assignmentResultId: Long,
    ): AssignmentResult {
        val assignmentResult =
            assignmentResultRepository.findById(assignmentResultId).orElseThrow {
                ServiceException(
                    HttpStatus.NOT_FOUND,
                    "Assignment result with ID $assignmentResultId not found",
                )
            }
        val resultEntity =
            assignmentResult.apply {
                this.points = result.totalEarnedPoints
                this.testSuiteResult = result
            }
        assignmentResultRepository.save(resultEntity)
        return resultEntity
    }

    fun createRawSubmission(
        assignmentId: Long,
        user: User,
    ): AssignmentResult {
        val assignment =
            assignmentRepository.findById(assignmentId).getOrElse {
                throw ServiceException(
                    HttpStatus.NOT_FOUND,
                    "Assignment with ID $assignmentId not found",
                )
            }!!
        val assignmentResult =
            AssignmentResult().apply {
                this.user = user
                this.assignment = assignment
                this.timeStamp = LocalDateTime.now()
                this.points = 0
                this.testSuiteResult = null
            }
        return assignmentResultRepository.save(assignmentResult)
    }
}
