package org.jikvict.jikvictbackend.repository

import org.jikvict.jikvictbackend.entity.Task
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskRepository : JpaRepository<Task, Long>
