package org.jikvict.jikvictbackend.model.request

import org.jikvict.jikvictbackend.entity.RefreshToken

data class RefreshRequest(
    val refreshToken: RefreshToken,
)
