package org.jikvict.jikvictbackend.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.model.dto.AssignmentDto
import org.jikvict.jikvictbackend.model.dto.VerificationTaskDto
import org.jikvict.jikvictbackend.model.queue.AssignmentTaskMessage
import org.jikvict.jikvictbackend.model.queue.TaskMessage
import org.jikvict.jikvictbackend.model.queue.VerificationTaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.model.response.PendingStatusResponse
import org.jikvict.jikvictbackend.model.response.ResponsePayload
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.UUID

@Service
class TaskQueueService(
    private val rabbitTemplate: RabbitTemplate,
    private val taskStatusRepository: TaskStatusRepository,
    private val objectMapper: ObjectMapper,
    private val taskRegistry: TaskRegistry,
    private val log: Logger,
) {
    /**
     * Enqueues an assignment creation task and returns the task ID
     */
    fun enqueueAssignmentCreationTask(
        assignmentNumber: Int,
        title: String = "Assignment $assignmentNumber",
        description: String = "",
        taskId: Long = assignmentNumber.toLong(),
        maxPoints: Int = 100,
        startDate: LocalDateTime = LocalDateTime.now(),
        endDate: LocalDateTime = LocalDateTime.now().plusDays(14),
        assignmentGroupIds: List<Long> = emptyList(),
    ): Long {
        // Create a task status record
        val taskStatus =
            TaskStatus().apply {
                taskType = "ASSIGNMENT_CREATION"
                status = PendingStatus.PENDING
                createdAt = LocalDateTime.now()
                parameters =
                    objectMapper.writeValueAsString(
                        mapOf(
                            "assignmentNumber" to assignmentNumber,
                            "title" to title,
                            "description" to description,
                            "taskId" to taskId,
                            "maxPoints" to maxPoints,
                            "startDate" to startDate,
                            "endDate" to endDate,
                            "assignmentGroupIds" to assignmentGroupIds,
                        ),
                    )
            }

        // Save the task status to get an ID
        val savedTaskStatus = taskStatusRepository.save(taskStatus)

        // Create AssignmentDto for additionalParams
        val assignmentDto =
            AssignmentDto(
                title = title,
                description = description,
                taskId = taskId,
                maxPoints = maxPoints,
                startDate = startDate,
                endDate = endDate,
                assignmentGroupIds = assignmentGroupIds,
            )

        // Create a message
        val message =
            AssignmentTaskMessage(
                taskId = savedTaskStatus.id,
                assignmentNumber = assignmentNumber,
                additionalParams = assignmentDto,
            )

        // Send the message to the queue
        sendTaskToQueue(message)

        return savedTaskStatus.id
    }

    /**
     * Enqueues a solution verification task and returns the task ID
     */
    fun enqueueSolutionVerificationTask(
        file: MultipartFile,
        assignmentNumber: Int,
        timeoutSeconds: Long = 300,
    ): Long {
        // Save the file to a temporary location
        val executionId = UUID.randomUUID().toString()
        val tempDir = Files.createTempDirectory("verification-$executionId")
        val targetFile = tempDir.resolve(file.originalFilename!!)
        file.transferTo(targetFile.toFile())

        // Create a task status record
        val taskStatus =
            TaskStatus().apply {
                taskType = "SOLUTION_VERIFICATION"
                status = PendingStatus.PENDING
                createdAt = LocalDateTime.now()
                parameters =
                    objectMapper.writeValueAsString(
                        mapOf(
                            "filePath" to targetFile.toString(),
                            "originalFilename" to file.originalFilename,
                            "timeoutSeconds" to timeoutSeconds,
                            "assignmentNumber" to assignmentNumber,
                        ),
                    )
            }

        // Save the task status to get an ID
        val savedTaskStatus = taskStatusRepository.save(taskStatus)

        // Create VerificationTaskDto for additionalParams
        val verificationTaskDto =
            VerificationTaskDto(
                filePath = targetFile.toString(),
                originalFilename = file.originalFilename!!,
                timeoutSeconds = timeoutSeconds,
                assignmentNumber = assignmentNumber,
            )

        // Create a message
        val message =
            VerificationTaskMessage(
                taskId = savedTaskStatus.id,
                filePath = targetFile.toString(),
                originalFilename = file.originalFilename!!,
                timeoutSeconds = timeoutSeconds,
                assignmentNumber = assignmentNumber,
                additionalParams = verificationTaskDto,
            )

        // Send the message to the queue
        sendTaskToQueue(message)

        return savedTaskStatus.id
    }

    /**
     * Sends a task message to the appropriate queue
     */
    private fun sendTaskToQueue(message: TaskMessage<*>) {
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

    /**
     * Gets the status of an assignment creation task
     * @param taskId The task ID
     * @return A response with the task status and assignment ID if available
     */
    fun getAssignmentCreationStatus(taskId: Long): PendingStatusResponse<Long?> {
        val taskStatus = getTaskStatus(taskId)
        return PendingStatusResponse(
            payload = ResponsePayload(taskStatus.resultId),
            status = taskStatus.status,
        )
    }
}
