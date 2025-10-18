package org.jikvict.problems.processors

import org.jikvict.problems.processors.contract.ExceptionProcessor
import org.jikvict.problems.util.NameFormatter
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import java.net.URI

class MethodArgumentNotValidExceptionProcessor : ExceptionProcessor<MethodArgumentNotValidException> {
    override fun convertToDetail(exception: MethodArgumentNotValidException): ProblemDetail {
        return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
            title = "Invalid Method Argument"
            detail = buildDetailMessage(exception)
            type = URI("application:error${NameFormatter.format(exception::class.java.name)}")
            setProperty(
                "errors",
                exception.bindingResult.fieldErrors.map { error ->
                    mapOf(
                        "field" to error.field,
                        "message" to (error.defaultMessage ?: "Invalid value"),
                        "rejectedValue" to error.rejectedValue,
                        "code" to error.code,
                    )
                },
            )
        }
    }

    private fun buildDetailMessage(source: MethodArgumentNotValidException): String {
        val allResults = source.bindingResult.allErrors
        val errors = allResults.map { error ->
            val field = if (error is org.springframework.validation.FieldError) error.field else "unknown"
            "$field: ${error.defaultMessage ?: "Invalid value"}"
        }
        return if (errors.isNotEmpty()) {
            "Validation failed for: ${errors.joinToString(", ")}"
        } else {
            source.localizedMessage ?: "Invalid method argument"
        }
    }
}
