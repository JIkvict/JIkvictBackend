package org.jikvict.spring.problems.configuration

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@AutoConfiguration
@Import(
    ExceptionProcessorsConfiguration::class,
    RegistryConfiguration::class,
    AdviceConfiguration::class,
)
class JikvictProblemsAutoconfiguration
