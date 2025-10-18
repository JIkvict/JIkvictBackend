package org.jikvict.jikvictbackend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.jikvict.jikvictbackend.entity.RefreshToken
import org.jikvict.jikvictbackend.model.request.LoginRequest
import org.jikvict.jikvictbackend.model.response.TokenResponse
import org.jikvict.jikvictbackend.repository.UserRepository
import org.jikvict.jikvictbackend.service.token.JwtService
import org.jikvict.jikvictbackend.service.token.RefreshTokenService
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authManager: AuthenticationManager,
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val refreshTokenService: RefreshTokenService,
) {
    @Operation(
        summary = "Log in",
        description = "Logs in a user and returns an access token and a refresh token.",
        method = "POST",
        operationId = "login",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successful login",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema =
                            Schema(
                                implementation = TokenResponse::class,
                            ),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request, invalid credentials",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema =
                            Schema(
                                implementation = ProblemDetail::class,
                            ),
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        response: HttpServletResponse,
    ): ResponseEntity<TokenResponse> {
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
        val refreshToken =
            extractRefreshTokenFromCookies(request)
                ?: return ResponseEntity.badRequest().build()

        val verified = refreshTokenService.verifyExpiration(refreshToken)
        val user = verified.user

        val access = jwtService.generateToken(user, 15 * 60 * 1000)
        addRefreshTokenCookie(response, refreshToken.token)
        return ResponseEntity.ok(TokenResponse(access))
    }

    private fun addRefreshTokenCookie(
        response: HttpServletResponse,
        refreshToken: String,
    ) {
        val cookie =
            Cookie("refreshToken", refreshToken).apply {
                isHttpOnly = false
                path = "/"
                maxAge = 7 * 24 * 60 * 60
            }
        response.addCookie(cookie)
    }

    private fun extractRefreshTokenFromCookies(request: HttpServletRequest): RefreshToken? {
        val token =
            request.cookies
                ?.firstOrNull { it.name == "refreshToken" }
                ?.value
                ?: return null
        val refreshToken = refreshTokenService.findByToken(token) ?: return null
        return refreshTokenService.verifyExpiration(refreshToken)
    }

}
