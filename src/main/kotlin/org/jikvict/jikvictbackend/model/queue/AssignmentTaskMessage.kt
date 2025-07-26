package org.jikvict.jikvictbackend.model.queue

import org.jikvict.jikvictbackend.model.dto.AssignmentDto

/**
 * Message model for assignment tasks in the queue
 */
data class AssignmentTaskMessage(
    override val taskId: Long,
    override val taskType: String = "ASSIGNMENT_CREATION",
    val assignmentNumber: Int,
    override val additionalParams: AssignmentDto,
) : TaskMessage<AssignmentDto>
