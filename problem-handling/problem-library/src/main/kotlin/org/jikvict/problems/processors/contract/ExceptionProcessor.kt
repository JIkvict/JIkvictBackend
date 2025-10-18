package org.jikvict.problems.processors.contract

import org.springframework.http.ProblemDetail

interface ExceptionProcessor<in T: Throwable> {
    fun convertToDetail(exception: T): ProblemDetail
}
