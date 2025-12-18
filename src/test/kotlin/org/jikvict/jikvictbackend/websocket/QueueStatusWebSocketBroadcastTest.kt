package org.jikvict.jikvictbackend.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.jikvict.jikvictbackend.controller.QueueStatusController
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.model.response.QueueStatusDto
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.service.UserDetailsServiceImpl
import org.jikvict.jikvictbackend.service.assignment.AssignmentCacheService
import org.jikvict.jikvictbackend.service.queue.QueueStatusService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.LocalDateTime

class QueueStatusWebSocketBroadcastTest {
    private lateinit var taskStatusRepository: TaskStatusRepository
    private lateinit var assignmentCacheService: AssignmentCacheService
    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var objectMapper: ObjectMapper
    private lateinit var queueStatusService: QueueStatusService
    private lateinit var controller: QueueStatusController

    @BeforeEach
    fun setup() {
        taskStatusRepository = mockk()
        assignmentCacheService = mockk()
        messagingTemplate = mockk(relaxed = true)
        objectMapper = ObjectMapper()

        queueStatusService = QueueStatusService(
            taskStatusRepository,
            assignmentCacheService,
            objectMapper,
        )

        controller = QueueStatusController(
            queueStatusService,
            mockk<UserDetailsServiceImpl>(),
            messagingTemplate,
        )
    }

    @Test
    fun `should broadcast queue status updates when tasks are submitted`() {
        println("\n[DEBUG_LOG] ═══════════════════════════════════════════════════════════════")
        println("[DEBUG_LOG] TEST: WebSocket Queue Status Broadcasting")
        println("[DEBUG_LOG] ═══════════════════════════════════════════════════════════════")
        println()


        val user1 = createUser(1L, "student1")
        val user2 = createUser(2L, "student2")
        val user3 = createUser(3L, "student3")


        println("[DEBUG_LOG] ─────────────────────────────────────────────────────────────────")
        println("[DEBUG_LOG] SCENARIO 1: Queue is empty - no tasks")
        println("[DEBUG_LOG] ─────────────────────────────────────────────────────────────────")

        every { taskStatusRepository.findAllByStatus(PendingStatus.PENDING) } returns emptyList()
        every { assignmentCacheService.getAssignmentTimeouts(emptyList()) } returns emptyList()

        controller.broadcastQueueStatus()

        verify(exactly = 0) { messagingTemplate.convertAndSend(any<String>(), any<QueueStatusDto>()) }
        println("[DEBUG_LOG] ✓ No messages sent (queue is empty)")
        println()


        println("[DEBUG_LOG] ─────────────────────────────────────────────────────────────────")
        println("[DEBUG_LOG] SCENARIO 2: User1 submits task for Assignment #101")
        println("[DEBUG_LOG] ─────────────────────────────────────────────────────────────────")

        val task1 = createTaskStatus(1L, user1, 101L, LocalDateTime.now())

        every { taskStatusRepository.findAllByStatus(PendingStatus.PENDING) } returns listOf(task1)
        every { assignmentCacheService.getAssignmentTimeouts(listOf(101L)) } returns listOf(
            createTimeoutProps(101L, 300L),
        )

        captureAndPrintMessages(1) {
            controller.broadcastQueueStatus()
        }
        println()


        println("[DEBUG_LOG] ─────────────────────────────────────────────────────────────────")
        println("[DEBUG_LOG] SCENARIO 3: User2 submits task for Assignment #102")
        println("[DEBUG_LOG] ─────────────────────────────────────────────────────────────────")

        val task2 = createTaskStatus(2L, user2, 102L, LocalDateTime.now().plusSeconds(5))

        every { taskStatusRepository.findAllByStatus(PendingStatus.PENDING) } returns listOf(task1, task2)
        every { assignmentCacheService.getAssignmentTimeouts(listOf(101L, 102L)) } returns listOf(
            createTimeoutProps(101L, 300L),
            createTimeoutProps(102L, 600L),
        )

        captureAndPrintMessages(2) {
            controller.broadcastQueueStatus()
        }
        println()


        println("[DEBUG_LOG] ─────────────────────────────────────────────────────────────────")
        println("[DEBUG_LOG] SCENARIO 4: User3 submits task for Assignment #101")
        println("[DEBUG_LOG] ─────────────────────────────────────────────────────────────────")

        val task3 = createTaskStatus(3L, user3, 101L, LocalDateTime.now().plusSeconds(10))

        every { taskStatusRepository.findAllByStatus(PendingStatus.PENDING) } returns listOf(task1, task2, task3)
        every { assignmentCacheService.getAssignmentTimeouts(listOf(101L, 102L)) } returns listOf(
            createTimeoutProps(101L, 300L),
            createTimeoutProps(102L, 600L),
        )

        captureAndPrintMessages(3) {
            controller.broadcastQueueStatus()
        }
        println()


        println("[DEBUG_LOG] ─────────────────────────────────────────────────────────────────")
        println("[DEBUG_LOG] SCENARIO 5: User1's task completes - removed from queue")
        println("[DEBUG_LOG] ─────────────────────────────────────────────────────────────────")

        every { taskStatusRepository.findAllByStatus(PendingStatus.PENDING) } returns listOf(task2, task3)
        every { assignmentCacheService.getAssignmentTimeouts(listOf(102L, 101L)) } returns listOf(
            createTimeoutProps(101L, 300L),
            createTimeoutProps(102L, 600L),
        )

        captureAndPrintMessages(2) {
            controller.broadcastQueueStatus()
        }
        println()

        println("[DEBUG_LOG] ═══════════════════════════════════════════════════════════════")
        println("[DEBUG_LOG] TEST COMPLETED SUCCESSFULLY")
        println("[DEBUG_LOG] ═══════════════════════════════════════════════════════════════")
        println()
    }

