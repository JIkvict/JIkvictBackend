package org.jikvict.problem.autoconfigure

import org.jikvict.problem.config.ProblemDetailConfiguration
import org.jikvict.problem.config.ProblemDetailProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(ProblemDetailProperties::class)
@Import(ProblemDetailConfiguration::class)
@ComponentScan(basePackages = ["org.jikvict.problem"])
class ProblemDetailAutoConfiguration
