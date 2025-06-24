package org.jikvict.problem.config

import org.jikvict.problem.advice.ProblemDetailExceptionHandler
import org.jikvict.problem.factory.ProblemDetailFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

@Configuration
@EnableConfigurationProperties(ProblemDetailProperties::class)
class ProblemDetailConfiguration(
    private val properties: ProblemDetailProperties
) {

    @PostConstruct
    fun init() {
        println("ProblemDetailConfiguration initialized with properties: $properties")
    }

    @Bean
    @ConditionalOnMissingBean
    fun problemDetailFactory(
        properties: ProblemDetailProperties
    ): ProblemDetailFactory {
        return ProblemDetailFactory(properties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun problemDetailExceptionHandler(problemDetailFactory: ProblemDetailFactory): ProblemDetailExceptionHandler {
        return ProblemDetailExceptionHandler(problemDetailFactory)
    }
}
