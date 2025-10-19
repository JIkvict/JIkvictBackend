package org.jikvict.jikvictbackend.model.mapper

import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.entity.AssignmentGroup
import org.jikvict.jikvictbackend.entity.isClosed
import org.jikvict.jikvictbackend.model.dto.AssignmentDto
import org.jikvict.jikvictbackend.model.dto.CreateAssignmentDto
import org.jikvict.jikvictbackend.repository.AssignmentGroupRepository
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants
import org.mapstruct.ReportingPolicy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING,
)
abstract class AssignmentMapper {
    @Autowired
    protected lateinit var assignmentGroupRepository: AssignmentGroupRepository

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignmentGroups", ignore = true)
    abstract fun toEntity(assignmentDto: AssignmentDto): Assignment

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignmentGroups", expression = "java(getAssignmentGroups(createAssignmentDto))")
    @Transactional
    abstract fun toEntity(createAssignmentDto: CreateAssignmentDto): Assignment

    protected fun getAssignmentGroups(createAssignmentDto: CreateAssignmentDto): Set<AssignmentGroup> =
        createAssignmentDto.assignmentGroupsIds
            .mapNotNull {
                assignmentGroupRepository.findAssignmentGroupById(it)
            }.toSet()

    @Mapping(target = "isClosed", expression = "java(mapIsClosed(assignment))")
    abstract fun toDto(assignment: Assignment): AssignmentDto

    fun mapIsClosed(assignment: Assignment): Boolean = assignment.isClosed
}
