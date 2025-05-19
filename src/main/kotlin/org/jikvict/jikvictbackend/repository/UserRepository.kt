package org.jikvict.jikvictbackend.repository

import org.jikvict.jikvictbackend.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository: JpaRepository<User, Long> {
    fun findByEmail(username: String): User?
    fun existsByEmail(email: String): Boolean
    fun findUserByUserNameField(username: String): User?
    fun findUserById(userId: Long): User?
}