package org.jikvict.jikvictbackend.service.token

import org.jikvict.jikvictbackend.entity.LongLivingToken
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.properties.JwtProperties
import org.jikvict.jikvictbackend.repository.LongLivingTokenRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class LongLivingTokenService(
    private val longLivingTokenRepository: LongLivingTokenRepository,
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
) {
    fun getOrCreate(user: User): LongLivingToken {
        val existing = longLivingTokenRepository.findByUserId(user.id)
        if (existing != null && existing.expiryDate.isAfter(Instant.now())) {
            return existing
        }

        existing?.let { longLivingTokenRepository.delete(it) }

        val token = jwtService.generateToken(user, jwtProperties.longLivingExpirationSeconds * 1000)
        val entity =
            LongLivingToken(
                token = token,
                user = user,
                expiryDate = Instant.now().plusSeconds(jwtProperties.longLivingExpirationSeconds),
            )
        return longLivingTokenRepository.save(entity)
    }

    fun deleteForUser(userId: Long): Boolean =
        longLivingTokenRepository
            .findByUserId(userId)
            ?.let {
                longLivingTokenRepository.delete(it)
                true
            }
            ?: false
}
