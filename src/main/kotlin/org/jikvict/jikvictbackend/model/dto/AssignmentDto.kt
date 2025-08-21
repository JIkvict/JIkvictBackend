package org.jikvict.jikvictbackend.model.dto

import org.jikvict.jikvictbackend.entity.AssignmentGroup
import org.jikvict.jikvictbackend.service.AssignmentService
import java.time.LocalDateTime

/**
 * DTO for [org.jikvict.jikvictbackend.entity.Assignment]
 */
data class AssignmentDto(
    val id: Long,
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
    val isClosed: Boolean = false,
)

context(service: AssignmentService) val AssignmentDto.assignmentGroups: List<AssignmentGroup>
    get() = service.getAssignmentById(this.id).assignmentGroups.toList()

data class CreateAssignmentDto(
    val title: String,
    val taskId: Int,
    val maxPoints: Int,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val timeOutSeconds: Long,
    val memoryLimit: Long,
    val cpuLimit: Long,
    val pidsLimit: Long,
    val assignmentGroupsIds: List<Long>,
)
