package org.jikvict.jikvictbackend.model.mapper

import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.entity.AssignmentGroup
import org.jikvict.jikvictbackend.entity.isClosed
import org.jikvict.jikvictbackend.model.dto.AssignmentDto
import org.jikvict.jikvictbackend.model.dto.CreateAssignmentDto
import org.jikvict.jikvictbackend.repository.AssignmentGroupRepository
import org.jikvict.jikvictbackend.service.GitService
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants
import org.mapstruct.ReportingPolicy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Path

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING,
)
abstract class AssignmentMapper {
    @Autowired
    protected lateinit var assignmentGroupRepository: AssignmentGroupRepository

    @Autowired
    protected lateinit var gitService: GitService

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignmentGroups", expression = "java(mapAssignmentGroups(assignmentDto))")
    @Mapping(target = "description", expression = "java(mapDescription(assignmentDto))")
    @Transactional
    abstract fun toEntity(assignmentDto: AssignmentDto): Assignment

    fun mapAssignmentGroups(assignmentDto: AssignmentDto): Set<AssignmentGroup> =
        assignmentDto.assignmentGroupsIds
            .mapNotNull {
                assignmentGroupRepository.findAssignmentGroupById(it)
            }.toSet()

    fun mapDescription(assignmentDto: AssignmentDto): String =
        assignmentDto.description ?: gitService.getFileContentFromAssignmentRepo(Path.of("DESCRIPTION.md"), assignmentDto.taskId).toString(Charsets.UTF_8)

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
    @Mapping(target = "assignmentGroupsIds", expression = "java(getAssignmentGroupIds(assignment))")
    abstract fun toDto(assignment: Assignment): AssignmentDto

    fun mapIsClosed(assignment: Assignment): Boolean = assignment.isClosed

    protected fun getAssignmentGroupIds(assignment: Assignment): List<Long> = assignment.assignmentGroups.map { it.id }
}
