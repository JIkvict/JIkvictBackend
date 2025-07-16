package org.jikvict.jikvictbackend.configuration

import org.jikvict.jikvictbackend.model.properties.AssignmentProperties
import org.jikvict.jikvictbackend.model.properties.JwtProperties
import org.jikvict.jikvictbackend.model.properties.SolutionsProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(SolutionsProperties::class, AssignmentProperties::class, JwtProperties::class)
class JikvictConfiguration
