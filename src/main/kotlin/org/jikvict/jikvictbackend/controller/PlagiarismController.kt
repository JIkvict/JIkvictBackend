package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.model.response.PendingStatusResponse
import org.jikvict.jikvictbackend.model.response.ResponsePayload
import org.jikvict.jikvictbackend.service.plagiarism.PlagiarismCheckService
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/plagiarism")
@PreAuthorize("hasRole('ADMIN')")
class PlagiarismController(
    private val plagiarismCheckService: PlagiarismCheckService,
) {
    @PostMapping("/check")
    fun startCheck(
        @RequestParam assignmentId: Long,
    ): ResponseEntity<Map<String, Long>> {
        val taskId = plagiarismCheckService.startCheck(assignmentId)
        return ResponseEntity.ok(mapOf("taskId" to taskId))
    }

    @GetMapping("/check/{taskId}")
    fun getStatus(
        @PathVariable taskId: Long,
    ): ResponseEntity<PendingStatusResponse<Long?>> {
        val taskStatus = plagiarismCheckService.getTaskStatus(taskId)
        if (taskStatus.taskType != PlagiarismCheckService.TASK_TYPE) {
            throw ServiceException(HttpStatus.NOT_FOUND, "Task $taskId is not a plagiarism check task")
        }
        val response =
            PendingStatusResponse<Long?>(
                payload = ResponsePayload(data = taskStatus.resultId),
                status = taskStatus.status,
                message = taskStatus.message,
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/check/{taskId}/report")
    fun getReport(
        @PathVariable taskId: Long,
    ): ResponseEntity<ByteArray> {
        val taskStatus = plagiarismCheckService.getTaskStatus(taskId)
        if (taskStatus.taskType != PlagiarismCheckService.TASK_TYPE) {
            throw ServiceException(HttpStatus.NOT_FOUND, "Task $taskId is not a plagiarism check task")
        }
        if (taskStatus.status != PendingStatus.DONE) {
            throw ServiceException(HttpStatus.CONFLICT, "Task $taskId is not done yet, current status: ${taskStatus.status}")
        }
        val resultId =
            taskStatus.resultId
                ?: throw ServiceException(HttpStatus.NOT_FOUND, "No result available for task $taskId")

        val reportZip = plagiarismCheckService.loadResultZip(resultId)
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_OCTET_STREAM
                set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plagiarism-report-assignment-${taskStatus.parameters}.zip\"")
            }
        return ResponseEntity(reportZip, headers, HttpStatus.OK)
    }
}
