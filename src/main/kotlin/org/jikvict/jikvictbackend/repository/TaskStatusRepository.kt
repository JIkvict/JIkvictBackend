package org.jikvict.jikvictbackend.repository

import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TaskStatusRepository : JpaRepository<TaskStatus, Long> {
    fun findByTaskTypeAndResultId(
        taskType: String,
        resultId: Long,
    ): Optional<TaskStatus>

    fun findAllByStatus(status: PendingStatus): List<TaskStatus>

    fun findTaskStatusById(id: Long): TaskStatus?

    fun findTaskStatusByStatus(status: PendingStatus): MutableList<TaskStatus>

    fun findByUser(user: User): List<TaskStatus>

    fun findAllByTaskTypeOrderByCreatedAtDesc(taskType: String): List<TaskStatus>

    fun findAllByUserAndTaskTypeAndStatus(
        user: User,
        taskType: String,
        status: PendingStatus,
    ): List<TaskStatus>
}
