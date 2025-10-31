package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.model.dto.PendingSubmissionDto
import org.jikvict.jikvictbackend.model.response.PendingStatusResponse
import org.jikvict.jikvictbackend.service.UserDetailsServiceImpl
import org.jikvict.jikvictbackend.service.queue.GeneralTaskQueueService
import org.jikvict.jikvictbackend.service.solution.SubmissionCheckerUserService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/task-status")
class TaskStatusController(
    private val taskQueueService: GeneralTaskQueueService,
    private val userDetailsService: UserDetailsServiceImpl,
    private val submissionCheckerUserService: SubmissionCheckerUserService,
) {
    /**
     * Gets the status of an any task
     * @param taskId The task ID
     * @return A response with the task status and assignment ID if available
     */
    @GetMapping("/status/{taskId}")
    fun getTaskStatus(
        @PathVariable taskId: Long,
    ): ResponseEntity<PendingStatusResponse<Long?>> {
        val response = taskQueueService.getTaskStatusResponse(taskId, userDetailsService.getCurrentUser())
        return ResponseEntity.ok(response)
    }

    @GetMapping("/pending")
    fun getPendingTask(): ResponseEntity<PendingSubmissionDto?> = ResponseEntity.ok(submissionCheckerUserService.getPendingSubmissions())

    @PostMapping("/cancel/{taskId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun cancelPendingSubmission(
        @PathVariable taskId: Long,
    ): ResponseEntity<Unit> {
        submissionCheckerUserService.cancelPendingSubmission(taskId)
        return ResponseEntity.ok().build()
    }
}
