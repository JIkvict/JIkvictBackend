package org.jikvict.jikvictbackend.service.queue

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.model.dto.AssignmentDto
import org.jikvict.jikvictbackend.model.queue.AssignmentTaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AssignmentTaskQueueService(
    rabbitTemplate: RabbitTemplate,
    private val taskStatusRepository: TaskStatusRepository,
    taskRegistry: TaskRegistry,
    log: Logger,
    private val objectMapper: ObjectMapper,
) : TaskQueueService(rabbitTemplate, taskStatusRepository, taskRegistry, log) {
    fun enqueueAssignmentCreationTask(assignmentDto: AssignmentDto): TaskStatus {
        val taskStatus =
            TaskStatus().apply {
                taskType = "ASSIGNMENT_CREATION"
                status = PendingStatus.PENDING
                createdAt = LocalDateTime.now()
                parameters = objectMapper.writeValueAsString(assignmentDto)
            }

        val savedTaskStatus = taskStatusRepository.save(taskStatus)

        val message =
            AssignmentTaskMessage(
                taskId = savedTaskStatus.id,
                additionalParams = assignmentDto,
            )

        sendTaskToQueue(message)
        return savedTaskStatus
    }
}
