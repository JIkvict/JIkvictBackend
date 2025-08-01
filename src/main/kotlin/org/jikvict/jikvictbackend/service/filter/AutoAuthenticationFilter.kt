package org.jikvict.jikvictbackend.service.filter

import org.jikvict.jikvictbackend.repository.UserRepository
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Component
@Profile("auto-auth")
class AutoAuthenticationFilter(
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        if (SecurityContextHolder.getContext().authentication == null) {
            val user = userRepository.findAll().firstOrNull()

            if (user != null) {
                val authToken =
                    UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.authorities,
                    )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)

                SecurityContextHolder.getContext().authentication = authToken
            }
        }

        chain.doFilter(request, response)
    }
}
