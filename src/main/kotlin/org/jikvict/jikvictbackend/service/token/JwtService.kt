package org.jikvict.jikvictbackend.service.token

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.properties.JwtProperties
import org.springframework.stereotype.Service
import java.util.Date

@Service
class JwtService(
    jwtProperties: JwtProperties,
) {
    private val key = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())

    fun generateToken(
        user: User,
        expirationMillis: Long = 15 * 60 * 1000,
    ): String =
        Jwts
            .builder()
            .subject(user.userNameField)
            .claim("roles", user.roles.map { it.name })
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMillis))
            .signWith(key, Jwts.SIG.HS256)
            .compact()

    fun extractUsername(token: String): String =
        Jwts
            .parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload.subject

    fun isTokenValid(
        token: String,
        user: User,
    ): Boolean {
        val username = extractUsername(token)
        return username == user.userNameField && !isTokenExpired(token)
    }

    private fun isTokenExpired(token: String): Boolean {
        val expiration =
            Jwts
                .parser()
                .decryptWith(key)
                .build()
                .parseSignedClaims(token)
                .payload.expiration
        return expiration.before(Date())
    }
}
