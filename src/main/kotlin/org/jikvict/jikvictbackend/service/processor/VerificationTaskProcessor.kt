package org.jikvict.jikvictbackend.service.processor

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.model.dto.VerificationTaskDto
import org.jikvict.jikvictbackend.model.queue.VerificationTaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.service.SolutionChecker
import org.jikvict.jikvictbackend.service.queue.SolutionVerificationTaskQueueService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class VerificationTaskProcessor(
    private val solutionChecker: SolutionChecker,
    private val taskQueueService: SolutionVerificationTaskQueueService,
    private val log: Logger,
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
                "Verifying solution: for assignment ${message.additionalParams.assignmentNumber}",
            )
            val result = solutionChecker.checkSolution(message.additionalParams.assignmentNumber, message.additionalParams.solutionBytes, message.additionalParams.timeoutSeconds)

            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.DONE,
                "Solution verification completed successfully",
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
