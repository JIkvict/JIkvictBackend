package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.service.AssignmentService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/assignment")
class AssignmentController(
    private val assignmentService: AssignmentService,
) {

    @GetMapping("/zip/{taskId}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadZip(@PathVariable taskId: String): ResponseEntity<ByteArray> {
        val zipBytes = assignmentService.cloneZipBytes(listOf("task$taskId/.*".toRegex()))

        val filename = "assignment_$taskId.zip"

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(zipBytes.size.toLong())
            .body(zipBytes)
    }
}
