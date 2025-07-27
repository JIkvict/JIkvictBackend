package org.jikvict.jikvictbackend.model.queue

import org.jikvict.jikvictbackend.model.dto.VerificationTaskDto

/**
 * Message model for solution verification tasks in the queue
 */
data class VerificationTaskMessage(
    override val taskId: Long,
    override val taskType: String = "SOLUTION_VERIFICATION",
    override val additionalParams: VerificationTaskDto,
) : TaskMessage<VerificationTaskDto>
