@file:Suppress("UNCHECKED_CAST")

package org.jikvict.problem.advice

import org.jikvict.problem.factory.ProblemDetailFactory
import org.jikvict.problem.type.ProblemTypeRegistry
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.NoHandlerFoundException

/**
 * Centralized exception handler that converts exceptions to RFC 9457 problem details.
 */
@ControllerAdvice
class ProblemDetailExceptionHandler(
    private val problemDetailFactory: ProblemDetailFactory,
) {
    private val log = LoggerFactory.getLogger(ProblemDetailExceptionHandler::class.java)

    /**
     * Creates a response entity with problem details.
     */
    private fun createProblemResponse(problemDetail: ProblemDetail): ResponseEntity<ProblemDetail> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.valueOf("application/problem+json")
        return ResponseEntity
            .status(problemDetail.status)
            .headers(headers)
            .body(problemDetail)
    }

    /**
     * Handles IllegalArgumentException.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        exception: IllegalArgumentException,
    ): ResponseEntity<ProblemDetail> {
        log.debug("Handling IllegalArgumentException: {}", exception.message)
        val problemDetail = problemDetailFactory.createProblemDetail(
            exception = exception,
            problemType = ProblemTypeRegistry.VALIDATION_ERROR,
        )
        return createProblemResponse(problemDetail)
    }

    /**
     * Handles IllegalStateException.
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(
        exception: IllegalStateException,
    ): ResponseEntity<ProblemDetail> {
        log.debug("Handling IllegalStateException: {}", exception.message)
        val problemDetail = problemDetailFactory.createProblemDetail(
            exception = exception,
            problemType = ProblemTypeRegistry.VALIDATION_ERROR,
        )
        return createProblemResponse(problemDetail)
    }

    /**
     * Handles MethodArgumentNotValidException.
     */
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
    ): ResponseEntity<Any>? {
        log.debug("Handling MethodArgumentNotValidException: {}", ex.message)

        val errors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }

        val problemDetail = problemDetailFactory.createValidationProblemDetail(
            exception = ex,
            errors = errors,
        )

        return createProblemResponse(problemDetail) as ResponseEntity<Any>
    }

    /**
     * Handles NoHandlerFoundException.
     */
    fun handleNoHandlerFoundException(
        ex: NoHandlerFoundException,
    ): ResponseEntity<Any>? {
        log.debug("Handling NoHandlerFoundException: {}", ex.message)

        val problemDetail = problemDetailFactory.createProblemDetail(
            exception = ex,
            problemType = ProblemTypeRegistry.NOT_FOUND,
            detail = "No handler found for ${ex.httpMethod} ${ex.requestURL}",
        )

        return createProblemResponse(problemDetail) as ResponseEntity<Any>
    }

    /**
     * Handles HttpRequestMethodNotSupportedException.
     */
    fun handleHttpRequestMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
    ): ResponseEntity<Any>? {
        log.debug("Handling HttpRequestMethodNotSupportedException: {}", ex.message)

        val problemDetail = problemDetailFactory.createProblemDetail(
            exception = ex,
            problemType = ProblemTypeRegistry.METHOD_NOT_ALLOWED,
            detail = "Method ${ex.method} is not supported for this request. Supported methods are: ${ex.supportedMethods?.joinToString(", ")}",
        )

        return createProblemResponse(problemDetail) as ResponseEntity<Any>
    }

    /**
     * Handles HttpMediaTypeNotSupportedException.
     */
    fun handleHttpMediaTypeNotSupported(
        ex: HttpMediaTypeNotSupportedException,
    ): ResponseEntity<Any>? {
        log.debug("Handling HttpMediaTypeNotSupportedException: {}", ex.message)

        val problemDetail = problemDetailFactory.createProblemDetail(
            exception = ex,
            problemType = ProblemTypeRegistry.UNSUPPORTED_MEDIA_TYPE,
            detail = "Media type ${ex.contentType} is not supported. Supported media types are: ${ex.supportedMediaTypes.joinToString(", ")}",
        )

        return createProblemResponse(problemDetail) as ResponseEntity<Any>
    }

    /**
     * Handles MaxUploadSizeExceededException.
     */
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceededException(
        exception: MaxUploadSizeExceededException,
    ): ResponseEntity<ProblemDetail> {
        log.debug("Handling MaxUploadSizeExceededException: {}", exception.message)

        val problemDetail = problemDetailFactory.createProblemDetail(
            exception = exception,
            problemType = ProblemTypeRegistry.VALIDATION_ERROR,
            detail = "Maximum upload size exceeded",
        )

        return createProblemResponse(problemDetail)
    }

    /**
     * Handles all other exceptions.
     */
    @ExceptionHandler(Exception::class)
    fun handleException(
        exception: Exception,
    ): ResponseEntity<ProblemDetail> {
        log.error("Handling unexpected exception", exception)

        val problemDetail = problemDetailFactory.createInternalServerErrorProblemDetail(
            exception = exception,
        )

        return createProblemResponse(problemDetail)
    }
}
