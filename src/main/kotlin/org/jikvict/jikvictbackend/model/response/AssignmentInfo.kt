package org.jikvict.jikvictbackend.model.response

import org.jikvict.jikvictbackend.model.dto.AssignmentResultDto

data class AssignmentInfo(
    val assignmentId: Long,
    val taskId: Int,
    val maxAttempts: Int,
    val attemptsUsed: Int,
    val results: List<AssignmentResultDto> = emptyList(),
)
