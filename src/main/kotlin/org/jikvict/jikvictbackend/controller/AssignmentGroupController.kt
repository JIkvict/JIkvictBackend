package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.model.dto.AssignmentGroupDto
import org.jikvict.jikvictbackend.model.mapper.AssignmentGroupMapper
import org.jikvict.jikvictbackend.repository.AssignmentGroupRepository
import org.jikvict.problems.exception.contract.ServiceException
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/assignment-groups")
class AssignmentGroupController(
    private val assignmentGroupRepository: AssignmentGroupRepository,
    private val assignmentGroupMapper: AssignmentGroupMapper,
) {
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllAssignmentGroups(): ResponseEntity<List<AssignmentGroupDto>> {
        val assignmentGroups = assignmentGroupRepository.findAll()
        val assignmentGroupDto = assignmentGroups.map(assignmentGroupMapper::toDto)
        return ResponseEntity.ok(assignmentGroupDto)
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAssignmentGroupById(
        @PathVariable id: Long,
    ): ResponseEntity<AssignmentGroupDto> {
        val assignmentGroup =
            assignmentGroupRepository
                .findById(id)
                .orElseThrow { ServiceException(HttpStatus.NOT_FOUND, "Assignment group with ID $id not found") }
        return ResponseEntity.ok(assignmentGroupMapper.toDto(assignmentGroup))
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createAssignmentGroup(
        @RequestBody assignmentGroupDto: AssignmentGroupDto,
    ): ResponseEntity<AssignmentGroupDto> {
        val assignmentGroup = assignmentGroupMapper.toEntity(assignmentGroupDto)
        val savedAssignmentGroup = assignmentGroupRepository.save(assignmentGroup)
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentGroupMapper.toDto(savedAssignmentGroup))
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun updateAssignmentGroup(
        @PathVariable id: Long,
        @RequestBody assignmentGroupDto: AssignmentGroupDto,
    ): ResponseEntity<AssignmentGroupDto> {
        if (!assignmentGroupRepository.existsById(id)) {
            throw ServiceException(HttpStatus.NOT_FOUND, "Assignment group with ID $id not found")
        }

        val assignmentGroup = assignmentGroupMapper.toEntity(assignmentGroupDto)
        assignmentGroup.id = id

        val updatedAssignmentGroup = assignmentGroupRepository.save(assignmentGroup)
        return ResponseEntity.ok(assignmentGroupMapper.toDto(updatedAssignmentGroup))
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{id}")
    fun deleteAssignmentGroup(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        if (!assignmentGroupRepository.existsById(id)) {
            throw ServiceException(HttpStatus.NOT_FOUND, "Assignment group with ID $id not found")
        }

        assignmentGroupRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
