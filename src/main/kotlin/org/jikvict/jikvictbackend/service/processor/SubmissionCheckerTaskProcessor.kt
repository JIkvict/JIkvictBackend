package org.jikvict.jikvictbackend.service.processor

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.model.dto.VerificationTaskDto
import org.jikvict.jikvictbackend.model.queue.VerificationTaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.UserRepository
import org.jikvict.jikvictbackend.service.assignment.AssignmentResultService
import org.jikvict.jikvictbackend.service.queue.SubmissionCheckerTaskQueueService
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.jikvict.jikvictbackend.service.solution.SubmissionCheckerUserService
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service
class SubmissionCheckerTaskProcessor(
    private val taskQueueService: SubmissionCheckerTaskQueueService,
    private val log: Logger,
    private val userRepository: UserRepository,
    private val assignmentResultService: AssignmentResultService,
    private val assignmentRepository: AssignmentRepository,
    private val userSolutionChecker: SubmissionCheckerUserService,
    private val taskRegistry: TaskRegistry,
) : TaskProcessor<VerificationTaskDto, VerificationTaskMessage> {
    override val taskType: String = "SOLUTION_VERIFICATION"
    override val queueName: String = "verification.queue"
    override val exchangeName: String = "verification.exchange"
    override val routingKey: String = "verification.routingkey"

    @RabbitListener(queues = ["verification.queue"], containerFactory = "manualAckContainerFactory")
    suspend fun process(message: VerificationTaskMessage) {
        if (taskQueueService.isTaskCancelled(message.taskId)) {
            return
        }
        try {
            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.PENDING,
                "Verifying solution: for assignment ${message.additionalParams.assignmentId}",
            )

            val assignmentEntity =
                assignmentRepository.findPropsById(message.additionalParams.assignmentId.toLong()) ?: throw ServiceException(
                    HttpStatus.NOT_FOUND,
                    "Assignment with ID ${message.additionalParams.assignmentId} not found",
                )

            val user =
                userRepository.findUserById(message.additionalParams.userId) ?: throw ServiceException(
                    HttpStatus.NOT_FOUND,
                    "User not found",
                )

            val result =
                withContext(Dispatchers.IO) {
                    userSolutionChecker.checkSubmission(
                        assignmentEntity.id,
                        message.additionalParams.solutionBytes,
                        user,
                    )
                }

            assignmentResultService.handleAssignmentResult(assignmentEntity.id, result, user)

            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.DONE,
                "Solution verification completed successfully result: \n$result",
            )

            log.info("Solution verification completed: ${message.taskId}")
            log.info("Solution verification result: $result")
        } catch (e: Exception) {
            log.error("Error verifying solution: ${e.message}", e)
            if (e is ServiceException) {
                when {
                    e.status.is4xxClientError ->
                        taskQueueService.updateTaskStatus(
                            message.taskId,
                            PendingStatus.REJECTED,
                            "Submission rejected: ${e.message}",
                        )

                    else ->
                        taskQueueService.updateTaskStatus(
                            message.taskId,
                            PendingStatus.FAILED,
                            "Error verifying solution: ${e.message}",
                        )
                }
            } else {
                taskQueueService.updateTaskStatus(
                    message.taskId,
                    PendingStatus.FAILED,
                    "Error verifying solution: ${e.message}",
                )
            }
        }
    }

    @PostConstruct
    fun init() {
        taskRegistry.registerProcessor(this)
    }
}
