package org.jikvict.problems.processors

import org.jikvict.problems.processors.contract.ExceptionProcessor
import org.jikvict.problems.util.NameFormatter
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.method.annotation.HandlerMethodValidationException
import java.net.URI

class HandlerMethodValidationExceptionProcessor : ExceptionProcessor<HandlerMethodValidationException> {

    override fun convertToDetail(exception: HandlerMethodValidationException): ProblemDetail {
        return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
            title = "Validation Error"
            detail = buildDetailMessage(exception)
            type = URI("application:error${NameFormatter.format(exception::class.java.name)}")
            setProperty("errors", extractValidationErrors(exception))
        }
    }

    private fun buildDetailMessage(source: HandlerMethodValidationException): String {
        val allResults = source.parameterValidationResults.toList()
        val errors = allResults.flatMap { result ->
            result.resolvableErrors.map { error ->
                val location = result.methodParameter.parameterName ?: "response"
                "$location: ${error.defaultMessage ?: error.codes}"
            }
        }

        return if (errors.isNotEmpty()) {
            "Validation failed for: ${errors.joinToString(", ")}"
        } else {
            source.localizedMessage
        }
    }

    private fun extractValidationErrors(source: HandlerMethodValidationException): List<Map<String, Any?>> {
        val allResults = source.parameterValidationResults.toList()
        return allResults.flatMap { result ->
            result.resolvableErrors.map { error ->
                val isReturnValue = source.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
                if (isReturnValue) {
                    mapOf(
                        "type" to "returnValue",
                        "message" to (error.defaultMessage ?: error.codes),
                        "rejectedValue" to result.argument,
                        "code" to error.codes,
                    )
                } else {
                    mapOf(
                        "type" to "parameter",
                        "field" to result.methodParameter.parameterName,
                        "message" to (error.defaultMessage ?: error.codes),
                        "rejectedValue" to result.argument,
                        "code" to error.codes,
                    )
                }
            }
        }
    }
}
