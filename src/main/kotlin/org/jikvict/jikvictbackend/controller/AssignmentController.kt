package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.model.dto.AssignmentDto
import org.jikvict.jikvictbackend.model.dto.CreateAssignmentDto
import org.jikvict.jikvictbackend.model.mapper.AssignmentMapper
import org.jikvict.jikvictbackend.model.response.AssignmentInfo
import org.jikvict.jikvictbackend.model.response.PendingStatusResponse
import org.jikvict.jikvictbackend.model.response.ResponsePayload
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.service.AssignmentService
import org.jikvict.jikvictbackend.service.queue.AssignmentTaskQueueService
import org.jikvict.problems.exception.contract.ServiceException
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
    private val assignmentTaskQueueService: AssignmentTaskQueueService,
) {
    @GetMapping("/{id}/info")
    fun getAssignmentInfoForUser(
        @PathVariable id: Long,
    ): ResponseEntity<AssignmentInfo> = ResponseEntity.ok(assignmentService.getAssignmentInfoForUser(id))

    @GetMapping("/zip/{assignmentId}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadZip(
        @PathVariable assignmentId: Long,
    ): ResponseEntity<ByteArray> {
        val zipBytes = assignmentService.getZip(assignmentId)

        val filename = "assignment_$assignmentId.zip"

        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(zipBytes.size.toLong())
            .body(zipBytes)
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
        val assignment = assignmentService.getAssignmentByIdForCurrentUser(id)
        return ResponseEntity.ok(assignment)
    }

    @GetMapping("/all", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAll(): List<AssignmentDto> {
        val assignments = assignmentService.getAllAssignmentsForUser()
        val assignmentDtoPage = assignments.map(assignmentMapper::toDto)
        return assignmentDtoPage
    }

    /**
     * Creates an assignment synchronously
     * @param assignmentDto The assignment data
     * @return The created assignment
     */
    @PostMapping
    fun createAssignment(
        @RequestBody assignmentDto: CreateAssignmentDto,
    ): ResponseEntity<PendingStatusResponse<Long>> {
        val result = assignmentTaskQueueService.enqueueAssignmentCreationTask(assignmentDto)
        val response =
            PendingStatusResponse(
                payload = ResponsePayload(result.id),
                status = result.status,
                message = result.parameters,
            )
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response)
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
