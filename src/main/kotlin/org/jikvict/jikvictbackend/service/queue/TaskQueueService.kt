package org.jikvict.jikvictbackend.service.queue

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.model.queue.TaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.model.response.PendingStatusResponse
import org.jikvict.jikvictbackend.model.response.ResponsePayload
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
abstract class TaskQueueService(
    private val rabbitTemplate: RabbitTemplate,
    private val taskStatusRepository: TaskStatusRepository,
    private val taskRegistry: TaskRegistry,
    private val log: Logger,
) {
    internal fun sendTaskToQueue(message: TaskMessage<*>) {
        val processor =
            taskRegistry.getProcessorByTaskType(message.taskType)
                ?: throw IllegalArgumentException("No processor registered for task type: ${message.taskType}")

        log.info("Sending ${message.taskType} task to queue: $message")
        rabbitTemplate.convertAndSend(
            processor.exchangeName,
            processor.routingKey,
            message,
        )
    }

    /**
     * Gets the status of a task by ID
     */
    fun getTaskStatus(taskId: Long): TaskStatus =
        taskStatusRepository
            .findById(taskId)
            .orElseThrow { IllegalArgumentException("Task with ID $taskId not found") }

    /**
     * Updates the status of a task
     */
    fun updateTaskStatus(
        taskId: Long,
        status: PendingStatus,
        message: String? = null,
        resultId: Long? = null,
    ) {
        val taskStatus =
            taskStatusRepository
                .findById(taskId)
                .orElseThrow { IllegalArgumentException("Task with ID $taskId not found") }

        taskStatus.status = status
        if (message != null) {
            taskStatus.message = message
        }
        if (resultId != null) {
            taskStatus.resultId = resultId
        }

        if (status == PendingStatus.DONE || status == PendingStatus.FAILED || status == PendingStatus.REJECTED) {
            taskStatus.completedAt = LocalDateTime.now()
        }

        taskStatusRepository.save(taskStatus)
    }

    fun getTaskStatusResponse(taskId: Long): PendingStatusResponse<Long?> {
        val taskStatus = getTaskStatus(taskId)
        return PendingStatusResponse(
            payload = ResponsePayload(taskStatus.resultId),
            status = taskStatus.status,
            message = taskStatus.message,
        )
    }
}
