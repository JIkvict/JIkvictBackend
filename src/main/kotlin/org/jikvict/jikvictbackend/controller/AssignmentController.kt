package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.model.dto.AssignmentDto
import org.jikvict.jikvictbackend.model.mapper.AssignmentMapper
import org.jikvict.jikvictbackend.model.response.PendingStatusResponse
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.service.AssignmentService
import org.jikvict.problems.exception.contract.ServiceException
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/assignment")
class AssignmentController(
    private val assignmentService: AssignmentService,
    private val assignmentRepository: AssignmentRepository,
    private val assignmentMapper: AssignmentMapper,
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

    @GetMapping("/all", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAll(
        @ParameterObject pageable: Pageable,
    ): PagedModel<AssignmentDto> {
        val assignments: Page<Assignment> = assignmentRepository.findAll(pageable)
        val assignmentDtoPage: Page<AssignmentDto> = assignments.map(assignmentMapper::toDto)
        return PagedModel(assignmentDtoPage)
    }

    /**
     * Creates an assignment synchronously
     * @param assignmentDto The assignment data
     * @return The created assignment
     */
    @PostMapping
    fun createAssignment(
        @RequestBody assignmentDto: AssignmentDto,
    ): ResponseEntity<AssignmentDto> {
        val assignment = assignmentMapper.toEntity(assignmentDto)
        val savedAssignment = assignmentRepository.save(assignment)
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentMapper.toDto(savedAssignment))
    }

    /**
     * Updates an assignment
     * @param id The assignment ID
     * @param assignmentDto The updated assignment data
     * @return The updated assignment
     */
    @PutMapping("/{id}")
    fun updateAssignment(
        @PathVariable id: Long,
        @RequestBody assignmentDto: AssignmentDto,
    ): ResponseEntity<AssignmentDto> {
        if (!assignmentRepository.existsById(id)) {
            throw ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $id not found")
        }

        val assignment = assignmentMapper.toEntity(assignmentDto)
        // Ensure the ID is set correctly
        assignment.id = id

        val updatedAssignment = assignmentRepository.save(assignment)
        return ResponseEntity.ok(assignmentMapper.toDto(updatedAssignment))
    }

    /**
     * Deletes an assignment
     * @param id The assignment ID
     * @return No content response
     */
    @DeleteMapping("/{id}")
    fun deleteAssignment(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        if (!assignmentRepository.existsById(id)) {
            throw ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $id not found")
        }

        assignmentRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
