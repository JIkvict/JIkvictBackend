package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.request.LoginRequest
import org.jikvict.jikvictbackend.model.request.RefreshRequest
import org.jikvict.jikvictbackend.model.request.RegisterRequest
import org.jikvict.jikvictbackend.model.response.TokenResponse
import org.jikvict.jikvictbackend.repository.UserRepository
import org.jikvict.jikvictbackend.service.JwtService
import org.jikvict.jikvictbackend.service.RefreshTokenService
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
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
    private val refreshTokenService: RefreshTokenService
) {
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        val auth = UsernamePasswordAuthenticationToken(request.email, request.password)
        authManager.authenticate(auth)

        val user = userRepository.findByEmail(request.email) ?: throw RuntimeException("Not found")
        val access = jwtService.generateToken(user, 15 * 60 * 1000)
        val refresh = refreshTokenService.createRefreshToken(user.id)

        return ResponseEntity.ok(TokenResponse(access, refresh))
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody req: RefreshRequest): ResponseEntity<TokenResponse> {
        val refreshToken = refreshTokenService.verifyExpiration(req.refreshToken)
        val user = refreshToken.user

        val access = jwtService.generateToken(user)
        return ResponseEntity.ok(TokenResponse(access, req.refreshToken))
    }

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<TokenResponse> {
        val user = User().apply {
            email = request.email
            userPassword = request.password
            userNameField = request.username
        }
        userRepository.save(user)
        val access = jwtService.generateToken(user, 15 * 60 * 1000)
        val refresh = refreshTokenService.createRefreshToken(user.id)
        return ResponseEntity.ok(TokenResponse(access, refresh))
    }
}
