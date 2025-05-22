package org.jikvict.jikvictbackend.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.jikvict.jikvictbackend.entity.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(user: User, expirationMillis: Long = 15 * 60 * 1000): String {
        return Jwts.builder()
            .setSubject(user.userNameField)
            .claim("roles", user.roles.map { it.name })
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expirationMillis))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun extractUsername(token: String): String =
        Jwts.parserBuilder().setSigningKey(key).build()
            .parseClaimsJws(token).body.subject

    fun isTokenValid(token: String, user: User): Boolean {
        val username = extractUsername(token)
        return username == user.userNameField && !isTokenExpired(token)
    }

    private fun isTokenExpired(token: String): Boolean {
        val expiration = Jwts.parserBuilder().setSigningKey(key).build()
            .parseClaimsJws(token).body.expiration
        return expiration.before(Date())
    }
}
