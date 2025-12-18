package org.jikvict.jikvictbackend.service.queue

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.service.assignment.AssignmentCacheService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class QueueStatusServiceTest {
    private lateinit var taskStatusRepository: TaskStatusRepository
    private lateinit var assignmentCacheService: AssignmentCacheService
    private lateinit var objectMapper: ObjectMapper
    private lateinit var queueStatusService: QueueStatusService

    @BeforeEach
    fun setup() {
        taskStatusRepository = mockk()
        assignmentCacheService = mockk()
        objectMapper = ObjectMapper()
        queueStatusService = QueueStatusService(
            taskStatusRepository,
            assignmentCacheService,
            objectMapper
        )
    }

    @Test
    fun `getQueueStatus should return correct position for user with pending task`() {
        // Given
        val user1 = createUser(1L, "user1")
        val user2 = createUser(2L, "user2")
        val user3 = createUser(3L, "user3")

        val task1 = createTaskStatus(1L, user1, 101L, LocalDateTime.now().minusMinutes(3))
        val task2 = createTaskStatus(2L, user2, 102L, LocalDateTime.now().minusMinutes(2))
        val task3 = createTaskStatus(3L, user3, 103L, LocalDateTime.now().minusMinutes(1))

        every { taskStatusRepository.findAllByStatus(PendingStatus.PENDING) } returns listOf(task1, task2, task3)

        val timeoutProps = listOf(
            createTimeoutProps(101L, 300L),
            createTimeoutProps(102L, 300L)
        )
        every { assignmentCacheService.getAssignmentTimeouts(listOf(101L, 102L)) } returns timeoutProps

        // When
        val result = queueStatusService.getQueueStatus(user2)

        // Then
        assertEquals(3, result.totalInQueue)
        assertEquals(2, result.userTaskPosition)
        assertEquals(2L, result.userTaskId)
        assertEquals(300L, result.estimatedTimeRemainingSeconds) // position 2, so (2-1) * 300 = 300
    }

    @Test
    fun `getQueueStatus should return null position for user without pending task`() {
        // Given
        val user1 = createUser(1L, "user1")
        val user2 = createUser(2L, "user2")
        val userWithoutTask = createUser(99L, "userWithoutTask")

        val task1 = createTaskStatus(1L, user1, 101L, LocalDateTime.now().minusMinutes(2))
        val task2 = createTaskStatus(2L, user2, 102L, LocalDateTime.now().minusMinutes(1))

        every { taskStatusRepository.findAllByStatus(PendingStatus.PENDING) } returns listOf(task1, task2)

        // When
        val result = queueStatusService.getQueueStatus(userWithoutTask)

        // Then
        assertEquals(2, result.totalInQueue)
        assertNull(result.userTaskPosition)
        assertNull(result.userTaskId)
        assertNull(result.estimatedTimeRemainingSeconds)
    }

    @Test
    fun `getQueueStatusForAll should return correct status for all users`() {
        // Given
        val user1 = createUser(1L, "user1")
        val user2 = createUser(2L, "user2")
        val user3 = createUser(3L, "user3")

        val task1 = createTaskStatus(1L, user1, 101L, LocalDateTime.now().minusMinutes(3))
        val task2 = createTaskStatus(2L, user2, 102L, LocalDateTime.now().minusMinutes(2))
        val task3 = createTaskStatus(3L, user3, 101L, LocalDateTime.now().minusMinutes(1))

        every { taskStatusRepository.findAllByStatus(PendingStatus.PENDING) } returns listOf(task1, task2, task3)

        val timeoutProps = listOf(
            createTimeoutProps(101L, 300L),
            createTimeoutProps(102L, 600L)
        )
        every { assignmentCacheService.getAssignmentTimeouts(listOf(101L, 102L)) } returns timeoutProps

        // When
        val result = queueStatusService.getQueueStatusForAll()

        // Then
        assertEquals(3, result.size)

        // User 1 - position 1
        val status1 = result[1L]
        assertNotNull(status1)
        assertEquals(3, status1!!.totalInQueue)
        assertEquals(1, status1.userTaskPosition)
        assertEquals(1L, status1.userTaskId)
        assertEquals(0L, status1.estimatedTimeRemainingSeconds) // position 1, so 0 * 300 = 0

        // User 2 - position 2
        val status2 = result[2L]
        assertNotNull(status2)
        assertEquals(3, status2!!.totalInQueue)
        assertEquals(2, status2.userTaskPosition)
        assertEquals(2L, status2.userTaskId)
        assertEquals(600L, status2.estimatedTimeRemainingSeconds) // position 2, so 1 * 600 = 600

        // User 3 - position 3
        val status3 = result[3L]
        assertNotNull(status3)
        assertEquals(3, status3!!.totalInQueue)
        assertEquals(3, status3.userTaskPosition)
        assertEquals(3L, status3.userTaskId)
        assertEquals(600L, status3.estimatedTimeRemainingSeconds) // position 3, so 2 * 300 = 600
    }

    @Test
    fun `getQueueStatusForAll should handle empty queue`() {
        // Given
        every { taskStatusRepository.findAllByStatus(PendingStatus.PENDING) } returns emptyList()
        every { assignmentCacheService.getAssignmentTimeouts(emptyList()) } returns emptyList()

        // When
        val result = queueStatusService.getQueueStatusForAll()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `getQueueStatus should handle missing assignmentId in parameters`() {
        // Given
        val user = createUser(1L, "user1")
        val task = TaskStatus().apply {
            id = 1L
            this.user = user
            createdAt = LocalDateTime.now()
            parameters = """{"someOtherField": "value"}"""
        }

        every { taskStatusRepository.findAllByStatus(PendingStatus.PENDING) } returns listOf(task)
        every { assignmentCacheService.getAssignmentTimeouts(emptyList()) } returns emptyList()

        // When
        val result = queueStatusService.getQueueStatus(user)

        // Then
        assertEquals(1, result.totalInQueue)
        assertEquals(1, result.userTaskPosition)
        assertEquals(1L, result.userTaskId)
        assertNull(result.estimatedTimeRemainingSeconds)
    }

    private fun createUser(id: Long, username: String): User {
        return User().apply {
            this.id = id
            this.userNameField = username
        }
    }

    private fun createTaskStatus(id: Long, user: User, assignmentId: Long, createdAt: LocalDateTime): TaskStatus {
        return TaskStatus().apply {
            this.id = id
            this.user = user
            this.createdAt = createdAt
            this.taskType = "SOLUTION_VERIFICATION"
            this.status = PendingStatus.PENDING
            this.parameters = """{"assignmentId": $assignmentId, "originalFilename": "test.zip"}"""
        }
    }

    private fun createTimeoutProps(id: Long, timeoutSeconds: Long): AssignmentRepository.AssignmentTimeoutProps {
        return object : AssignmentRepository.AssignmentTimeoutProps {
            override val id: Long = id
            override val timeOutSeconds: Long = timeoutSeconds
        }
    }
}
