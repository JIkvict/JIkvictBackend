package org.jikvict.jikvictbackend.model.dto

data class StatsRequestDto(
    val userIds: List<Long>,
    val groupIds: List<Long>,
)
