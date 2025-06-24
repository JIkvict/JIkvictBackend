package org.jikvict.problem.model

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.net.URI

/**
 * Extension functions for Spring's ProblemDetail class to maintain compatibility
 * with the previous custom implementation.
 */

/**
 * Adds an additional property to the problem detail.
 *
 * @param name The name of the property
 * @param value The value of the property
 * @return This ProblemDetail instance for method chaining
 */
fun ProblemDetail.withProperty(name: String, value: Any?): ProblemDetail {
    this.setProperty(name, value)
    return this
}

/**
 * Returns all additional properties.
 * This is for compatibility with the previous custom implementation.
 */
fun ProblemDetail.getAdditionalProperties(): Map<String, Any?> {
    return this.properties?.toMap() ?: emptyMap()
}

/**
 * Creates a ProblemDetail for a custom type with the given HTTP status.
 *
 * @param type The problem type URI
 * @param title The problem title
 * @param status The HTTP status
 * @param detail Optional detail message
 * @param instance Optional instance URI
 * @return A new ProblemDetail instance
 */
fun forType(
    type: URI,
    title: String,
    status: HttpStatus,
    detail: String? = null,
    instance: URI? = null
): ProblemDetail {
    val problemDetail = ProblemDetail.forStatus(status)
    problemDetail.type = type
    problemDetail.title = title
    if (detail != null) {
        problemDetail.detail = detail
    }
    if (instance != null) {
        problemDetail.instance = instance
    }
    return problemDetail
}
