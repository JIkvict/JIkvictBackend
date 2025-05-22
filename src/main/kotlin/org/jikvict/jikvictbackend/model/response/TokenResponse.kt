package org.jikvict.jikvictbackend.model.response

data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
)
