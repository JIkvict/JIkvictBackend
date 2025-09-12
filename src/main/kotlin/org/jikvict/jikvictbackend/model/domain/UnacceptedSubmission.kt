package org.jikvict.jikvictbackend.model.domain

import java.time.LocalDateTime

data class UnacceptedSubmission(
    val time: LocalDateTime,
    val message: String,
    val assignmentId: Long,
)
