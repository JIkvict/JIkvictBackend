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
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

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
        corsConfigurationSource: CorsConfigurationSource,
    ): SecurityFilterChain {
        http {
            csrf { disable() }
            cors { configurationSource = corsConfigurationSource }
            authorizeHttpRequests {
                authorize("/auth/**", permitAll)
                authorize("/v3/api-docs", permitAll)
                authorize(anyRequest, authenticated)
            }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
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
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun authenticationManager(
        http: HttpSecurity,
    ): AuthenticationManager =
        http
            .getSharedObject(AuthenticationManagerBuilder::class.java)
            .authenticationProvider(ldapAuthenticationProvider)
            .build()
}
