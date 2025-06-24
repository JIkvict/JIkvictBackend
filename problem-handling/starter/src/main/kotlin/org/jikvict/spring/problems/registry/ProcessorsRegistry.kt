package org.jikvict.spring.problems.registry

import org.jikvict.problems.processors.contract.ExceptionProcessor

class ProcessorsRegistry {
    private val processors = mutableMapOf<Class<*>, ExceptionProcessor<*>>()

    fun register(exceptionProcessor: ExceptionProcessor<*>, exceptionClass: Class<*>) {
        processors[exceptionClass] = exceptionProcessor
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Throwable> getProcessor(exceptionClass: Class<T>): ExceptionProcessor<Throwable> {
        val processor = processors[exceptionClass]
        if (processor == null) {
            return processors[Throwable::class.java]!! as ExceptionProcessor<Throwable>
        }
        return processor as ExceptionProcessor<Throwable>
    }
}
