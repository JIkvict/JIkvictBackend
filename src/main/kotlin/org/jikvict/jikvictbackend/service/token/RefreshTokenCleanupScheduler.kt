package org.jikvict.jikvictbackend.service.token

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class RefreshTokenCleanupScheduler(
    private val refreshTokenRepository: org.jikvict.jikvictbackend.repository.RefreshTokenRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    @Scheduled(fixedDelayString = $$"${jikvict.tokens.refresh-cleanup-interval-ms:3600000}")
    fun cleanupExpiredTokens() {
        val now = Instant.now()
        val deleted = refreshTokenRepository.deleteByExpiryDateBefore(now)
        if (deleted > 0) {
            log.info("Deleted {} expired refresh tokens", deleted)
        }
    }
}
