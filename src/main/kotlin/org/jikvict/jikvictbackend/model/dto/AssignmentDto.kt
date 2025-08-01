package org.jikvict.jikvictbackend.model.dto

import java.time.LocalDateTime

/**
 * DTO for [org.jikvict.jikvictbackend.entity.Assignment]
 */
data class AssignmentDto(
    val title: String,
    val description: String?,
    val taskId: Int,
    val maxPoints: Int,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val assignmentGroupIds: List<Long> = emptyList(),
    val timeOutSeconds: Long,
)
