package org.jikvict.jikvictbackend.repository

import org.jikvict.jikvictbackend.entity.LongLivingToken
import org.springframework.data.jpa.repository.JpaRepository

interface LongLivingTokenRepository : JpaRepository<LongLivingToken, Long> {
    fun findByUserId(userId: Long): LongLivingToken?

    fun deleteByUserId(userId: Long): Long
}
