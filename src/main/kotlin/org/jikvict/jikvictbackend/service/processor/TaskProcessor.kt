package org.jikvict.jikvictbackend.service.processor

import org.jikvict.jikvictbackend.model.queue.TaskMessage

/**
 * Interface for processing different types of tasks
 */
interface TaskProcessor<T : TaskMessage> {
    /**
     * The type of task this processor can handle
     */
    val taskType: String

    /**
     * The queue name for this processor
     */
    val queueName: String

    /**
     * The exchange name for this processor
     */
    val exchangeName: String

    /**
     * The routing key for this processor
     */
    val routingKey: String

    /**
     * Process a task message
     * @param message The task message to process
     */
    fun process(message: T)
}
