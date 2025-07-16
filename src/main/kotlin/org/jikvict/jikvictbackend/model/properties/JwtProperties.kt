package org.jikvict.jikvictbackend.model.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("jikvict.jwt")
data class JwtProperties(
    val secret: String,
    val refreshExpirationMs: Long
)
