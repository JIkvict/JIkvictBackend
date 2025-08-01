package org.jikvict.jikvictbackend.service.processor

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.model.dto.VerificationTaskDto
import org.jikvict.jikvictbackend.model.queue.VerificationTaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.UserRepository
import org.jikvict.jikvictbackend.service.AssignmentResultService
import org.jikvict.jikvictbackend.service.SolutionChecker
import org.jikvict.jikvictbackend.service.queue.SolutionVerificationTaskQueueService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrElse

@Service
class VerificationTaskProcessor(
    private val solutionChecker: SolutionChecker,
    private val taskQueueService: SolutionVerificationTaskQueueService,
    private val log: Logger,
    private val userRepository: UserRepository,
    private val assignmentResultService: AssignmentResultService,
    private val assignmentRepository: AssignmentRepository,
) : TaskProcessor<VerificationTaskDto, VerificationTaskMessage> {
    override val taskType: String = "SOLUTION_VERIFICATION"
    override val queueName: String = "verification.queue"
    override val exchangeName: String = "verification.exchange"
    override val routingKey: String = "verification.routingkey"

    @RabbitListener(queues = ["verification.queue"])
    override fun process(message: VerificationTaskMessage) {
        try {
            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.PENDING,
                "Verifying solution: for assignment ${message.additionalParams.assignmentId}",
            )
            val assignmentEntity =
                assignmentRepository.findById(message.additionalParams.assignmentId.toLong()).getOrElse {
                    taskQueueService.updateTaskStatus(
                        message.taskId,
                        PendingStatus.FAILED,
                        "Failed to find assignment: ${message.additionalParams.assignmentId}",
                    )
                    return
                }!!
            val result =
                runCatching {
                    solutionChecker.checkSolution(assignmentEntity.taskId, message.additionalParams.solutionBytes, assignmentEntity.timeOutSeconds)
                }.onFailure {
                    taskQueueService.updateTaskStatus(
                        message.taskId,
                        PendingStatus.FAILED,
                        "Failed to verify solution: ${it.message}",
                    )
                    return
                }.getOrNull()!!
            val user =
                userRepository.findById(message.additionalParams.userId).getOrElse {
                    taskQueueService.updateTaskStatus(
                        message.taskId,
                        PendingStatus.FAILED,
                        "Failed to find user: ${message.additionalParams.userId}",
                    )
                    return
                }!!
            runCatching {
                assignmentResultService.handleAssignmentResult(result, assignmentEntity.id, user)
            }.onFailure {
                taskQueueService.updateTaskStatus(
                    message.taskId,
                    PendingStatus.FAILED,
                    "Failed to save result: ${it.message}",
                )
                log.error("Failed to save result: $it")
                it.printStackTrace()
                return
            }

            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.DONE,
                "Solution verification completed successfully result: \n$result",
            )

            log.info("Solution verification completed: ${message.taskId}")
            log.info("Solution verification result: $result")
        } catch (e: Exception) {
            log.error("Error verifying solution: ${e.message}", e)
            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.FAILED,
                "Error verifying solution: ${e.message}",
            )
        }
    }
}
