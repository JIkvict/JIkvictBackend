package org.jikvict.jikvictbackend.repository

import org.jikvict.jikvictbackend.entity.Assignment
import org.springframework.data.jpa.repository.JpaRepository

interface AssignmentRepository : JpaRepository<Assignment, Long> {
}
