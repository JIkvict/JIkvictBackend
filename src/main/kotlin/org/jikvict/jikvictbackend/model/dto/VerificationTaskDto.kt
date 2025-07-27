package org.jikvict.jikvictbackend.model.dto

/**
 * DTO for verification tasks
 */
data class VerificationTaskDto(
    val timeoutSeconds: Long,
    val assignmentNumber: Int,
    val solutionBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VerificationTaskDto

        if (timeoutSeconds != other.timeoutSeconds) return false
        if (assignmentNumber != other.assignmentNumber) return false
        if (!solutionBytes.contentEquals(other.solutionBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timeoutSeconds.hashCode()
        result = 31 * result + assignmentNumber
        result = 31 * result + solutionBytes.contentHashCode()
        return result
    }
}
