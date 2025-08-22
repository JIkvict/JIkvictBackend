package org.jikvict.jikvictbackend.service.assignment

import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.dto.withHiddenInfo
import org.jikvict.jikvictbackend.model.mapper.AssignmentResultMapper
import org.jikvict.jikvictbackend.model.response.AssignmentInfo
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager

@Service
class AssignmentInfoUserService(
    private val assignmentResultMapper: AssignmentResultMapper,
    private val entityManager: EntityManager,
    private val assignmentResultService: AssignmentResultService,
    private val assignmentService: AssignmentService,
    private val assignmentRepository: AssignmentRepository,
) {
    @Transactional
    fun getAssignmentInfoForUser(
        assignmentId: Long,
        user: User,
    ): AssignmentInfo {
        val assignment = getAssignmentByIdForUser(assignmentId, user)
        val attemptsUsed = assignmentResultService.getUsedAttempts(assignmentId, user)
        val results = assignmentResultService.getResults(assignmentId, user)
        results.forEach { entityManager.detach(it) }

        val mappedResults =
            if (assignment.isClosed == false) {
                results.map {
                    assignmentResultMapper.toDto(it).copy(
                        result = it.testSuiteResult?.withHiddenInfo(),
                    )
                }
            } else {
                results.map {
                    assignmentResultMapper.toDto(it)
                }
            }

        val assignmentInfo =
            AssignmentInfo(
                assignmentId = assignment.id,
                taskId = assignment.taskId,
                maxAttempts = assignment.maximumAttempts,
                attemptsUsed = attemptsUsed,
                results = mappedResults,
            )
        return assignmentInfo
    }

    @Transactional
    fun getAllAssignmentsForUser(user: User): List<Assignment> {
        val assignments = assignmentRepository.findAllByAssignmentGroups(user.assignmentGroups)
        return assignments
    }

    @Transactional
    fun getAssignmentByIdForUser(
        id: Long,
        user: User,
    ): Assignment {
        val assignment = assignmentService.getAssignmentById(id)
        require(assignment.assignmentGroups.map { it.id }.any { it in user.assignmentGroups.map { it.id } }) {
            throw ServiceException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this assignment, user: ${user.id} does not have: ${assignment.assignmentGroups.map { it.id }}",
            )
        }
        return assignment
    }

    @Transactional
    fun getAssignmentZipForUser(
        assignmentId: Long,
        user: User,
    ): ByteArray {
        val assignment = getAssignmentByIdForUser(assignmentId, user)
        return assignmentService.getZip(assignment.id)
    }
}
