package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.model.response.PendingStatusResponse
import org.jikvict.jikvictbackend.model.response.ResponsePayload
import org.jikvict.jikvictbackend.service.ZipValidatorService
import org.jikvict.jikvictbackend.service.queue.SolutionVerificationTaskQueueService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/solution-checker")
class SolutionCheckerController(
    private val zipValidatorService: ZipValidatorService,
    private val solutionVerificationTaskQueueService: SolutionVerificationTaskQueueService,
) {
    @PostMapping("/submit", consumes = ["multipart/form-data"])
    fun submitSolution(
        @RequestParam file: MultipartFile,
        @RequestParam assignmentId: Int,
    ): ResponseEntity<PendingStatusResponse<Long>> {
        zipValidatorService.validateZipArchive(file)

        val taskId = solutionVerificationTaskQueueService.enqueueSolutionVerificationTask(file, assignmentId)

        return ResponseEntity.accepted().body(
            PendingStatusResponse(
                payload = ResponsePayload(taskId),
                status = PendingStatus.PENDING,
            ),
        )
    }
}
