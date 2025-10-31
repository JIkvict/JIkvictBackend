package org.jikvict.jikvictbackend.service.solution

import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.service.assignment.AssignmentService
import org.jikvict.testing.model.TestSuiteResult
import org.springframework.stereotype.Service

@Service
class SubmissionCheckerService(
    private val solutionChecker: SolutionChecker,
    private val assignmentService: AssignmentService,
) {
    suspend fun checkSubmission(
        assignment: Assignment,
        solutionBytes: ByteArray,
        isActive: () -> Boolean
    ): TestSuiteResult {
        val hiddenFiles = assignmentService.getHiddenFilesForTask(assignment.taskId)
        return solutionChecker.checkSolution(solutionBytes, hiddenFiles, assignment, isActive)
    }
}
