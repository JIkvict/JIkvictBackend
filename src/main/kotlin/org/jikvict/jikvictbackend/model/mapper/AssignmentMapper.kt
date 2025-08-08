package org.jikvict.jikvictbackend.model.mapper

import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.model.dto.AssignmentDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants
import org.mapstruct.ReportingPolicy

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING,
)
abstract class AssignmentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignmentGroups", ignore = true)
    abstract fun toEntity(assignmentDto: AssignmentDto): Assignment

    abstract fun toDto(assignment: Assignment): AssignmentDto
}
