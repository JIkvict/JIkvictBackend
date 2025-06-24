package org.jikvict.jikvictbackend.configuration

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(SolutionsProperties::class)
class JikvictConfiguration
