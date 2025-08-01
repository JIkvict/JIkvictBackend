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
import java.time.LocalDateTime

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
        assignmentId: Int,
        taskId: Int,
    ): Long {
        val taskStatus =
            TaskStatus().apply {
                taskType = "SOLUTION_VERIFICATION"
                status = PendingStatus.PENDING
                createdAt = LocalDateTime.now()
                parameters =
                    objectMapper.writeValueAsString(
                        mapOf(
                            "originalFilename" to file.originalFilename,
                            "assignmentId" to assignmentId,
                            "taskId" to taskId,
                        ),
                    )
            }

        val savedTaskStatus = taskStatusRepository.save(taskStatus)

        val verificationTaskDto =
            VerificationTaskDto(
                assignmentId = assignmentId,
                taskId = taskId,
                solutionBytes = file.bytes,
            )

        val message =
            VerificationTaskMessage(
                taskId = savedTaskStatus.id,
                additionalParams = verificationTaskDto,
            )

        sendTaskToQueue(message)

        return savedTaskStatus.id
    }
}
