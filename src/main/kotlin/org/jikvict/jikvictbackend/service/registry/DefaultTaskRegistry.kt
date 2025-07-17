package org.jikvict.jikvictbackend.service.registry

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.model.queue.TaskMessage
import org.jikvict.jikvictbackend.service.processor.TaskProcessor
import org.springframework.stereotype.Service

/**
 * Default implementation of TaskRegistry
 */
@Service
class DefaultTaskRegistry(
    private val log: Logger,
) : TaskRegistry {
    private val processors = mutableMapOf<String, TaskProcessor<*>>()

    override fun <T : TaskMessage> registerProcessor(processor: TaskProcessor<T>) {
        log.info("Registering task processor for task type: ${processor.taskType}")
        processors[processor.taskType] = processor
    }

    override fun getProcessorByTaskType(taskType: String): TaskProcessor<*>? = processors[taskType]

    override fun getAllProcessors(): List<TaskProcessor<*>> = processors.values.toList()
}
