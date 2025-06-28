package org.jikvict.jikvictbackend.model.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("jikvict.assignment")
data class AssignmentProperties(
    val repositoryUrl: String,
    val githubToken: String,
    val githubUsername: String,
)
