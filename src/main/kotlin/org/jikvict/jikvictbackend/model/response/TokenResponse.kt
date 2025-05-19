package org.jikvict.jikvictbackend.model.response

import org.jikvict.jikvictbackend.entity.RefreshToken

data class TokenResponse(
    val accessToken: String,
    val refreshToken: RefreshToken,
    val tokenType: String = "Bearer",
)
