package org.jikvict.spring.problems.configuration

import org.jikvict.spring.problems.registry.ProcessorsRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RegistryConfiguration {
    @Bean
    fun processorsRegistry(): ProcessorsRegistry {
        return ProcessorsRegistry()
    }
}
