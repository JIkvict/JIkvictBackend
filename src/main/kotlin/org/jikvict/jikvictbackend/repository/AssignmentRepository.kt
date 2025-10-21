package org.jikvict.jikvictbackend.repository

import org.jikvict.jikvictbackend.entity.Assignment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AssignmentRepository : JpaRepository<Assignment, Long> {
    interface AssignmentProps {
        val taskId: Int
        val id: Long
    }

    @Query("select a.id as id, a.taskId as taskId from Assignment a where a.id = :id")
    fun findPropsById(
        @Param("id") id: Long,
    ): AssignmentProps?

    @Query("select a from Assignment a join a.assignmentGroups g where g.id in :groupIds")
    fun findAllByGroupIds(
        @Param("groupIds") groupIds: Collection<Long>,
    ): List<Assignment>
}
