package org.jikvict.jikvictbackend.service.assignment

import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class AssignmentCacheService(
    private val assignmentRepository: AssignmentRepository,
) {
    @Cacheable("assignmentTimeouts")
    fun getAssignmentTimeouts(assignmentIds: List<Long>): List<AssignmentRepository.AssignmentTimeoutProps> {
        if (assignmentIds.isEmpty()) {
            return emptyList()
        }
        return assignmentRepository.findTimeoutsByIds(assignmentIds)
    }
}
