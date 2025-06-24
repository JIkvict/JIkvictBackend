package org.jikvict.problem.type

import org.springframework.http.HttpStatus
import java.net.URI

/**
 * Represents a problem type as defined in RFC 9457.
 *
 * @property uri The URI reference that identifies the problem type
 * @property title A short, human-readable summary of the problem type
 * @property status The HTTP status code associated with this problem type
 */
data class ProblemType(
    val uri: URI,
    val title: String,
    val status: HttpStatus
)
