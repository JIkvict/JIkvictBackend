package org.jikvict.jikvictbackend.model.dto

import org.jikvict.testing.model.TestSuiteResult
import java.time.LocalDateTime

/**
 * Admin/Teacher view of assignment result with identifiers for management.
 */
data class AssignmentResultAdminDto(
    val id: Long,
    val timeStamp: LocalDateTime,
    val points: Int,
    val result: TestSuiteResult?,
    val logs: String?,
)
