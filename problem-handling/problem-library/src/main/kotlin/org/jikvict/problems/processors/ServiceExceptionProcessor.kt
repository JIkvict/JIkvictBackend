package org.jikvict.problems.processors

import org.jikvict.problems.exception.contract.ServiceException
import org.jikvict.problems.processors.contract.ExceptionProcessor
import org.jikvict.problems.util.NameFormatter
import org.springframework.http.ProblemDetail
import java.net.URI

class ServiceExceptionProcessor : ExceptionProcessor<ServiceException> {
    override fun convertToDetail(exception: ServiceException): ProblemDetail {
        val status = exception.status
        return ProblemDetail.forStatus(status).apply {
            title = exception::class.java.simpleName
            detail = exception.message
            type = URI("application:error${NameFormatter.format(exception::class.java.name)}")
        }
    }
}
