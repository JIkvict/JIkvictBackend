package org.jikvict.problem.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "problem.details")
data class ProblemDetailProperties(
    val includeStackTrace: Boolean = false,
    val includeExceptionClassName: Boolean = true,
    val includeTimestamp: Boolean = true,
    val defaultMessages: Map<String, String> = mapOf(
        "IllegalArgumentException" to "Invalid argument provided",
        "IllegalStateException" to "Invalid state encountered",
        "ZipValidationException" to "Zip validation failed",
        "MethodArgumentNotValidException" to "Validation failed for method argument",
        "NoHandlerFoundException" to "No handler found for the requested URL",
        "HttpRequestMethodNotSupportedException" to "Method not supported for this request",
        "HttpMediaTypeNotSupportedException" to "Media type not supported",
        "MaxUploadSizeExceededException" to "Maximum upload size exceeded",
        "Exception" to "An unexpected error occurred",
    ),
)
