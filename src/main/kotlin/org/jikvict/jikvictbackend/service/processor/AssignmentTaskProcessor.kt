package org.jikvict.jikvictbackend.service.processor

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.entity.Task
import org.jikvict.jikvictbackend.model.dto.AssignmentDto
import org.jikvict.jikvictbackend.model.queue.AssignmentTaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.TaskRepository
import org.jikvict.jikvictbackend.service.AssignmentService
import org.jikvict.jikvictbackend.service.TaskQueueService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class AssignmentTaskProcessor(
    private val assignmentService: AssignmentService,
    private val assignmentRepository: AssignmentRepository,
    private val taskRepository: TaskRepository,
    private val taskQueueService: TaskQueueService,
    private val log: Logger,
) : TaskProcessor<AssignmentDto, AssignmentTaskMessage> {
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

            // Get assignment description from the message or from the service if not provided
            val description =
                message.additionalParams.description.ifBlank {
                    assignmentService.getAssignmentDescription(message.assignmentNumber)
                }

            // Find or create Task entity
            val task = findOrCreateTask(message.assignmentNumber)

            // Create assignment entity
            val assignment =
                Assignment(
                    title = message.additionalParams.title,
                    description = description,
                    task = task,
                    maxPoints = message.additionalParams.maxPoints,
                    startDate = message.additionalParams.startDate,
                    endDate = message.additionalParams.endDate,
                )

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

    /**
     * Finds an existing Task by ID or creates a new one
     */
    private fun findOrCreateTask(taskNumber: Int): Task {
        // Try to find an existing task with the same ID
        val existingTask = taskRepository.findById(taskNumber.toLong())

        if (existingTask.isPresent) {
            return existingTask.get()
        }

        // Create a new task
        val task =
            Task().apply {
                id = taskNumber.toLong()
            }

        return taskRepository.save(task)
    }
}
