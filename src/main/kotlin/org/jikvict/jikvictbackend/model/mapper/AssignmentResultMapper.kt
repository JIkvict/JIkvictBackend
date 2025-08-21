package org.jikvict.jikvictbackend.model.mapper

import org.jikvict.jikvictbackend.entity.AssignmentResult
import org.jikvict.jikvictbackend.model.dto.AssignmentResultDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants
import org.mapstruct.ReportingPolicy

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING,
)
interface AssignmentResultMapper {
    @Mapping(source = "testSuiteResult", target = "result")
    fun toDto(assignmentResult: AssignmentResult): AssignmentResultDto
}
