package org.jikvict.spring.problems.configuration

import org.jikvict.spring.problems.advice.GeneralExceptionControllerAdvice
import org.jikvict.spring.problems.registry.ProcessorsRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AdviceConfiguration {

    @Bean
    fun generalExceptionControllerAdvice(registry: ProcessorsRegistry) = GeneralExceptionControllerAdvice(registry)
}
