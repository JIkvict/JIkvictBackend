package org.jikvict.jikvictbackend.model.dto

/**
 * DTO for verification tasks
 */
data class VerificationTaskDto(
    val assignmentId: Int,
    val userId: Long,
    val solutionBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VerificationTaskDto

        if (assignmentId != other.assignmentId) return false
        if (userId != other.userId) return false
        if (!solutionBytes.contentEquals(other.solutionBytes)) return false

        return true
    }

    override fun toString(): String = "VerificationTaskDto(assignmentId=$assignmentId, userId=$userId, solutionBytes.size=${solutionBytes.size})"

    override fun hashCode(): Int {
        var result = assignmentId
        result = 31 * result + userId.hashCode()
        result = 31 * result + solutionBytes.contentHashCode()
        return result
    }
}
