package org.jikvict.jikvictbackend.model.dto

import org.jikvict.jikvictbackend.model.response.PendingStatus
import java.time.LocalDateTime

/**
 * Simplified DTO for TaskStatus representing a user's submission state.
 */
data class SubmissionDto(
    val id: Long,
    val taskType: String,
    val status: PendingStatus,
    val message: String?,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val assignmentId: Long?,
)
