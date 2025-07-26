package org.jikvict.jikvictbackend.service.processor

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.model.dto.VerificationTaskDto
import org.jikvict.jikvictbackend.model.queue.VerificationTaskMessage
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.service.AssignmentService
import org.jikvict.jikvictbackend.service.SolutionChecker
import org.jikvict.jikvictbackend.service.TaskQueueService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class VerificationTaskProcessor(
    private val solutionChecker: SolutionChecker,
    private val taskQueueService: TaskQueueService,
    private val assignmentService: AssignmentService,
    private val log: Logger,
) : TaskProcessor<VerificationTaskDto, VerificationTaskMessage> {
    override val taskType: String = "SOLUTION_VERIFICATION"
    override val queueName: String = "verification.queue"
    override val exchangeName: String = "verification.exchange"
    override val routingKey: String = "verification.routingkey"

    @RabbitListener(queues = ["verification.queue"])
    override fun process(message: VerificationTaskMessage) {
        log.info("Processing solution verification task: ${message.originalFilename} for assignment ${message.assignmentNumber}")

        try {
            // Update task status to in-progress
            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.PENDING,
                "Verifying solution: ${message.originalFilename} for assignment ${message.assignmentNumber}",
            )

            // Get the file from the path
            val filePath = Paths.get(message.filePath)
            val file = filePath.toFile()

            if (!file.exists()) {
                throw IllegalArgumentException("File not found: ${message.filePath}")
            }

            // Get hidden files for the assignment
            log.info("Retrieving hidden files for assignment ${message.assignmentNumber}")
            val hiddenFilesBytes = assignmentService.getHiddenFilesForAssignment(message.assignmentNumber)

            // Get exposed files for the assignment
            log.info("Retrieving exposed files for assignment ${message.assignmentNumber}")

            // Create a mock MultipartFile from the merged archive
            val multipartFile =
                object : MultipartFile {
                    override fun getName(): String = "file"

                    override fun getOriginalFilename(): String = message.originalFilename

                    override fun getContentType(): String? = "application/zip"

                    override fun isEmpty(): Boolean = file.length() == 0L

                    override fun getSize(): Long = file.length()

                    override fun getBytes(): ByteArray = Files.readAllBytes(filePath)

                    override fun getInputStream(): java.io.InputStream = file.inputStream()

                    override fun transferTo(dest: File) {
                        Files.copy(filePath, dest.toPath())
                    }

                    override fun transferTo(dest: Path) {
                        Files.copy(filePath, dest)
                    }
                }
            val hiddenFilesMultipartFile =
                object : MultipartFile {
                    override fun getName(): String = "hiddenFiles"

                    override fun getOriginalFilename(): String = "hidden_files.zip"

                    override fun getContentType(): String? = "application/zip"

                    override fun isEmpty(): Boolean = hiddenFilesBytes.isEmpty()

                    override fun getSize(): Long = hiddenFilesBytes.size.toLong()

                    override fun getBytes(): ByteArray = hiddenFilesBytes

                    override fun getInputStream() = ByteArrayInputStream(hiddenFilesBytes)

                    override fun transferTo(dest: File) {
                        Files.write(dest.toPath(), hiddenFilesBytes)
                    }

                    override fun transferTo(dest: Path) {
                        Files.write(dest, hiddenFilesBytes)
                    }
                }

            // Execute the solution checker with the merged archive
            log.info("Executing solution checker with merged archive")
            solutionChecker.executeCode(multipartFile, hiddenFilesMultipartFile, message.timeoutSeconds)

            // If we reach here, no exception was thrown, so we assume success
            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.DONE,
                "Solution verification completed successfully",
            )

            log.info("Solution verification completed: ${message.taskId}")
        } catch (e: Exception) {
            log.error("Error verifying solution: ${e.message}", e)
            taskQueueService.updateTaskStatus(
                message.taskId,
                PendingStatus.FAILED,
                "Error verifying solution: ${e.message}",
            )
        } finally {
            // Clean up the temporary file
            try {
                val filePath = Paths.get(message.filePath)
                val parentDir = filePath.parent
                if (Files.exists(filePath)) {
                    Files.delete(filePath)
                }
                if (Files.exists(parentDir)) {
                    Files.delete(parentDir)
                }
            } catch (e: Exception) {
                log.error("Error cleaning up temporary file: ${e.message}", e)
            }
        }
    }
}
