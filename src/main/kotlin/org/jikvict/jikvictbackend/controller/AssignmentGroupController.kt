package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.entity.AssignmentGroup
import org.jikvict.jikvictbackend.model.dto.AssignmentGroupDto
import org.jikvict.jikvictbackend.model.mapper.AssignmentGroupMapper
import org.jikvict.jikvictbackend.repository.AssignmentGroupRepository
import org.jikvict.problems.exception.contract.ServiceException
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
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
@RequestMapping("/api/assignment-groups")
class AssignmentGroupController(
    private val assignmentGroupRepository: AssignmentGroupRepository,
    private val assignmentGroupMapper: AssignmentGroupMapper,
) {
    /**
     * Get all assignment groups with pagination
     */
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllAssignmentGroups(
        @ParameterObject pageable: Pageable,
    ): PagedModel<AssignmentGroupDto> {
        val assignmentGroups: Page<AssignmentGroup> = assignmentGroupRepository.findAll(pageable)
        val assignmentGroupDtoPage: Page<AssignmentGroupDto> = assignmentGroups.map(assignmentGroupMapper::toDto)
        return PagedModel(assignmentGroupDtoPage)
    }

    /**
     * Get an assignment group by ID
     */
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

    /**
     * Create a new assignment group
     */
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createAssignmentGroup(
        @RequestBody assignmentGroupDto: AssignmentGroupDto,
    ): ResponseEntity<AssignmentGroupDto> {
        val assignmentGroup = assignmentGroupMapper.toEntity(assignmentGroupDto)
        val savedAssignmentGroup = assignmentGroupRepository.save(assignmentGroup)
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentGroupMapper.toDto(savedAssignmentGroup))
    }

    /**
     * Update an existing assignment group
     */
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

    /**
     * Delete an assignment group
     */
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
