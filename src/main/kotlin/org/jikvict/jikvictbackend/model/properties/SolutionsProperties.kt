package org.jikvict.jikvictbackend.model.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("solutions")
data class SolutionsProperties(
    val maxFileSize: String = "",
    val allowedFileTypes: List<String> = listOf(),
    val maxExecutionTime: Long = 0L,
    // ZIP validation properties
    val maxEntrySize: Long = 10_000_000L,
    val suspiciousExtensions: List<String> = listOf(".exe", ".dll", ".cmd", ".sh", ".js", ".bin"),
    val maxCompressionRatio: Double = 10.0,
    val maxUnknownCompressionEntrySize: Long = 1000L,
)
