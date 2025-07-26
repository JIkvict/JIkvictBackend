package org.jikvict.jikvictbackend.model.dto

/**
 * DTO for verification tasks
 */
data class VerificationTaskDto(
    val filePath: String,
    val originalFilename: String,
    val timeoutSeconds: Long,
    val assignmentNumber: Int,
)
