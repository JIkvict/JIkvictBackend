package org.jikvict.jikvictbackend.model.dto

import java.time.LocalDateTime

/**
 * Admin/Teacher view of assignment result with identifiers for management.
 */
data class AssignmentResultAdminDto(
    val id: Long,
    val assignmentId: Long,
    val timeStamp: LocalDateTime,
    val points: Int,
)
