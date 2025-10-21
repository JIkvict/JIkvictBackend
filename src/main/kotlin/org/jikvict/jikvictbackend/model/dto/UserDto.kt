package org.jikvict.jikvictbackend.model.dto

/**
 * DTO for [org.jikvict.jikvictbackend.entity.User]
 */
data class UserDto(
    val id: Long,
    val userNameField: String,
    val email: String,
    val aisId: String,
    val roles: MutableSet<String>,
    val assignmentGroups: MutableSet<AssignmentGroupDto>,
)
