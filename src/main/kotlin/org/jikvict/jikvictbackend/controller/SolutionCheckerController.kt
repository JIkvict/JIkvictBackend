package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.model.response.PendingStatusResponse
import org.jikvict.jikvictbackend.model.response.ResponsePayload
import org.jikvict.jikvictbackend.service.TaskQueueService
import org.jikvict.jikvictbackend.service.ZipValidatorService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/solution-checker")
class SolutionCheckerController(
    private val zipValidatorService: ZipValidatorService,
    private val taskQueueService: TaskQueueService,
) {

    /**
     * Submit a solution for verification (asynchronous)
     * @param file The solution file to verify
     * @param assignmentNumber The assignment number
     * @param timeoutSeconds The timeout in seconds (default: 300)
     * @return A response with the task ID and pending status
     */
    @PostMapping("/submit", consumes = ["multipart/form-data"])
    fun submitSolution(
        @RequestParam file: MultipartFile,
        @RequestParam assignmentNumber: Int,
        @RequestParam(required = false, defaultValue = "300") timeoutSeconds: Long,
    ): ResponseEntity<PendingStatusResponse<Long>> {
        // Validate the ZIP archive
        zipValidatorService.validateZipArchive(file)

        // Enqueue the verification task
        val taskId = taskQueueService.enqueueSolutionVerificationTask(file, assignmentNumber, timeoutSeconds)

        // Return the task ID and pending status
        return ResponseEntity.accepted().body(
            PendingStatusResponse(
                payload = ResponsePayload(taskId),
                status = org.jikvict.jikvictbackend.model.response.PendingStatus.PENDING,
            ),
        )
    }

    /**
     * Get the status of a verification task
     * @param taskId The task ID
     * @return A response with the task status
     */
    @GetMapping("/status/{taskId}")
    fun getVerificationStatus(
        @PathVariable taskId: Long,
    ): ResponseEntity<PendingStatusResponse<Long?>> {
        val taskStatus = taskQueueService.getTaskStatus(taskId)
        return ResponseEntity.ok(
            PendingStatusResponse(
                payload = ResponsePayload(taskStatus.resultId),
                status = taskStatus.status,
            ),
        )
    }
}
