package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.annotation.OnlyTeacher
import org.jikvict.jikvictbackend.model.domain.AssignmentInfo
import org.jikvict.jikvictbackend.model.dto.AssignmentDto
import org.jikvict.jikvictbackend.model.dto.CreateAssignmentDto
import org.jikvict.jikvictbackend.model.mapper.AssignmentMapper
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.service.UserDetailsServiceImpl
import org.jikvict.jikvictbackend.service.assignment.AssignmentInfoUserService
import org.jikvict.jikvictbackend.service.assignment.AssignmentService
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/assignment")
class AssignmentController(
    private val assignmentRepository: AssignmentRepository,
    private val assignmentMapper: AssignmentMapper,
    private val userDetailsService: UserDetailsServiceImpl,
    private val assignmentInfoUserService: AssignmentInfoUserService,
    private val assignmentService: AssignmentService,
) {
    @GetMapping("/{id}/info")
    fun getAssignmentInfoForUser(
        @PathVariable id: Long,
    ): ResponseEntity<AssignmentInfo> {
        val user = userDetailsService.getCurrentUser()
        return ResponseEntity.ok(assignmentInfoUserService.getAssignmentInfoForUser(id, user))
    }

    @GetMapping("/zip/{assignmentId}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadZip(
        @PathVariable assignmentId: Long,
    ): ResponseEntity<ByteArray> {
        val user = userDetailsService.getCurrentUser()
        val zipBytes = assignmentInfoUserService.getAssignmentZipForUser(assignmentId, user)

        val filename = "assignment_$assignmentId.zip"

        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(zipBytes.size.toLong())
            .body(zipBytes)
    }

    @GetMapping("/{id}")
    fun getAssignment(
        @PathVariable id: Long,
    ): ResponseEntity<AssignmentDto> {
        val user = userDetailsService.getCurrentUser()
        val assignment = assignmentInfoUserService.getAssignmentByIdForUser(id, user)
        val dto = assignmentMapper.toDto(assignment)
        return ResponseEntity.ok(dto)
    }

    @GetMapping("/all", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAll(): List<AssignmentDto> {
        val user = userDetailsService.getCurrentUser()
        val assignments = assignmentInfoUserService.getAllAssignmentsForUser(user)
        val assignmentDtoPage = assignments.map(assignmentMapper::toDto)
        return assignmentDtoPage
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all-admin", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllAdmin(): List<AssignmentDto> {
        val assignments = assignmentRepository.findAll().asSequence()
        val assignmentDtoPage = assignments.map(assignmentMapper::toDto)
        return assignmentDtoPage.toList()
    }

    @OnlyTeacher
    @GetMapping("/{assignmentGroup}/all")
    fun getAllForAssignmentGroup(
        @PathVariable assignmentGroup: Long,
    ): ResponseEntity<List<AssignmentDto>> {
        val assignments = assignmentService.getAssignmentsForGroup(assignmentGroup)
        val assignmentDtos = assignments.map(assignmentMapper::toDto)
        return ResponseEntity.ok(assignmentDtos)
    }

    @OnlyTeacher
    @GetMapping("/all-for-groups")
    fun getAssignmentsForGroups(
        @RequestParam("groupIds") groupIds: List<Long>,
    ): ResponseEntity<List<AssignmentDto>> {
        val assignments = assignmentService.getAssignmentsForGroups(groupIds.toSet())
        val assignmentDtos = assignments.map(assignmentMapper::toDto)
        return ResponseEntity.ok(assignmentDtos)
    }

    @OnlyTeacher
    @PostMapping
    fun createAssignment(
        @RequestBody assignmentDto: CreateAssignmentDto,
    ): ResponseEntity<AssignmentDto> {
        val result = assignmentService.createAssignment(assignmentDto)
        val dto = assignmentMapper.toDto(result)
        return ResponseEntity.ok(dto)
    }

    @OnlyTeacher
    @GetMapping("/available-tasks")
    fun availableTasks(): ResponseEntity<List<Long>> = ResponseEntity.ok(assignmentService.getAllAvailableTaskIds())

    @OnlyTeacher
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

    @OnlyTeacher
    @PutMapping("/admin/{id}")
    fun getAssignmentAdmin(
        @PathVariable id: Long,
    ): ResponseEntity<AssignmentDto> {
        val assignment = assignmentRepository.findById(id).orElseThrow()
        return ResponseEntity.ok(assignmentMapper.toDto(assignment))
    }

    @OnlyTeacher
    @DeleteMapping("/{id}")
    fun deleteAssignment(
        @PathVariable id: Long,
    ): ResponseEntity<Unit> {
        if (!assignmentRepository.existsById(id)) {
            throw ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $id not found")
        }

        assignmentRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
