package org.jikvict.jikvictbackend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.jikvict.jikvictbackend.model.response.TokenResponse
import org.jikvict.jikvictbackend.service.UserDetailsServiceImpl
import org.jikvict.jikvictbackend.service.token.LongLivingTokenService
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth/long-token")
class LongLivingTokenController(
    private val longLivingTokenService: LongLivingTokenService,
    private val userDetailsService: UserDetailsServiceImpl,
) {
    @Operation(
        summary = "Create or get long-living token",
        description = "Creates a new long-living token for the authenticated user or returns existing valid token. Only one token per user is allowed.",
        method = "POST",
        operationId = "createLongLivingToken",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Token created or returned successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = TokenResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - authentication required",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping
    fun createLongLivingToken(): ResponseEntity<TokenResponse> {
        val user = userDetailsService.getCurrentUser()
        val tokenEntity = longLivingTokenService.getOrCreate(user)
        return ResponseEntity.ok(TokenResponse(tokenEntity.token))
    }

    @Operation(
        summary = "Get existing long-living token",
        description = "Returns the existing long-living token for the authenticated user if it exists and is valid.",
        method = "GET",
        operationId = "getLongLivingToken",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Token found and returned",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = TokenResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "No valid token found for user",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - authentication required",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
        ],
    )
    @GetMapping
    fun getLongLivingToken(): ResponseEntity<TokenResponse> {
        val user = userDetailsService.getCurrentUser()
        val tokenEntity = longLivingTokenService.getOrCreate(user)
        return ResponseEntity.ok(TokenResponse(tokenEntity.token))
    }

    @Operation(
        summary = "Delete long-living token",
        description = "Deletes the long-living token for the authenticated user.",
        method = "DELETE",
        operationId = "deleteLongLivingToken",
        responses = [
            ApiResponse(
                responseCode = "204",
                description = "Token deleted successfully",
            ),
            ApiResponse(
                responseCode = "404",
                description = "No token found to delete",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - authentication required",
                content = [
                    Content(
                        mediaType = "application/problem+json",
                        schema = Schema(implementation = ProblemDetail::class),
                    ),
                ],
            ),
        ],
    )
    @DeleteMapping
    fun deleteLongLivingToken(): ResponseEntity<Unit> {
        val user = userDetailsService.getCurrentUser()
        val deleted = longLivingTokenService.deleteForUser(user.id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
