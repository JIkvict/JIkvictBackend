package org.jikvict.jikvictbackend.model.response

data class QueueStatusDto(
    val totalInQueue: Int,
    val userTaskPosition: Int?,
    val userTaskId: Long?,
    val estimatedTimeRemainingSeconds: Long?,
)
