package org.jikvict.spring.problems.registry

import org.jikvict.problems.processors.contract.ExceptionProcessor

class ProcessorsRegistry {
    private val processors = mutableMapOf<Class<*>, ExceptionProcessor<*>>()

    fun register(exceptionProcessor: ExceptionProcessor<*>, exceptionClass: Class<*>) {
        processors[exceptionClass] = exceptionProcessor
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Throwable> getProcessor(exceptionClass: Class<T>): ExceptionProcessor<Throwable> {
        val matching = processors.keys.firstOrNull {
            it.isAssignableFrom(exceptionClass) && it != Throwable::class.java
        }
        if (matching != null) {
            return processors[matching] as ExceptionProcessor<Throwable>
        }
        return processors[Throwable::class.java]!! as ExceptionProcessor<Throwable>
    }
}
