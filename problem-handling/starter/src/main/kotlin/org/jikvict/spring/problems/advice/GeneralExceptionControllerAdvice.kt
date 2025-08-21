package org.jikvict.spring.problems.advice

import io.swagger.v3.oas.annotations.Hidden
import org.apache.logging.log4j.LogManager
import org.jikvict.spring.problems.registry.ProcessorsRegistry
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@Hidden
@RestControllerAdvice
class GeneralExceptionControllerAdvice(
    private val registry: ProcessorsRegistry
) {
    private val logger = LogManager.getLogger(this::class.java)

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ProblemDetail {
        logger.error("An exception occurred", exception)
        logger.error("Caused by", exception.cause)
        return registry.getProcessor(exception::class.java).convertToDetail(exception)
    }
}
