package org.jikvict.problems.exception.contract

import org.springframework.http.HttpStatus

open class ServiceException(
    val status: HttpStatus,
    override val message: String,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)
