package org.jikvict.problems.processors

import org.jikvict.problems.processors.contract.ExceptionProcessor
import org.jikvict.problems.util.NameFormatter
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.net.URI

class IllegalStateExceptionProcessor: ExceptionProcessor<IllegalStateException> {
    override fun convertToDetail(exception: IllegalStateException): ProblemDetail {
        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).apply {
            title = "Application encountered an illegal state"
            detail = exception.localizedMessage
            type = URI("application:error${NameFormatter.format(exception::class.java.name)}")
        }
    }
}
