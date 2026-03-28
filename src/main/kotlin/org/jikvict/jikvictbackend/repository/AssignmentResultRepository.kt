package org.jikvict.jikvictbackend.repository

import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.entity.AssignmentResult
import org.jikvict.jikvictbackend.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AssignmentResultRepository : JpaRepository<AssignmentResult, Long> {
    fun findByUserAndAssignment(
        user: User,
        assignment: Assignment,
    ): List<AssignmentResult>

    fun findFirstByUserAndAssignmentOrderByTimeStampDesc(
        user: User,
        assignment: Assignment,
    ): AssignmentResult?

    fun findAllByUser(user: User): List<AssignmentResult>

    @Query(
        """
        SELECT ar FROM assignment_result ar
        WHERE ar.assignment = :assignment
        AND ar.timeStamp = (
            SELECT MAX(ar2.timeStamp) FROM assignment_result ar2
            WHERE ar2.user = ar.user AND ar2.assignment = ar.assignment
        )
        """,
    )
    fun findLatestSubmissionPerUserForAssignment(
        @Param("assignment") assignment: Assignment,
    ): List<AssignmentResult>
}
