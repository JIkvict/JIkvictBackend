package org.jikvict.jikvictbackend.service.processor

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.model.dto.CreateAssignmentDto
import org.jikvict.jikvictbackend.model.queue.AssignmentTaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.service.assignment.AssignmentService
import org.jikvict.jikvictbackend.service.queue.AssignmentTaskQueueService
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class AssignmentTaskProcessor(
    private val assignmentService: AssignmentService,
    private val taskQueueService: AssignmentTaskQueueService,
    private val log: Logger,
    private val taskRegistry: TaskRegistry,
) : TaskProcessor<CreateAssignmentDto, AssignmentTaskMessage> {
    override val taskType: String = "ASSIGNMENT_CREATION"
    override val queueName: String = "assignment.queue"
    override val exchangeName: String = "assignment.exchange"
    override val routingKey: String = "assignment.routingkey"

    @RabbitListener(queues = ["assignment.queue"])
    fun process(message: AssignmentTaskMessage) {
        log.info("Processing assignment creation task: ${message.taskId}")
        try {
            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.PENDING,
                "Creating assignment for task ${message.additionalParams.taskId}",
            )
            val savedAssignment = assignmentService.createAssignment(message.additionalParams)

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

    @PostConstruct
    fun init() {
        taskRegistry.registerProcessor(this)
    }
}
