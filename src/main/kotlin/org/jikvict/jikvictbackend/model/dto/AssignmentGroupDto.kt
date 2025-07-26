package org.jikvict.jikvictbackend.model.dto

/**
 * DTO for [org.jikvict.jikvictbackend.entity.AssignmentGroup]
 */
data class AssignmentGroupDto(
    val id: Long? = null,
    val name: String,
    val userIds: List<Long> = emptyList(),
    val assignmentIds: List<Long> = emptyList(),
)
