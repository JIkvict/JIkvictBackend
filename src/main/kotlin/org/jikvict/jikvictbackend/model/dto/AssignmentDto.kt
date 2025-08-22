package org.jikvict.jikvictbackend.model.dto

import java.time.LocalDateTime

/**
 * DTO for [org.jikvict.jikvictbackend.entity.Assignment]
 */
data class AssignmentDto(
    val id: Long,
    val title: String,
    val description: String?,
    val taskId: Int,
    val maxPoints: Int,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val timeOutSeconds: Long,
    val memoryLimit: Long,
    val cpuLimit: Long,
    val pidsLimit: Long,
    val isClosed: Boolean,
    val maximumAttempts: Int,
)


data class CreateAssignmentDto(
    val title: String,
    val taskId: Int,
    val maxPoints: Int,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val timeOutSeconds: Long,
    val memoryLimit: Long,
    val cpuLimit: Long,
    val pidsLimit: Long,
    val assignmentGroupsIds: List<Long>,
)
