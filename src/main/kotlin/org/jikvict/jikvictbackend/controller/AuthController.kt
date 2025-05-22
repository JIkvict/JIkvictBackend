package org.jikvict.jikvictbackend.controller

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.jikvict.jikvictbackend.entity.RefreshToken
import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.request.LoginRequest
import org.jikvict.jikvictbackend.model.request.RegisterRequest
import org.jikvict.jikvictbackend.model.response.TokenResponse
import org.jikvict.jikvictbackend.repository.UserRepository
import org.jikvict.jikvictbackend.service.JwtService
import org.jikvict.jikvictbackend.service.RefreshTokenService
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authManager: AuthenticationManager,
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val refreshTokenService: RefreshTokenService,
    private val passwordEncoder: PasswordEncoder
) {
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest, response: HttpServletResponse): ResponseEntity<TokenResponse> {
        val auth = UsernamePasswordAuthenticationToken(request.username, request.password)
        authManager.authenticate(auth)

        val user = userRepository.findUserByUserNameField(request.username) ?: throw RuntimeException("Not found")
        val access = jwtService.generateToken(user, 15 * 60 * 1000)
        val refresh = refreshTokenService.createRefreshToken(user.id)
        addRefreshTokenCookie(response, refresh.token)
        return ResponseEntity.ok(TokenResponse(access))
    }

    @PostMapping("/refresh")
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<TokenResponse> {
        val refreshToken = extractRefreshTokenFromCookies(request)
            ?: return ResponseEntity.badRequest().build()

        val verified = refreshTokenService.verifyExpiration(refreshToken)
        val user = verified.user

        val access = jwtService.generateToken(user, 15 * 60 * 1000)
        addRefreshTokenCookie(response, refreshToken.token)
        return ResponseEntity.ok(TokenResponse(access))
    }

    private fun addRefreshTokenCookie(response: HttpServletResponse, refreshToken: String) {
        val cookie = Cookie("refreshToken", refreshToken).apply {
            isHttpOnly = true
            secure = true
            path = "/"
            maxAge = 7 * 24 * 60 * 60
        }
        response.addCookie(cookie)
    }

    private fun extractRefreshTokenFromCookies(request: HttpServletRequest): RefreshToken? {
        val token = request.cookies
            ?.firstOrNull { it.name == "refreshToken" }
            ?.value
            ?: return null
        val refreshToken = refreshTokenService.findByToken(token) ?: return null
        return refreshTokenService.verifyExpiration(refreshToken)
    }

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest, response: HttpServletResponse): ResponseEntity<TokenResponse> {
        val hashedPassword = passwordEncoder.encode(request.password)

        val user = User().apply {
            userPassword = hashedPassword
            userNameField = request.username
        }

        userRepository.save(user)

        val access = jwtService.generateToken(user)
        val refresh = refreshTokenService.createRefreshToken(user.id)
        addRefreshTokenCookie(response, refresh.token)
        return ResponseEntity.ok(TokenResponse(access))
    }
}
