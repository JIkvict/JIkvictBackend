package org.jikvict.jikvictbackend.service.task

import com.fasterxml.jackson.databind.ObjectMapper
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskStatusService(
    private val taskStatusRepository: TaskStatusRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun getUnsuccessfulSubmissionsForUser(
        user: User,
        assignmentId: Long,
    ): List<TaskStatus> {
        val submissions = taskStatusRepository.findAllByUserAndTaskTypeAndStatus(user, "SOLUTION_VERIFICATION", PendingStatus.FAILED)
        val cancelled = taskStatusRepository.findAllByUserAndTaskTypeAndStatus(user, "SOLUTION_VERIFICATION", PendingStatus.CANCELLED)
        val all = submissions + cancelled
        val filteredByAssignment =
            all.filter { submission ->
                runCatching {
                    val node = objectMapper.readTree(submission.parameters)
                    node?.get("assignmentId")?.asLong() == assignmentId
                }.getOrDefault(false)
            }
        return filteredByAssignment
    }
}
