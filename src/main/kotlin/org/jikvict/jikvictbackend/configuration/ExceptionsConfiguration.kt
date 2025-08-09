package org.jikvict.jikvictbackend.configuration

import org.jikvict.problems.processors.contract.ExceptionProcessor
import org.jikvict.spring.problems.registry.ProcessorsRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import java.net.URI

@Configuration
class ExceptionsConfiguration(
    private val registry: ProcessorsRegistry,
) {

    @Bean
    fun badCredentialsExceptionProcessor(): ExceptionProcessor<BadCredentialsException> {
        val processor = BadCredentialsExceptionProcessor()
        registry.register(processor, BadCredentialsException::class.java)
        return processor
    }

    @Bean
    fun authenticationExceptionProcessor(): ExceptionProcessor<AuthenticationException> {
        val processor = AuthenticationExceptionProcessor()
        registry.register(processor, AuthenticationException::class.java)
        return processor
    }
}

class BadCredentialsExceptionProcessor : ExceptionProcessor<BadCredentialsException> {
    override fun convertToDetail(exception: BadCredentialsException): ProblemDetail {
        return ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED).apply {
            title = "Authentication Failed"
            detail = exception.message ?: "Invalid credentials provided"
            type = URI("application:error${exception::class.java.name}")
        }
    }
}

class AuthenticationExceptionProcessor : ExceptionProcessor<AuthenticationException> {
    override fun convertToDetail(exception: AuthenticationException): ProblemDetail {
        return ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED).apply {
            title = "Authentication Failed"
            detail = exception.message ?: "Authentication error occurred"
            type = URI("application:error${exception::class.java.name}")
        }
    }
}


