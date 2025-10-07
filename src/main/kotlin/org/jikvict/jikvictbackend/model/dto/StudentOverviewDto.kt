package org.jikvict.jikvictbackend.model.dto

/**
 * Overview payload for teacher to see student's activity.
 */
data class StudentOverviewDto(
    val userId: Long,
    val userName: String,
    val submissions: List<SubmissionDto>,
    val results: List<AssignmentResultAdminDto>,
)
