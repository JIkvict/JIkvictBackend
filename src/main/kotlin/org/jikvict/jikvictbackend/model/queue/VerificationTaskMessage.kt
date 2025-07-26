package org.jikvict.jikvictbackend.model.queue

import org.jikvict.jikvictbackend.model.dto.VerificationTaskDto

/**
 * Message model for solution verification tasks in the queue
 */
data class VerificationTaskMessage(
    override val taskId: Long,
    override val taskType: String = "SOLUTION_VERIFICATION",
    val filePath: String,
    val originalFilename: String,
    val timeoutSeconds: Long = 300,
    val assignmentNumber: Int,
    override val additionalParams: VerificationTaskDto,
) : TaskMessage<VerificationTaskDto>
