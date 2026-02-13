package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.annotation.OnlyTeacher
import org.jikvict.jikvictbackend.model.dto.AssignmentGroupDto
import org.jikvict.jikvictbackend.service.assignment.AssignmentGroupService
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
    private val assignmentGroupService: AssignmentGroupService,
) {
    @OnlyTeacher
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllAssignmentGroups(): ResponseEntity<List<AssignmentGroupDto>> {
        val assignmentGroupDto = assignmentGroupService.getAll()
        return ResponseEntity.ok(assignmentGroupDto)
    }

    @OnlyTeacher
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAssignmentGroupById(
        @PathVariable id: Long,
    ): ResponseEntity<AssignmentGroupDto> {
        val dto = assignmentGroupService.getById(id)
        return ResponseEntity.ok(dto)
    }

    @OnlyTeacher
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createAssignmentGroup(
        @RequestBody assignmentGroupDto: AssignmentGroupDto,
    ): ResponseEntity<AssignmentGroupDto> {
        val created = assignmentGroupService.create(assignmentGroupDto)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @OnlyTeacher
    @PutMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun updateAssignmentGroup(
        @PathVariable id: Long,
        @RequestBody assignmentGroupDto: AssignmentGroupDto,
    ): ResponseEntity<AssignmentGroupDto> {
        val updated = assignmentGroupService.update(id, assignmentGroupDto)
        return ResponseEntity.ok(updated)
    }

    @OnlyTeacher
    @DeleteMapping("/{id}")
    fun deleteAssignmentGroup(
        @PathVariable id: Long,
    ): ResponseEntity<Unit> {
        assignmentGroupService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
