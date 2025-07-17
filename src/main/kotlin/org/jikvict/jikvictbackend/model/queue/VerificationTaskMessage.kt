package org.jikvict.jikvictbackend.model.queue

/**
 * Message model for solution verification tasks in the queue
 */
data class VerificationTaskMessage(
    override val taskId: Long,
    override val taskType: String = "SOLUTION_VERIFICATION",
    val filePath: String,
    val originalFilename: String,
    val timeoutSeconds: Long = 300,
    override val additionalParams: Map<String, Any> = emptyMap(),
) : TaskMessage
