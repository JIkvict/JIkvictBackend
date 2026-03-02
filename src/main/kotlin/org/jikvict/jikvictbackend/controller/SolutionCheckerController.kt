package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.model.response.PendingStatusResponse
import org.jikvict.jikvictbackend.model.response.ResponsePayload
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.service.ZipValidatorService
import org.jikvict.jikvictbackend.service.queue.SubmissionCheckerTaskQueueService
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpStatus
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
    private val solutionVerificationTaskQueueService: SubmissionCheckerTaskQueueService,
    private val assignmentRepository: AssignmentRepository,
) {
    @PostMapping("/submit", consumes = ["multipart/form-data"])
    fun submitSolution(
        @RequestParam file: MultipartFile,
        @RequestParam assignmentId: Int,
    ): ResponseEntity<PendingStatusResponse<Long>> {
        val assignmentProps = assignmentRepository.findPropsById(assignmentId.toLong())
            ?: throw ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $assignmentId not found")

        zipValidatorService.validateZipArchive(file, assignmentProps.taskId)

        val taskId = solutionVerificationTaskQueueService.enqueueSolutionVerificationTask(file, assignmentId)

        return ResponseEntity.accepted().body(
            PendingStatusResponse(
                payload = ResponsePayload(taskId),
                status = PendingStatus.PENDING,
            ),
        )
    }
}
