package org.jikvict.problem.factory

import org.jikvict.problem.config.ProblemDetailProperties
import org.jikvict.problem.model.forType
import org.jikvict.problem.model.withProperty
import org.jikvict.problem.type.ProblemType
import org.jikvict.problem.type.ProblemTypeRegistry
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Component
import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.io.PrintWriter
import java.io.StringWriter

@Component
class ProblemDetailFactory(
    private val properties: ProblemDetailProperties
) {

    /**
     * Creates a ProblemDetail from an exception using a predefined problem type.
     *
     * @param exception The exception to convert
     * @param problemType The problem type to use
     * @param detail Optional detail message (defaults to exception message)
     * @param instance Optional instance URI
     * @return A ProblemDetail instance
     */
    fun createProblemDetail(
        exception: Throwable,
        problemType: ProblemType,
        detail: String? = getDefaultMessage(exception),
        instance: URI? = null,
    ): ProblemDetail {
        val problemDetail = forType(
            type = problemType.uri,
            title = problemType.title,
            status = problemType.status,
            detail = detail,
            instance = instance
        )

        // Add common additional properties based on configuration
        if (properties.includeTimestamp) {
            problemDetail.withProperty("timestamp", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        }

        if (properties.includeExceptionClassName) {
            problemDetail.withProperty("exception", exception.javaClass.name)
        }

        if (properties.includeStackTrace) {
            val sw = StringWriter()
            exception.printStackTrace(PrintWriter(sw))
            problemDetail.withProperty("stackTrace", sw.toString())
        }


        return problemDetail
    }

    /**
     * Creates a ProblemDetail for a validation error.
     *
     * @param exception The exception to convert
     * @param errors Map of field names to error messages
     */
    fun createValidationProblemDetail(
        exception: Throwable,
        errors: Map<String, String>,
    ): ProblemDetail {
        val problemDetail = createProblemDetail(
            exception = exception,
            problemType = ProblemTypeRegistry.VALIDATION_ERROR
        )

        // Add validation errors
        problemDetail.withProperty("errors", errors)

        return problemDetail
    }

    /**
     * Creates a ProblemDetail for a resource not found error.
     *
     * @param exception The exception to convert
     * @param resourceType The type of resource that was not found
     * @param resourceId The ID of the resource that was not found
     * @return A ProblemDetail instance
     */
    fun createNotFoundProblemDetail(
        exception: Throwable,
        resourceType: String,
        resourceId: String,
    ): ProblemDetail {
        val problemDetail = createProblemDetail(
            exception = exception,
            problemType = ProblemTypeRegistry.NOT_FOUND,
            detail = "The $resourceType with ID '$resourceId' was not found"
        )

        // Add resource information
        problemDetail.withProperty("resourceType", resourceType)
        problemDetail.withProperty("resourceId", resourceId)

        return problemDetail
    }

    /**
     * Creates a ProblemDetail for an internal server error.
     *
     * @param exception The exception to convert
     * @return A ProblemDetail instance
     */
    fun createInternalServerErrorProblemDetail(
        exception: Throwable,
    ): ProblemDetail {
        return createProblemDetail(
            exception = exception,
            problemType = ProblemTypeRegistry.INTERNAL_SERVER_ERROR
        )
    }

    /**
     * Gets the default message for an exception from the properties or falls back to the exception message.
     *
     * @param exception The exception
     * @return The default message
     */
    private fun getDefaultMessage(exception: Throwable): String? {
        val className = exception.javaClass.simpleName
        return exception.message ?: properties.defaultMessages[className] ?: properties.defaultMessages["Exception"]
    }

    /**
     * Creates a ProblemDetail for a custom problem type.
     *
     * @param exception The exception to convert
     * @param type The problem type URI
     * @param title The problem title
     * @param status The HTTP status
     * @param detail Optional detail message
     * @param instance Optional instance URI
     * @return A ProblemDetail instance
     */
    fun createCustomProblemDetail(
        exception: Throwable,
        type: URI,
        title: String,
        status: HttpStatus,
        detail: String? = exception.message,
        instance: URI? = null,
    ): ProblemDetail {
        val customType = ProblemType(type, title, status)
        return createProblemDetail(
            exception = exception,
            problemType = customType,
            detail = detail,
            instance = instance
        )
    }
}
