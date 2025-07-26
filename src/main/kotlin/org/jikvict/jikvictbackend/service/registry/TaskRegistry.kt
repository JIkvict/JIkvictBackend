package org.jikvict.jikvictbackend.service.registry

import org.jikvict.jikvictbackend.model.queue.TaskMessage
import org.jikvict.jikvictbackend.service.processor.TaskProcessor

/**
 * Interface for registering and retrieving task processors
 */
interface TaskRegistry {
    /**
     * Register a task processor
     * @param processor The task processor to register
     */
    fun <T, M : TaskMessage<T>> registerProcessor(processor: TaskProcessor<T, M>)

    /**
     * Get a task processor by task type
     * @param taskType The type of task
     * @return The task processor for the specified task type, or null if not found
     */
    fun getProcessorByTaskType(taskType: String): TaskProcessor<*, *>?

    /**
     * Get all registered task processors
     * @return A list of all registered task processors
     */
    fun getAllProcessors(): List<TaskProcessor<*, *>>
}
