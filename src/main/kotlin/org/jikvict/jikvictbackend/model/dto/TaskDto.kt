package org.jikvict.jikvictbackend.model.dto

/**
 * DTO for [org.jikvict.jikvictbackend.entity.Task]
 */
data class TaskDto(
    val id: Long,
    val githubTaskId: Int? = null,
)
