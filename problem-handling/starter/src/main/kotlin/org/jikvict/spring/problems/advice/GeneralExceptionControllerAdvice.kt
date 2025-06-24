package org.jikvict.spring.problems.advice

import org.jikvict.spring.problems.registry.ProcessorsRegistry
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GeneralExceptionControllerAdvice(
    private val registry: ProcessorsRegistry
) {
    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ProblemDetail {
        return registry.getProcessor(exception::class.java).convertToDetail(exception)
    }
}
