package org.jikvict.problem.type

import org.springframework.http.HttpStatus
import java.net.URI

/**
 * Registry of standard problem types as defined in RFC 9457.
 * This class provides a centralized place to define common error types.
 */
object ProblemTypeRegistry {
    /**
     * Creates a URI for a problem type with the given exception class name.
     *
     * @param exceptionClassName The name of the exception class
     * @return A URI for the problem type
     */
    fun uri(exceptionClassName: String): URI = URI.create(exceptionClassName)

    /**
     * Problem type for validation errors.
     */
    val VALIDATION_ERROR = ProblemType(
        uri = uri("javax.validation.ValidationException"),
        title = "Validation Error",
        status = HttpStatus.BAD_REQUEST
    )

    /**
     * Problem type for resource not found errors.
     */
    val NOT_FOUND = ProblemType(
        uri = uri("java.util.NoSuchElementException"),
        title = "Resource Not Found",
        status = HttpStatus.NOT_FOUND
    )

    /**
     * Problem type for authentication errors.
     */
    val AUTHENTICATION_ERROR = ProblemType(
        uri = uri("org.springframework.security.core.AuthenticationException"),
        title = "Authentication Error",
        status = HttpStatus.UNAUTHORIZED
    )

    /**
     * Problem type for authorization errors.
     */
    val AUTHORIZATION_ERROR = ProblemType(
        uri = uri("org.springframework.security.access.AccessDeniedException"),
        title = "Authorization Error",
        status = HttpStatus.FORBIDDEN
    )

    /**
     * Problem type for method not allowed errors.
     */
    val METHOD_NOT_ALLOWED = ProblemType(
        uri = uri("org.springframework.web.HttpRequestMethodNotSupportedException"),
        title = "Method Not Allowed",
        status = HttpStatus.METHOD_NOT_ALLOWED
    )

    /**
     * Problem type for media type not supported errors.
     */
    val UNSUPPORTED_MEDIA_TYPE = ProblemType(
        uri = uri("org.springframework.web.HttpMediaTypeNotSupportedException"),
        title = "Unsupported Media Type",
        status = HttpStatus.UNSUPPORTED_MEDIA_TYPE
    )

    /**
     * Problem type for internal server errors.
     */
    val INTERNAL_SERVER_ERROR = ProblemType(
        uri = uri("java.lang.Exception"),
        title = "Internal Server Error",
        status = HttpStatus.INTERNAL_SERVER_ERROR
    )

    /**
     * Problem type for service unavailable errors.
     */
    val SERVICE_UNAVAILABLE = ProblemType(
        uri = uri("org.springframework.web.client.ResourceAccessException"),
        title = "Service Unavailable",
        status = HttpStatus.SERVICE_UNAVAILABLE
    )

    /**
     * Problem type for bad gateway errors.
     */
    val BAD_GATEWAY = ProblemType(
        uri = uri("org.springframework.web.client.RestClientException"),
        title = "Bad Gateway",
        status = HttpStatus.BAD_GATEWAY
    )

    /**
     * Problem type for gateway timeout errors.
     */
    val GATEWAY_TIMEOUT = ProblemType(
        uri = uri("java.util.concurrent.TimeoutException"),
        title = "Gateway Timeout",
        status = HttpStatus.GATEWAY_TIMEOUT
    )
}
