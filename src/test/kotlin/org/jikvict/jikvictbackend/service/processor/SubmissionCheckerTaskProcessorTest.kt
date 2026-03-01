package org.jikvict.jikvictbackend.service.processor

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.exception.SolutionCheckingException
import org.jikvict.jikvictbackend.model.dto.VerificationTaskDto
import org.jikvict.jikvictbackend.model.queue.VerificationTaskMessage
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.UserRepository
import org.jikvict.jikvictbackend.service.assignment.AssignmentResultService
import org.jikvict.jikvictbackend.service.queue.SubmissionCheckerTaskQueueService
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.jikvict.jikvictbackend.service.solution.SubmissionCheckerUserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SubmissionCheckerTaskProcessorTest {
    private val taskQueueService = mockk<SubmissionCheckerTaskQueueService>(relaxed = true)
    private val log = mockk<Logger>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    private val assignmentResultService = mockk<AssignmentResultService>(relaxed = true)
    private val assignmentRepository = mockk<AssignmentRepository>()
    private val userSolutionChecker = mockk<SubmissionCheckerUserService>()
    private val taskRegistry = mockk<TaskRegistry>(relaxed = true)

    private val processor = SubmissionCheckerTaskProcessor(
        taskQueueService,
        log,
        userRepository,
        assignmentResultService,
        assignmentRepository,
        userSolutionChecker,
        taskRegistry
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `should save logs even if solution checking fails with SolutionCheckingException`() = runBlocking {
        // Given
        val userId = 1L
        val assignmentIdLong = 2L
        val assignmentIdInt = 2
        val taskStatusId = 3L
        val solutionBytes = byteArrayOf(1, 2, 3)
        val expectedLogs = "Container logs that should be saved"

        val user = User().apply { id = userId }
        val assignmentProps = mockk<AssignmentRepository.AssignmentProps>()
        every { assignmentProps.id } returns assignmentIdLong

        val message = VerificationTaskMessage(
            taskId = taskStatusId,
            additionalParams = VerificationTaskDto(assignmentIdInt, userId, solutionBytes)
        )

        every { taskQueueService.isTaskCancelled(taskStatusId) } returns false
        coEvery { assignmentRepository.findPropsById(assignmentIdLong) } returns assignmentProps
        coEvery { userRepository.findUserById(userId) } returns user

        // Use slot or match to match the suspend function call
        coEvery {
            userSolutionChecker.checkSubmission(
                assignmentId = assignmentIdLong,
                solutionBytes = solutionBytes,
                user = user,
                isActive = any()
            )
        } throws SolutionCheckingException(expectedLogs, "Failed to parse test results")

        // When
        try {
            processor.process(message)
        } catch (e: Exception) {
            // Expected
        }

        // Then
        coVerify {
            assignmentResultService.handleAssignmentResult(
                assignmentId = assignmentIdLong,
                result = null,
                user = user,
                solutionBytes = solutionBytes,
                logs = expectedLogs
            )
        }
    }
}
