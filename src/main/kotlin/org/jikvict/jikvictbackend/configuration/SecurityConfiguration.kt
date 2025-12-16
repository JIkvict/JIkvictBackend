package org.jikvict.jikvictbackend.configuration

import org.jikvict.jikvictbackend.service.auth.LdapAuthenticationProvider
import org.jikvict.jikvictbackend.service.filter.AutoAuthenticationFilter
import org.jikvict.jikvictbackend.service.filter.JwtAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfiguration(
    private val jwtAuthFilter: JwtAuthFilter,
    private val ldapAuthenticationProvider: LdapAuthenticationProvider,
    private val autoAuthenticationFilter: AutoAuthenticationFilter?,
) {
    @Bean
    fun filterChain(
        http: HttpSecurity,
        authenticationProvider: AuthenticationProvider,
    ): SecurityFilterChain {
        http {
            csrf { disable() }
            cors { disable() }
            authorizeHttpRequests {
                // Require auth for long-living token endpoints
                authorize("/api/auth/long-token", authenticated)
                authorize("/api/auth/long-token/**", authenticated)
                // Public auth endpoints (login/refresh)
                authorize("/api/auth/**", permitAll)
                authorize("/v3/api-docs", permitAll)
                authorize("/api/v1/**", permitAll)
                authorize(anyRequest, authenticated)
            }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }

            exceptionHandling {
                authenticationEntryPoint =
                    AuthenticationEntryPoint { _, response, _ ->
                        response.sendError(401)
                    }
                accessDeniedHandler =
                    AccessDeniedHandler { _, response, _ ->
                        response.sendError(403)
                    }
            }

            addFilterBefore<UsernamePasswordAuthenticationFilter>(
                jwtAuthFilter,
            )

            autoAuthenticationFilter?.let {
                addFilterBefore<JwtAuthFilter>(
                    it,
                )
            }
        }
        http.authenticationProvider(authenticationProvider)
        return http.build()
    }

    @Bean
    fun authenticationManager(http: HttpSecurity): AuthenticationManager =
        http
            .getSharedObject(AuthenticationManagerBuilder::class.java)
            .authenticationProvider(ldapAuthenticationProvider)
            .build()
}
