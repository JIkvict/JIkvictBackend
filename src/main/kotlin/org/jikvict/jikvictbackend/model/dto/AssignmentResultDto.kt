package org.jikvict.jikvictbackend.model.dto

import org.jikvict.testing.model.TestResult
import org.jikvict.testing.model.TestSuiteResult
import java.time.LocalDateTime

/**
 * DTO for [org.jikvict.jikvictbackend.entity.AssignmentResult]
 */
data class AssignmentResultDto(
    val id: Long,
    val timeStamp: LocalDateTime,
    val points: Int,
    val result: TestSuiteResult?,
)

fun TestSuiteResult.withHiddenInfo(): TestSuiteResult =
    copy(
        testResults = testResults.map { it.withHiddenInfo() },
    )

fun TestResult.withHiddenInfo(): TestResult =
    copy(
        displayName = "HIDDEN",
        testName = "HIDDEN",
        logs = emptyList(),
    )
