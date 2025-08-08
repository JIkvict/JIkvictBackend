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
    val timeOutSeconds: Long,
    val memoryLimit: Long,
    val cpuLimit: Long,
    val pidsLimit: Long,
)
