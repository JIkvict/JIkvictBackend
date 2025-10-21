package org.jikvict.jikvictbackend.service.assignment

import org.jikvict.jikvictbackend.model.dto.AssignmentGroupDto
import org.jikvict.jikvictbackend.model.mapper.AssignmentGroupMapper
import org.jikvict.jikvictbackend.repository.AssignmentGroupRepository
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AssignmentGroupService(
    private val assignmentGroupRepository: AssignmentGroupRepository,
    private val assignmentGroupMapper: AssignmentGroupMapper,
) {
    @Transactional
    fun getAll(): List<AssignmentGroupDto> = assignmentGroupRepository.findAll().map(assignmentGroupMapper::toDto)

    @Transactional
    fun getById(id: Long): AssignmentGroupDto {
        val assignmentGroup =
            assignmentGroupRepository
                .findById(id)
                .orElseThrow { ServiceException(HttpStatus.NOT_FOUND, "Assignment group with ID $id not found") }
        return assignmentGroupMapper.toDto(assignmentGroup)
    }

    @Transactional
    fun create(assignmentGroupDto: AssignmentGroupDto): AssignmentGroupDto {
        val entity = assignmentGroupMapper.toEntity(assignmentGroupDto)
        val saved = assignmentGroupRepository.save(entity)
        return assignmentGroupMapper.toDto(saved)
    }

    @Transactional
    fun update(
        id: Long,
        assignmentGroupDto: AssignmentGroupDto,
    ): AssignmentGroupDto {
        if (!assignmentGroupRepository.existsById(id)) {
            throw ServiceException(HttpStatus.NOT_FOUND, "Assignment group with ID $id not found")
        }
        val entity = assignmentGroupMapper.toEntity(assignmentGroupDto)
        entity.id = id
        val updated = assignmentGroupRepository.save(entity)
        return assignmentGroupMapper.toDto(updated)
    }

    fun delete(id: Long) {
        if (!assignmentGroupRepository.existsById(id)) {
            throw ServiceException(HttpStatus.NOT_FOUND, "Assignment group with ID $id not found")
        }
        assignmentGroupRepository.deleteById(id)
    }
}
