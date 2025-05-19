package org.jikvict.jikvictbackend.service

import org.jikvict.jikvictbackend.entity.RefreshToken
import org.jikvict.jikvictbackend.repository.RefreshTokenRepository
import org.jikvict.jikvictbackend.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository
) {

    @Value("\${jwt.refreshExpirationMs}")
    private var refreshTokenDurationMs: Long = 86400000

    fun createRefreshToken(userId: Long): RefreshToken {
        val token = UUID.randomUUID().toString()
        val expiryDate = Instant.now().plusMillis(refreshTokenDurationMs)
        val user = userRepository.findUserById(userId)
        requireNotNull(user) { "User is not present in the database" }

        val refreshToken = RefreshToken(
            token = token,
            user = user,
            expiryDate = expiryDate
        )

        return refreshTokenRepository.save(refreshToken)
    }

    fun findByToken(token: String): RefreshToken? {
        return refreshTokenRepository.findByToken(token)
    }

    fun verifyExpiration(token: RefreshToken): RefreshToken {
        if (token.expiryDate.isBefore(Instant.now())) {
            refreshTokenRepository.delete(token)
            throw RuntimeException("Refresh token was expired. Please make a new signin request")
        }
        return token
    }

    @Transactional
    fun deleteByUserId(userId: Long) {
        refreshTokenRepository.deleteByUserId(userId)
    }
}
