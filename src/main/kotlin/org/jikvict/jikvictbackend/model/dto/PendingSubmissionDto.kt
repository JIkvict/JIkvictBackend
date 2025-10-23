package org.jikvict.jikvictbackend.model.dto

import java.time.LocalDateTime

data class PendingSubmissionDto(
    val assignmentId: Long,
    val createdAt: LocalDateTime,
)
