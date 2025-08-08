package org.jikvict.jikvictbackend.service.processor

import org.jikvict.jikvictbackend.model.queue.TaskMessage

/**
 * Interface for processing different types of tasks
 * @param T The type of additional parameters in the task message
 * @param M The type of task message that extends TaskMessage<T>
 */
interface TaskProcessor<T, M : TaskMessage<T>> {
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
}
