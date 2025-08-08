package org.jikvict.jikvictbackend.repository

import org.jikvict.jikvictbackend.entity.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findUserByUserNameField(username: String): User?

    @EntityGraph(attributePaths = ["assignmentGroups"])
    fun findUserById(userId: Long): User?
}
