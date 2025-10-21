package org.jikvict.jikvictbackend.service.assignment

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.model.dto.CreateAssignmentDto
import org.jikvict.jikvictbackend.model.mapper.AssignmentMapper
import org.jikvict.jikvictbackend.repository.AssignmentGroupRepository
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.jikvictbackend.service.GitService
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.nio.file.Path

@Service
class AssignmentService(
    private val log: Logger,
    private val assignmentRepository: AssignmentRepository,
    private val assignmentMapper: AssignmentMapper,
    private val gitService: GitService,
    private val assignmentGroupRepository: AssignmentGroupRepository,
) {
    fun getAssignmentsForGroup(groupId: Long): List<Assignment> {
        val group =
            assignmentGroupRepository.findById(groupId).orElseThrow {
                ServiceException(HttpStatus.NOT_FOUND, "Assignment group not found")
            }
        return assignmentRepository.findAllByGroupIds(setOf(group.id))
    }

    fun getAssignmentsForGroups(groupIds: Set<Long>): List<Assignment> = assignmentRepository.findAllByGroupIds(groupIds)

    fun createAssignment(assignmentDto: CreateAssignmentDto): Assignment {
        val description =
            runCatching {
                getAssignmentDescription(assignmentDto.taskId)
            }.onFailure {
                log.error("Could not create assignment: ${it.message}")
                throw ServiceException(
                    HttpStatus.NOT_FOUND,
                    "Could not create assignment: probably there is no task${assignmentDto.taskId} in the repository",
                )
            }.getOrNull()!!

        val assignment =
            assignmentMapper.toEntity(assignmentDto).apply {
                this.description = description
            }

        val savedAssignment = assignmentRepository.save(assignment)
        return savedAssignment
    }

    @Transactional
    internal fun getZip(assignmentId: Long): ByteArray {
        val assignment = getAssignmentById(assignmentId)
        val taskId = assignment.taskId
        return cloneZipBytes(listOf("task$taskId/.*".toRegex()))
    }

    @Transactional
    internal fun getAssignmentById(id: Long): Assignment =
        assignmentRepository.findById(id).orElseThrow {
            ServiceException(
                HttpStatus.NOT_FOUND,
                "Assignment with ID $id not found",
            )
        }

    internal fun getHiddenFilesForTask(task: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        gitService.streamZipToOutput(
            outputStream,
            pathFilters = listOf("^task$task/.*/hidden/.*".toRegex()),
            excludePatterns =
                listOf(
                    "\\.git/.*".toRegex(),
                    "\\.idea/.*".toRegex(),
                ),
        )
        return outputStream.toByteArray()
    }

    private fun cloneZipBytes(include: List<Regex>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        gitService.streamZipToOutput(
            outputStream,
            pathFilters = include,
            excludePatterns =
                listOf(
                    ".*/hidden(/.*)?".toRegex(),
                    "\\.git/.*".toRegex(),
                    "\\.idea/.*".toRegex(),
                ),
        )
        return outputStream.toByteArray()
    }

    fun getAssignmentDescription(task: Int): String = gitService.getFileContentFromAssignmentRepo(Path.of("DESCRIPTION.md"), task).toString(Charsets.UTF_8)
}
