package org.jikvict.spring.problems.configuration

import org.jikvict.problems.processors.DefaultExceptionProcessor
import org.jikvict.problems.processors.HandlerMethodValidationExceptionProcessor
import org.jikvict.problems.processors.IllegalArgumentExceptionProcessor
import org.jikvict.problems.processors.IllegalStateExceptionProcessor
import org.jikvict.problems.processors.MethodArgumentNotValidExceptionProcessor
import org.jikvict.spring.problems.registry.ProcessorsRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ExceptionProcessorsConfiguration(
    private val registry: ProcessorsRegistry,
) {

    @Bean
    fun defaultExceptionProcessor(): DefaultExceptionProcessor {
        val processor = DefaultExceptionProcessor()
        registry.register(processor, Throwable::class.java)
        return processor
    }

    @Bean
    fun handlerMethodValidationExceptionProcessor(): HandlerMethodValidationExceptionProcessor {
        val processor = HandlerMethodValidationExceptionProcessor()
        registry.register(processor, HandlerMethodValidationExceptionProcessor::class.java)
        return processor
    }

    @Bean
    fun illegalStateExceptionProcessor(): IllegalStateExceptionProcessor {
        val processor = IllegalStateExceptionProcessor()
        registry.register(processor, IllegalStateException::class.java)
        return processor
    }

    @Bean
    fun illegalArgumentExceptionProcessor(): IllegalArgumentExceptionProcessor {
        val processor = IllegalArgumentExceptionProcessor()
        registry.register(processor, IllegalArgumentException::class.java)
        return processor
    }

    @Bean
    fun methodArgumentNotValidExceptionProcessor(): MethodArgumentNotValidExceptionProcessor {
        val processor = MethodArgumentNotValidExceptionProcessor()
        registry.register(processor, MethodArgumentNotValidExceptionProcessor::class.java)
        return processor
    }
}
