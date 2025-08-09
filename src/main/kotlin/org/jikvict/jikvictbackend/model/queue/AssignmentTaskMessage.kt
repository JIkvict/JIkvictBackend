package org.jikvict.jikvictbackend.model.queue

import org.jikvict.jikvictbackend.model.dto.CreateAssignmentDto

/**
 * Message model for assignment tasks in the queue
 */
data class AssignmentTaskMessage(
    override val taskId: Long,
    override val taskType: String = "ASSIGNMENT_CREATION",
    override val additionalParams: CreateAssignmentDto,
) : TaskMessage<CreateAssignmentDto>
