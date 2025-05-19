package org.jikvict.jikvictbackend.repository

import org.jikvict.jikvictbackend.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository: JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?
    fun existsByToken(token: String): Boolean
    fun deleteByToken(token: String)
    fun deleteByUserId(userId: Long)
}