package org.jikvict.jikvictbackend.model.request

data class LoginRequest(
    val email: String,
    val password: String,
)
