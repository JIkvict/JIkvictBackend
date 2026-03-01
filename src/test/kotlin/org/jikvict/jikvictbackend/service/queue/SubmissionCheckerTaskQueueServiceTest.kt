package org.jikvict.jikvictbackend.service.queue

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.entity.AssignmentResult
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.queue.VerificationTaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.AssignmentResultRepository
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.service.UserDetailsServiceImpl
import org.jikvict.jikvictbackend.service.processor.TaskProcessor
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.time.LocalDateTime
import java.util.Optional

class SubmissionCheckerTaskQueueServiceTest {
    private lateinit var rabbitTemplate: RabbitTemplate
    private lateinit var taskStatusRepository: TaskStatusRepository
    private lateinit var assignmentResultRepository: AssignmentResultRepository
    private lateinit var assignmentRepository: AssignmentRepository
    private lateinit var taskRegistry: TaskRegistry
    private lateinit var log: Logger
    private lateinit var objectMapper: ObjectMapper
    private lateinit var userDetailsService: UserDetailsServiceImpl
    private lateinit var service: SubmissionCheckerTaskQueueService

    @BeforeEach
    fun setup() {
        rabbitTemplate = mockk(relaxed = true)
        taskStatusRepository = mockk(relaxed = true)
        assignmentResultRepository = mockk(relaxed = true)
        assignmentRepository = mockk(relaxed = true)
        taskRegistry = mockk(relaxed = true)
        log = mockk(relaxed = true)
        objectMapper = ObjectMapper()
        userDetailsService = mockk(relaxed = true)

        service = SubmissionCheckerTaskQueueService(
            rabbitTemplate,
            taskStatusRepository,
            assignmentResultRepository,
            assignmentRepository,
            taskRegistry,
            log,
            objectMapper,
            userDetailsService,
        )

        val mockProcessor = mockk<TaskProcessor<*, *>>()
        every { mockProcessor.exchangeName } returns "test-exchange"
        every { mockProcessor.routingKey } returns "test-routing-key"
        every { taskRegistry.getProcessorByTaskType("SOLUTION_VERIFICATION") } returns mockProcessor
    }

    @Test
    fun `retryFailedSubmissions should retry only the latest failed submission for each user-assignment pair`() {
        // Given
        val user1 = createUser(1L, "user1")
        val user2 = createUser(2L, "user2")

        // User 1, Assignment 10: FAILED (latest), DONE (old)
        val task1_10_old = createTaskStatus(1L, user1, 10L, PendingStatus.DONE, LocalDateTime.now().minusHours(2))
        val task1_10_latest = createTaskStatus(2L, user1, 10L, PendingStatus.FAILED, LocalDateTime.now().minusHours(1))

        // User 1, Assignment 11: DONE (latest)
        val task1_11_latest = createTaskStatus(3L, user1, 11L, PendingStatus.DONE, LocalDateTime.now().minusMinutes(30))

        // User 2, Assignment 10: FAILED (latest)
        val task2_10_latest = createTaskStatus(4L, user2, 10L, PendingStatus.FAILED, LocalDateTime.now().minusMinutes(15))

        // User 2, Assignment 11: FAILED (old), FAILED (latest) -> should only retry once (the latest)
        val task2_11_old = createTaskStatus(5L, user2, 11L, PendingStatus.FAILED, LocalDateTime.now().minusHours(3))
        val task2_11_latest = createTaskStatus(6L, user2, 11L, PendingStatus.FAILED, LocalDateTime.now().minusHours(1))

        val allTasks = listOf(
            task1_11_latest, task2_10_latest, task1_10_latest, task2_11_latest, task1_10_old, task2_11_old,
        ).sortedByDescending { it.createdAt }

        every { taskStatusRepository.findAllByTaskTypeOrderByCreatedAtDesc("SOLUTION_VERIFICATION") } returns allTasks

        // Mocking AssignmentResult retrieval
        val result1_10 = createAssignmentResult(100L, user1, 10L, byteArrayOf(1, 2, 3))
        val result2_10 = createAssignmentResult(200L, user2, 10L, byteArrayOf(4, 5, 6))
        val result2_11 = createAssignmentResult(300L, user2, 11L, byteArrayOf(7, 8, 9))

        // Mock task.resultId lookup
        task1_10_latest.resultId = 100L
        task2_10_latest.resultId = 200L
        task2_11_latest.resultId = 300L

        every { assignmentResultRepository.findById(100L) } returns Optional.of(result1_10)
        every { assignmentResultRepository.findById(200L) } returns Optional.of(result2_10)
        every { assignmentResultRepository.findById(300L) } returns Optional.of(result2_11)

        // Mock saved TaskStatus
        every { taskStatusRepository.save(any<TaskStatus>()) } answers { it.invocation.args[0] as TaskStatus }

        // When
        val retriedCount = service.retryFailedSubmissions()

        // Then
        assertEquals(3, retriedCount) // task1_10_latest, task2_10_latest, task2_11_latest

        // Verify enqueued tasks (3 calls to rabbitTemplate)
        verify(exactly = 3) { rabbitTemplate.convertAndSend(any(), any(), any<VerificationTaskMessage>()) }

        // Verify TaskStatus saved 3 times for retries
        verify(exactly = 3) { taskStatusRepository.save(match { it.status == PendingStatus.PENDING }) }
    }

    @Test
    fun `retryFailedSubmissions should filter by assignmentId if provided`() {
        // Given
        val user1 = createUser(1L, "user1")
        val task1_10_failed = createTaskStatus(1L, user1, 10L, PendingStatus.FAILED, LocalDateTime.now())
        val task1_11_failed = createTaskStatus(2L, user1, 11L, PendingStatus.FAILED, LocalDateTime.now().minusMinutes(1))

        every { taskStatusRepository.findAllByTaskTypeOrderByCreatedAtDesc("SOLUTION_VERIFICATION") } returns listOf(task1_10_failed, task1_11_failed)

        val result1_10 = createAssignmentResult(100L, user1, 10L, byteArrayOf(1, 2, 3))
        task1_10_failed.resultId = 100L
        every { assignmentResultRepository.findById(100L) } returns Optional.of(result1_10)

        // Mock saved TaskStatus
        every { taskStatusRepository.save(any<TaskStatus>()) } answers { it.invocation.args[0] as TaskStatus }

        // When
        val retriedCount = service.retryFailedSubmissions(10L)

        // Then
        assertEquals(1, retriedCount)
        verify(exactly = 1) { rabbitTemplate.convertAndSend(any(), any(), any<VerificationTaskMessage>()) }
    }

    private fun createUser(id: Long, username: String): User = User().apply {
        this.id = id
        this.userNameField = username
    }

    private fun createTaskStatus(
        id: Long,
        user: User,
        assignmentId: Long,
        status: PendingStatus,
        createdAt: LocalDateTime,
    ): TaskStatus = TaskStatus().apply {
        this.id = id
        this.user = user
        this.status = status
        this.createdAt = createdAt
        this.taskType = "SOLUTION_VERIFICATION"
        this.parameters = """{"assignmentId": $assignmentId, "originalFilename": "solution.zip"}"""
    }

    private fun createAssignmentResult(id: Long, user: User, assignmentId: Long, zipFile: ByteArray): AssignmentResult = AssignmentResult().apply {
        this.id = id
        this.user = user
        this.assignment = Assignment().apply { this.id = assignmentId }
        this.zipFile = zipFile
    }
}
