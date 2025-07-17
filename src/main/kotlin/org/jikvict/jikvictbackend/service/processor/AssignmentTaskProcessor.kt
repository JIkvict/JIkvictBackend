package org.jikvict.jikvictbackend.service.processor

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.model.queue.AssignmentTaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.service.AssignmentService
import org.jikvict.jikvictbackend.service.TaskQueueService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class AssignmentTaskProcessor(
    private val assignmentService: AssignmentService,
    private val assignmentRepository: AssignmentRepository,
    private val taskQueueService: TaskQueueService,
    private val log: Logger,
) : TaskProcessor<AssignmentTaskMessage> {
    override val taskType: String = "ASSIGNMENT_CREATION"
    override val queueName: String = "assignment.queue"
    override val exchangeName: String = "assignment.exchange"
    override val routingKey: String = "assignment.routingkey"

    @RabbitListener(queues = ["assignment.queue"])
    override fun process(message: AssignmentTaskMessage) {
        log.info("Processing assignment creation task: ${message.assignmentNumber}")

        try {
            // Update task status to in-progress
            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.PENDING,
                "Creating assignment ${message.assignmentNumber}",
            )

            // Get assignment description
            val description = assignmentService.getAssignmentDescription(message.assignmentNumber)

            // Create assignment entity
            val assignment =
                Assignment().apply {
                    title = "Assignment ${message.assignmentNumber}"
                    this.description = description
                    taskNumber = message.assignmentNumber
                }

            // Save assignment
            val savedAssignment = assignmentRepository.save(assignment)

            // Update task status to done
            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.DONE,
                "Assignment ${message.assignmentNumber} created successfully",
                savedAssignment.id,
            )

            log.info("Assignment creation completed: ${savedAssignment.id}")
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