    private fun captureAndPrintMessages(expectedCount: Int, action: () -> Unit) {
        // Clear previous invocations to ensure each scenario is verified independently
        clearMocks(messagingTemplate, answers = false)

        val topicSlot = slot<String>()
        val messageSlot = slot<QueueStatusDto>()
        val capturedMessages = mutableListOf<Pair<String, QueueStatusDto>>()

        every {
            messagingTemplate.convertAndSend(capture(topicSlot), capture(messageSlot))
        } answers {
            capturedMessages.add(topicSlot.captured to messageSlot.captured)
        }

        action()

        println("[DEBUG_LOG] Broadcasting ${capturedMessages.size} WebSocket message(s):")
        println()

        capturedMessages.forEachIndexed { index, (topic, message) ->
            val userId = topic.substringAfterLast("/")
            println("[DEBUG_LOG] ┌─── Message #${index + 1} ───────────────────────────────────────────────")
            println("[DEBUG_LOG] │ Topic: $topic")
            println("[DEBUG_LOG] │ User ID: $userId")
            println("[DEBUG_LOG] ├──────────────────────────────────────────────────────────────")
            println("[DEBUG_LOG] │ Payload (QueueStatusDto):")
            println("[DEBUG_LOG] │   • Total tasks in queue:        ${message.totalInQueue}")
            println("[DEBUG_LOG] │   • User task position:          ${message.userTaskPosition}")
            println("[DEBUG_LOG] │   • User task ID:                ${message.userTaskId}")

            val estimatedTime = message.estimatedTimeRemainingSeconds
            if (estimatedTime != null) {
                val minutes = estimatedTime / 60
                val seconds = estimatedTime % 60
                println("[DEBUG_LOG] │   • Estimated wait time:         ${minutes}m ${seconds}s (${estimatedTime}s)")
            } else {
                println("[DEBUG_LOG] │   • Estimated wait time:         N/A")
            }

            println("[DEBUG_LOG] └──────────────────────────────────────────────────────────────")
            println()
        }

        verify(exactly = expectedCount) {
            messagingTemplate.convertAndSend(any<String>(), any<QueueStatusDto>())
        }
    }

    private fun createUser(id: Long, username: String): User {
        return User().apply {
            this.id = id
            this.userNameField = username
        }
    }

    private fun createTaskStatus(
        id: Long,
        user: User,
        assignmentId: Long,
        createdAt: LocalDateTime,
    ): TaskStatus {
        return TaskStatus().apply {
            this.id = id
            this.user = user
            this.createdAt = createdAt
            this.taskType = "SOLUTION_VERIFICATION"
            this.status = PendingStatus.PENDING
            this.parameters = """{"assignmentId": $assignmentId, "originalFilename": "solution.zip"}"""
        }
    }

    private fun createTimeoutProps(id: Long, timeoutSeconds: Long): AssignmentRepository.AssignmentTimeoutProps {
        return object : AssignmentRepository.AssignmentTimeoutProps {
            override val id: Long = id
            override val timeOutSeconds: Long = timeoutSeconds
        }
    }
}
