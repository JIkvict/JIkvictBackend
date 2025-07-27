package org.jikvict.jikvictbackend.model.mapper

import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.model.dto.AssignmentDto
import org.jikvict.jikvictbackend.repository.AssignmentGroupRepository
import org.mapstruct.AfterMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants
import org.mapstruct.MappingTarget
import org.mapstruct.ReportingPolicy
import org.springframework.beans.factory.annotation.Autowired

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING,
)
abstract class AssignmentMapper {
    @Autowired
    private lateinit var assignmentGroupRepository: AssignmentGroupRepository

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignmentGroups", ignore = true)
    abstract fun toEntity(assignmentDto: AssignmentDto): Assignment

    @AfterMapping
    fun mapTaskAndGroups(
        assignmentDto: AssignmentDto,
        @MappingTarget assignment: Assignment,
    ) {
        val groups = assignmentGroupRepository.findAllById(assignmentDto.assignmentGroupIds)
        assignment.assignmentGroups.clear()
        assignment.assignmentGroups.addAll(groups)
    }

    @Mapping(target = "assignmentGroupIds", expression = "java(mapAssignmentGroupsToIds(assignment))")
    abstract fun toDto(assignment: Assignment): AssignmentDto

    protected fun mapAssignmentGroupsToIds(assignment: Assignment): List<Long> = assignment.assignmentGroups.map { it.id }
}
