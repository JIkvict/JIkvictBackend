package org.jikvict.jikvictbackend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.jikvict.jikvictbackend.entity.AssignmentResult
import org.jikvict.jikvictbackend.model.domain.AssignmentInfo
import org.jikvict.jikvictbackend.model.dto.AssignmentResultAdminDto
import org.jikvict.jikvictbackend.model.dto.StatsRequestDto
import org.jikvict.jikvictbackend.model.dto.StudentOverviewDto
import org.jikvict.jikvictbackend.model.dto.SubmissionDto
import org.jikvict.jikvictbackend.repository.AssignmentResultRepository
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.repository.UserRepository
import org.jikvict.jikvictbackend.service.assignment.AssignmentInfoUserService
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TeacherStudentController(
    private val userRepository: UserRepository,
    private val taskStatusRepository: TaskStatusRepository,
    private val assignmentResultRepository: AssignmentResultRepository,
    private val objectMapper: ObjectMapper,
    private val assignmentInfoUserService: AssignmentInfoUserService,
) {
    data class UpdatePointsRequest(
        val points: Int,
    )

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/teacher/assignment-info/{assignmentId}")
    fun getAssignmentInfo(@PathVariable assignmentId: Long, @RequestBody request: StatsRequestDto): ResponseEntity<List<AssignmentInfo>> {
        return ResponseEntity.ok(assignmentInfoUserService.getAssignmentInfoByUserGroupsAndUsers(assignmentId, request.userIds, request.groupIds))
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/teacher/students/{userId}/overview")
    fun getStudentOverview(
        @PathVariable userId: Long,
    ): ResponseEntity<StudentOverviewDto> {
        val user =
            userRepository.findById(userId).orElseThrow {
                ServiceException(HttpStatus.NOT_FOUND, "User with ID $userId not found")
            }

        val submissions =
            taskStatusRepository
                .findByUser(user)
                .filter {
                    it.taskType == "SOLUTION_VERIFICATION"
                }.map { ts ->
                    SubmissionDto(
                        id = ts.id,
                        taskType = ts.taskType,
                        status = ts.status,
                        message = ts.message,
                        createdAt = ts.createdAt,
                        completedAt = ts.completedAt,
                        assignmentId = extractAssignmentId(ts.parameters),
                    )
                }

        val results = assignmentResultRepository.findAllByUser(user).map { it.toAdminDto() }

        val payload =
            StudentOverviewDto(
                userId = user.id,
                userName = user.userNameField,
                submissions = submissions,
                results = results,
            )
        return ResponseEntity.ok(payload)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/submissions/{submissionId}")
    fun deleteSubmission(
        @PathVariable submissionId: Long,
    ): ResponseEntity<Unit> {
        val exists = taskStatusRepository.existsById(submissionId)
        if (!exists) {
            throw ServiceException(HttpStatus.NOT_FOUND, "Submission with ID $submissionId not found")
        }
        taskStatusRepository.deleteById(submissionId)
        return ResponseEntity.noContent().build()
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/results/{resultId}/points")
    fun updateResultPoints(
        @PathVariable resultId: Long,
        @RequestBody request: UpdatePointsRequest,
    ): ResponseEntity<AssignmentResultAdminDto> {
        if (request.points < 0) {
            throw ServiceException(HttpStatus.BAD_REQUEST, "Points must be non-negative")
        }
        val result =
            assignmentResultRepository.findById(resultId).orElseThrow {
                ServiceException(HttpStatus.NOT_FOUND, "Result with ID $resultId not found")
            }
        result.points = request.points
        val saved = assignmentResultRepository.save(result)
        return ResponseEntity.ok(saved.toAdminDto())
    }

    private fun extractAssignmentId(parametersJson: String?): Long? =
        try {
            val node = objectMapper.readTree(parametersJson)
            node?.get("assignmentId")?.asLong()
        } catch (ex: Exception) {
            null
        }

    private fun AssignmentResult.toAdminDto(): AssignmentResultAdminDto =
        AssignmentResultAdminDto(
            id = this.id,
            assignmentId = this.assignment.id,
            timeStamp = this.timeStamp,
            points = this.points,
        )
}
