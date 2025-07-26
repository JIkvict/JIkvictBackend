package org.jikvict.jikvictbackend.repository

import org.jikvict.jikvictbackend.entity.AssignmentGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssignmentGroupRepository : JpaRepository<AssignmentGroup, Long>
