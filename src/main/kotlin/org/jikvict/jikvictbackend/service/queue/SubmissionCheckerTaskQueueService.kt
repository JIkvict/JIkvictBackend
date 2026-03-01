package org.jikvict.jikvictbackend.service.queue

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.dto.VerificationTaskDto
import org.jikvict.jikvictbackend.model.queue.VerificationTaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.AssignmentResultRepository
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.service.UserDetailsServiceImpl
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Service
class SubmissionCheckerTaskQueueService(
    rabbitTemplate: RabbitTemplate,
    private val taskStatusRepository: TaskStatusRepository,
    private val assignmentResultRepository: AssignmentResultRepository,
    private val assignmentRepository: AssignmentRepository,
    taskRegistry: TaskRegistry,
    log: Logger,
    private val objectMapper: ObjectMapper,
    private val userDetailsService: UserDetailsServiceImpl,
) : TaskQueueService(rabbitTemplate, taskStatusRepository, taskRegistry, log) {
    fun enqueueSolutionVerificationTask(
        file: MultipartFile,
        assignmentId: Int,
    ): Long {
        val user = userDetailsService.getCurrentUser()
        if (isAlreadyQueuedForUser(user)) {
            throw IllegalStateException("You already have a pending task")
        }
        return enqueue(user, assignmentId, file.bytes, file.originalFilename)
    }

    fun enqueueRetry(
        user: User,
        assignmentId: Int,
        solutionBytes: ByteArray,
        originalFilename: String?,
    ): Long = enqueue(user, assignmentId, solutionBytes, originalFilename)

    private fun enqueue(
        user: User,
        assignmentId: Int,
        solutionBytes: ByteArray,
        originalFilename: String?,
    ): Long {
        val taskStatus =
            TaskStatus().apply {
                taskType = "SOLUTION_VERIFICATION"
                status = PendingStatus.PENDING
                createdAt = LocalDateTime.now()
                parameters =
                    objectMapper.writeValueAsString(
                        mapOf(
                            "originalFilename" to originalFilename,
                            "assignmentId" to assignmentId,
                        ),
                    )
                this.user = user
            }
        val savedTaskStatus = taskStatusRepository.save(taskStatus)

        val verificationTaskDto =
            VerificationTaskDto(
                assignmentId = assignmentId,
                userId = user.id,
                solutionBytes = solutionBytes,
            )

        val message =
            VerificationTaskMessage(
                taskId = savedTaskStatus.id,
                additionalParams = verificationTaskDto,
            )

        sendTaskToQueue(message)

        return savedTaskStatus.id
    }

    @Transactional
    fun retryFailedSubmissions(targetAssignmentId: Long? = null): Int {
        val allTasks = taskStatusRepository.findAllByTaskTypeOrderByCreatedAtDesc("SOLUTION_VERIFICATION")
        val processedPairs = mutableSetOf<Pair<Long, Long>>()
        val tasksToRetry = mutableListOf<TaskStatus>()

        for (task in allTasks) {
            val userId = task.user.id
            val assignmentId = extractAssignmentId(task.parameters) ?: continue

            if (targetAssignmentId != null && assignmentId != targetAssignmentId) continue

            val pair = userId to assignmentId
            if (pair in processedPairs) continue
            processedPairs.add(pair)

            if (task.status == PendingStatus.FAILED) {
                tasksToRetry.add(task)
            }
        }

        var retriedCount = 0
        for (task in tasksToRetry) {
            val assignmentId = extractAssignmentId(task.parameters) ?: continue
            val result =
                if (task.resultId != null) {
                    assignmentResultRepository.findById(task.resultId!!).getOrNull()
                } else {
                    val assignment = assignmentRepository.findById(assignmentId).getOrNull() ?: continue
                    assignmentResultRepository.findFirstByUserAndAssignmentOrderByTimeStampDesc(task.user, assignment)
                }

            val solutionBytes = result?.zipFile ?: continue
            val originalFilename = extractOriginalFilename(task.parameters)

            enqueueRetry(task.user, assignmentId.toInt(), solutionBytes, originalFilename)
            retriedCount++
        }

        return retriedCount
    }

    private fun extractAssignmentId(parametersJson: String?): Long? =
        try {
            val node = objectMapper.readTree(parametersJson)
            node?.get("assignmentId")?.asLong()
        } catch (_: Exception) {
            null
        }

    private fun extractOriginalFilename(parametersJson: String?): String? =
        try {
            val node = objectMapper.readTree(parametersJson)
            node?.get("originalFilename")?.asText()
        } catch (_: Exception) {
            null
        }
}
