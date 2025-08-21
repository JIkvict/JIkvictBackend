package org.jikvict.jikvictbackend.repository

import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.entity.AssignmentResult
import org.jikvict.jikvictbackend.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface AssignmentResultRepository : JpaRepository<AssignmentResult, Long> {
    fun findByUserAndAssignment(
        user: User,
        assignment: Assignment,
    ): List<AssignmentResult>
}
