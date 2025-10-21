package org.jikvict.jikvictbackend.model.mapper

import org.jikvict.jikvictbackend.entity.Role
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.dto.UserDto
import org.jikvict.jikvictbackend.repository.RoleRepository
import org.mapstruct.Mapper
import org.mapstruct.MappingConstants
import org.mapstruct.ReportingPolicy
import org.springframework.beans.factory.annotation.Autowired

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING,
    uses = [AssignmentGroupMapper::class],
)
abstract class UserMapper {

    @Autowired
    lateinit var roleRepository: RoleRepository

    abstract fun toEntity(userDto: UserDto): User
    abstract fun toUserDto(user: User): UserDto

    fun mapRolesToString(roles: Set<Role>): Set<String> {
        return roles.map { it.name }.toSet()
    }

    fun mapRolesFromString(roles: Set<String>): Set<Role> {
        return roles.mapNotNull { roleRepository.findByName(it) }.toSet()
    }
}
