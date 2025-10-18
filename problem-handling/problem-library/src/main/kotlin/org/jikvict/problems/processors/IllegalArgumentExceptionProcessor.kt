package org.jikvict.problems.processors

import org.jikvict.problems.processors.contract.ExceptionProcessor
import org.jikvict.problems.util.NameFormatter
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.net.URI

class IllegalArgumentExceptionProcessor: ExceptionProcessor<IllegalArgumentException> {
    override fun convertToDetail(exception: IllegalArgumentException): ProblemDetail {
        return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
            title = "Invalid Argument"
            detail = exception.localizedMessage
            type = URI("application:error${NameFormatter.format(exception::class.java.name)}")
        }
    }
}
