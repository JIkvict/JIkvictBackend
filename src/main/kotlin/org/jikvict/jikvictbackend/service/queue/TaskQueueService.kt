package org.jikvict.jikvictbackend.service.queue

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.queue.TaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.model.response.PendingStatusResponse
import org.jikvict.jikvictbackend.model.response.ResponsePayload
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
abstract class TaskQueueService(
    private val rabbitTemplate: RabbitTemplate,
    private val taskStatusRepository: TaskStatusRepository,
    private val taskRegistry: TaskRegistry,
    private val log: Logger,
) {
    fun isAlreadyQueuedForUser(user: User): Boolean {
        val tasks = taskStatusRepository.findAllByUserAndTaskTypeAndStatus(user, "SOLUTION_VERIFICATION", PendingStatus.PENDING)
        return tasks.isNotEmpty()
    }

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
    fun getTaskStatus(
        taskId: Long,
        user: User,
    ): TaskStatus {
        val task =
            taskStatusRepository
                .findById(taskId)
                .orElseThrow { ServiceException(HttpStatus.NOT_FOUND, "Task with ID $taskId not found") }
        if (task.user.id != user.id) {
            throw ServiceException(HttpStatus.FORBIDDEN, "You do not have permission to access this task")
        }
        return task
    }

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

    /**
     * Updates only the result ID of a task
     */
    fun updateTaskResultId(
        taskId: Long,
        resultId: Long,
    ) {
        val taskStatus =
            taskStatusRepository
                .findById(taskId)
                .orElseThrow { IllegalArgumentException("Task with ID $taskId not found") }

        taskStatus.resultId = resultId
        taskStatusRepository.save(taskStatus)
    }

    fun isTaskCancelled(taskId: Long): Boolean {
        val taskStatusRepository = taskStatusRepository.findById(taskId).orElseThrow { ServiceException(HttpStatus.NOT_FOUND, "Task with ID $taskId not found") }
        return taskStatusRepository.status == PendingStatus.CANCELLED
    }

    fun getTaskStatusResponse(
        taskId: Long,
        user: User,
    ): PendingStatusResponse<Long?> {
        val taskStatus = getTaskStatus(taskId, user)
        return PendingStatusResponse(
            payload = ResponsePayload(taskStatus.resultId),
            status = taskStatus.status,
            message = taskStatus.message,
        )
    }
}
