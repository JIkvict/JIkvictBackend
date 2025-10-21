package org.jikvict.jikvictbackend.model.mapper

import org.jikvict.jikvictbackend.entity.AssignmentGroup
import org.jikvict.jikvictbackend.model.dto.AssignmentGroupDto
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.repository.UserRepository
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
abstract class AssignmentGroupMapper {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var assignmentRepository: AssignmentRepository

    @Mapping(target = "users", ignore = true)
    abstract fun toEntity(assignmentGroupDto: AssignmentGroupDto): AssignmentGroup

    @AfterMapping
    fun mapUsers(
        assignmentGroupDto: AssignmentGroupDto,
        @MappingTarget assignmentGroup: AssignmentGroup,
    ) {
        // Map Users
        val users = userRepository.findAllById(assignmentGroupDto.userIds)
        assignmentGroup.users.clear()
        assignmentGroup.users.addAll(users)
    }

    @Mapping(target = "userIds", expression = "java(mapUsersToIds(assignmentGroup))")
    @Mapping(target = "assignmentIds", expression = "java(mapAssignmentsToIds(assignmentGroup))")
    abstract fun toDto(assignmentGroup: AssignmentGroup): AssignmentGroupDto

    protected fun mapUsersToIds(assignmentGroup: AssignmentGroup): List<Long> = assignmentGroup.users.map { it.id }
    protected fun mapAssignmentsToIds(assignmentGroup: AssignmentGroup): List<Long> {
        return assignmentRepository.findAllByAssignmentGroups(setOf(assignmentGroup)).map { it.id }
    }
}
