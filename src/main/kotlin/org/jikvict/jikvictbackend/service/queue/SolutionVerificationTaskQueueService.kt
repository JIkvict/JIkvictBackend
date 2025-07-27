package org.jikvict.jikvictbackend.service.queue

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.model.dto.VerificationTaskDto
import org.jikvict.jikvictbackend.model.queue.VerificationTaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.UUID

@Service
class SolutionVerificationTaskQueueService(
    rabbitTemplate: RabbitTemplate,
    private val taskStatusRepository: TaskStatusRepository,
    taskRegistry: TaskRegistry,
    log: Logger,
    private val objectMapper: ObjectMapper,
) : TaskQueueService(rabbitTemplate, taskStatusRepository, taskRegistry, log) {
    fun enqueueSolutionVerificationTask(
        file: MultipartFile,
        assignmentNumber: Int,
        timeoutSeconds: Long = 300,
    ): Long {
        val executionId = UUID.randomUUID().toString()
        val tempDir = Files.createTempDirectory("verification-$executionId")
        val targetFile = tempDir.resolve(file.originalFilename!!)
        file.transferTo(targetFile.toFile())

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

        val savedTaskStatus = taskStatusRepository.save(taskStatus)

        val verificationTaskDto =
            VerificationTaskDto(
                filePath = targetFile.toString(),
                originalFilename = file.originalFilename!!,
                timeoutSeconds = timeoutSeconds,
                assignmentNumber = assignmentNumber,
            )

        val message =
            VerificationTaskMessage(
                taskId = savedTaskStatus.id,
                filePath = targetFile.toString(),
                originalFilename = file.originalFilename!!,
                timeoutSeconds = timeoutSeconds,
                assignmentNumber = assignmentNumber,
                additionalParams = verificationTaskDto,
            )

        sendTaskToQueue(message)

        return savedTaskStatus.id
    }
}
