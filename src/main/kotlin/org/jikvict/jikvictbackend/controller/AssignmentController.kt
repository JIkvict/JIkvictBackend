package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.model.response.PendingStatusResponse
import org.jikvict.jikvictbackend.service.AssignmentService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/assignment")
class AssignmentController(
    private val assignmentService: AssignmentService,
) {
    @GetMapping("/zip/{taskId}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadZip(
        @PathVariable taskId: String,
    ): ResponseEntity<ByteArray> {
        val zipBytes = assignmentService.cloneZipBytes(listOf("task$taskId/.*".toRegex()))

        val filename = "assignment_$taskId.zip"

        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(zipBytes.size.toLong())
            .body(zipBytes)
    }

    @GetMapping("/description/{taskId}", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getAssignmentDescription(
        @PathVariable taskId: Int,
    ): ResponseEntity<String> {
        val description = assignmentService.getAssignmentDescription(taskId)
        return ResponseEntity.ok(description)
    }

    /**
     * Creates an assignment asynchronously
     * @param assignmentNumber The assignment number
     * @return A response with the task ID and pending status
     */
    @PostMapping("/create/{assignmentNumber}")
    fun createAssignment(
        @PathVariable assignmentNumber: Int,
    ): ResponseEntity<PendingStatusResponse<Long>> {
        val response = assignmentService.createAssignmentByNumber(assignmentNumber)
        return ResponseEntity.accepted().body(response)
    }

    /**
     * Gets an assignment by ID
     * @param id The assignment ID
     * @return The assignment
     */
    @GetMapping("/{id}")
    fun getAssignment(
        @PathVariable id: Long,
    ): ResponseEntity<Assignment> {
        val assignment = assignmentService.getAssignmentById(id)
        return ResponseEntity.ok(assignment)
    }
}
