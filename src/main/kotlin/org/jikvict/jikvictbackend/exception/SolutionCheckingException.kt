package org.jikvict.jikvictbackend.exception

import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpStatus

class SolutionCheckingException(
    val logs: String?,
    message: String,
    cause: Throwable? = null,
    status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
) : ServiceException(status, message, cause)
