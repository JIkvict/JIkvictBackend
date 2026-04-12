package org.jikvict.jikvictbackend.model.response

import org.jikvict.jikvictbackend.model.request.PlagiarismCheckParameters
import java.time.LocalDateTime

data class PlagiarismCheckSummaryResponse(
    val taskId: Long,
    val assignmentId: Long,
    val startedAt: LocalDateTime,
    val initiatedBy: String,
    val status: PendingStatus,
    val parameters: PlagiarismCheckParameters?,
)
