package org.jikvict.jikvictbackend.model.queue

/**
 * Message model for assignment tasks in the queue
 */
data class AssignmentTaskMessage(
    override val taskId: Long,
    override val taskType: String = "ASSIGNMENT_CREATION",
    val assignmentNumber: Int,
    override val additionalParams: Map<String, Any> = emptyMap(),
) : TaskMessage
