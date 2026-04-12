package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.annotation.AnyTeacher
import org.jikvict.jikvictbackend.annotation.RWTeacher
import org.jikvict.jikvictbackend.model.request.PlagiarismCheckParameters
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.model.response.PendingStatusResponse
import org.jikvict.jikvictbackend.model.response.PlagiarismCheckSummaryResponse
import org.jikvict.jikvictbackend.model.response.ResponsePayload
import org.jikvict.jikvictbackend.service.plagiarism.PlagiarismCheckService
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/plagiarism")
class PlagiarismController(
    private val plagiarismCheckService: PlagiarismCheckService,
) {
    @RWTeacher
    @PostMapping("/check")
    fun startCheck(
        @RequestParam assignmentId: Long,
        @RequestBody(required = false) parameters: PlagiarismCheckParameters?,
    ): ResponseEntity<Map<String, Long>> {
        val taskId = plagiarismCheckService.startCheck(assignmentId, parameters)
        return ResponseEntity.ok(mapOf("taskId" to taskId))
    }

    @GetMapping("/checks")
    @AnyTeacher
    fun listChecks(
        @RequestParam assignmentId: Long,
    ): ResponseEntity<List<PlagiarismCheckSummaryResponse>> =
        ResponseEntity.ok(plagiarismCheckService.listChecksForAssignment(assignmentId))

    @GetMapping("/check/{taskId}")
    @AnyTeacher
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
    @AnyTeacher
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
