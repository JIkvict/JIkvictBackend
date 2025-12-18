package org.jikvict.jikvictbackend.service.queue

import com.fasterxml.jackson.databind.ObjectMapper
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.model.response.QueueStatusDto
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.service.assignment.AssignmentCacheService
import org.springframework.stereotype.Service

@Service
class QueueStatusService(
    private val taskStatusRepository: TaskStatusRepository,
    private val assignmentCacheService: AssignmentCacheService,
    private val objectMapper: ObjectMapper,
) {
    fun getQueueStatus(user: User): QueueStatusDto {
        val pendingTasks =
            taskStatusRepository
                .findAllByStatus(PendingStatus.PENDING)
                .sortedBy { it.createdAt }

        val totalInQueue = pendingTasks.size

        val userTask =
            pendingTasks.firstOrNull { it.user.id == user.id }

        val userTaskPosition = userTask?.let { task ->
            pendingTasks.indexOfFirst { it.id == task.id } + 1
        }

        val estimatedTime = userTask?.let { task ->
            calculateEstimatedTime(pendingTasks, task.id)
        }

        return QueueStatusDto(
            totalInQueue = totalInQueue,
            userTaskPosition = userTaskPosition,
            userTaskId = userTask?.id,
            estimatedTimeRemainingSeconds = estimatedTime,
        )
    }

    fun getQueueStatusForAll(): Map<Long, QueueStatusDto> {
        val pendingTasks =
            taskStatusRepository
                .findAllByStatus(PendingStatus.PENDING)
                .sortedBy { it.createdAt }

        val totalInQueue = pendingTasks.size

        val assignmentIds =
            pendingTasks.mapNotNull { task ->
                runCatching {
                    val node = objectMapper.readTree(task.parameters)
                    node?.get("assignmentId")?.asLong()
                }.getOrNull()
            }.distinct()

        val timeoutMap = assignmentCacheService.getAssignmentTimeouts(assignmentIds).associateBy { it.id }

        return pendingTasks.mapIndexed { index, task ->
            val assignmentId =
                runCatching {
                    val node = objectMapper.readTree(task.parameters)
                    node?.get("assignmentId")?.asLong()
                }.getOrNull()

            val timeoutSeconds = assignmentId?.let { timeoutMap[it]?.timeOutSeconds }
            val estimatedTime = timeoutSeconds?.let { (index) * it }

            task.user.id to
                QueueStatusDto(
                    totalInQueue = totalInQueue,
                    userTaskPosition = index + 1,
                    userTaskId = task.id,
                    estimatedTimeRemainingSeconds = estimatedTime,
                )
        }.toMap()
    }

    private fun calculateEstimatedTime(
        pendingTasks: List<TaskStatus>,
        userTaskId: Long,
    ): Long? {
        val taskIndex = pendingTasks.indexOfFirst { it.id == userTaskId }
        if (taskIndex < 0) return null

        val assignmentIds =
            pendingTasks.take(taskIndex + 1).mapNotNull { task ->
                runCatching {
                    val node = objectMapper.readTree(task.parameters)
                    node?.get("assignmentId")?.asLong()
                }.getOrNull()
            }.distinct()

        val timeoutMap = assignmentCacheService.getAssignmentTimeouts(assignmentIds).associateBy { it.id }

        val userAssignmentId =
            runCatching {
                val node = objectMapper.readTree(pendingTasks[taskIndex].parameters)
                node?.get("assignmentId")?.asLong()
            }.getOrNull() ?: return null

        val timeoutSeconds = timeoutMap[userAssignmentId]?.timeOutSeconds ?: return null

        return taskIndex * timeoutSeconds
    }
}
