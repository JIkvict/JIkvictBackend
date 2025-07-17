package org.jikvict.jikvictbackend.model.queue

import java.io.Serializable

/**
 * Interface for all task messages in the queue
 */
interface TaskMessage : Serializable {
    /**
     * The ID of the task
     */
    val taskId: Long

    /**
     * The type of the task
     */
    val taskType: String

    /**
     * Additional parameters for the task
     */
    val additionalParams: Map<String, Any>
}
