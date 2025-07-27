package org.jikvict.jikvictbackend.service.processor

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.model.dto.AssignmentDto
import org.jikvict.jikvictbackend.model.queue.AssignmentTaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.service.AssignmentService
import org.jikvict.jikvictbackend.service.queue.TaskQueueService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class AssignmentTaskProcessor(
    private val assignmentService: AssignmentService,
    private val taskQueueService: TaskQueueService,
    private val log: Logger,
) : TaskProcessor<AssignmentDto, AssignmentTaskMessage> {
    override val taskType: String = "ASSIGNMENT_CREATION"
    override val queueName: String = "assignment.queue"
    override val exchangeName: String = "assignment.exchange"
    override val routingKey: String = "assignment.routingkey"

    @RabbitListener(queues = ["assignment.queue"])
    override fun process(message: AssignmentTaskMessage) {
        log.info("Processing assignment creation task: ${message.taskId}")

        try {
            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.PENDING,
                "Creating assignment for task ${message.additionalParams.taskId}",
            )

            val savedAssignment =
                runCatching {
                    assignmentService.createAssignment(message.additionalParams)
                }.onFailure {
                    taskQueueService.updateTaskStatus(
                        message.taskId,
                        PendingStatus.FAILED,
                        "Failed to create assignment: ${it.message}",
                    )
                    return
                }.getOrNull()!!
            log.info("Assignment creation completed: ${savedAssignment.id}")
            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.DONE,
                "Assignment created successfully: ${savedAssignment.id}",
            )
        } catch (e: Exception) {
            log.error("Error creating assignment: ${e.message}", e)
            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.FAILED,
                "Error creating assignment: ${e.message}",
            )
        }
    }
}
