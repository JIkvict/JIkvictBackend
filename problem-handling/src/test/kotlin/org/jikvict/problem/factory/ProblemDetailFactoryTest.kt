package org.jikvict.problem.factory

import org.jikvict.problem.config.ProblemDetailProperties
import org.jikvict.problem.model.getAdditionalProperties
import org.jikvict.problem.type.ProblemTypeRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest
import java.net.URI

class ProblemDetailFactoryTest {

    private lateinit var factory: ProblemDetailFactory
    private lateinit var properties: ProblemDetailProperties
    private lateinit var request: ServletWebRequest

    @BeforeEach
    fun setUp() {
        properties = ProblemDetailProperties(
            includeStackTrace = false,
            includeExceptionClassName = true,
            includeTimestamp = true,
            defaultMessages = mapOf(
                "IllegalArgumentException" to "Invalid argument provided",
                "Exception" to "An unexpected error occurred"
            )
        )
        factory = ProblemDetailFactory(properties)
        request = ServletWebRequest(MockHttpServletRequest("GET", "/api/users/123"))
    }

    @Test
    fun `should create problem detail from exception`() {
        // Given
        val exception = IllegalArgumentException("Invalid user ID")

        // When
        val problemDetail = factory.createProblemDetail(
            exception = exception,
            problemType = ProblemTypeRegistry.VALIDATION_ERROR
        )

        // Then
        assertEquals(ProblemTypeRegistry.VALIDATION_ERROR.uri, problemDetail.type)
        assertEquals(ProblemTypeRegistry.VALIDATION_ERROR.title, problemDetail.title)
        assertEquals(ProblemTypeRegistry.VALIDATION_ERROR.status.value(), problemDetail.status)
        assertEquals("Invalid user ID", problemDetail.detail)
        assertNotNull(problemDetail.getAdditionalProperties()["timestamp"])
        assertEquals(exception.javaClass.name, problemDetail.getAdditionalProperties()["exception"])
    }

    @Test
    fun `should create validation problem detail`() {
        // Given
        val exception = IllegalArgumentException("Invalid user data")
        val errors = mapOf(
            "name" to "Name is required",
            "email" to "Email must be valid"
        )

        // When
        val problemDetail = factory.createValidationProblemDetail(
            exception = exception,
            errors = errors
        )

        // Then
        assertEquals(ProblemTypeRegistry.VALIDATION_ERROR.uri, problemDetail.type)
        assertEquals(ProblemTypeRegistry.VALIDATION_ERROR.title, problemDetail.title)
        assertEquals(ProblemTypeRegistry.VALIDATION_ERROR.status.value(), problemDetail.status)
        assertEquals("Invalid user data", problemDetail.detail)
        assertEquals(errors, problemDetail.getAdditionalProperties()["errors"])
    }

    @Test
    fun `should create not found problem detail`() {
        // Given
        val exception = NoSuchElementException("User not found")
        val resourceType = "User"
        val resourceId = "123"

        // When
        val problemDetail = factory.createNotFoundProblemDetail(
            exception = exception,
            resourceType = resourceType,
            resourceId = resourceId
        )

        // Then
        assertEquals(ProblemTypeRegistry.NOT_FOUND.uri, problemDetail.type)
        assertEquals(ProblemTypeRegistry.NOT_FOUND.title, problemDetail.title)
        assertEquals(ProblemTypeRegistry.NOT_FOUND.status.value(), problemDetail.status)
        assertEquals("The User with ID '123' was not found", problemDetail.detail)
        assertEquals(resourceType, problemDetail.getAdditionalProperties()["resourceType"])
        assertEquals(resourceId, problemDetail.getAdditionalProperties()["resourceId"])
    }

    @Test
    fun `should create internal server error problem detail`() {
        // Given
        val exception = RuntimeException("Something went wrong")

        // When
        val problemDetail = factory.createInternalServerErrorProblemDetail(
            exception = exception
        )

        // Then
        assertEquals(ProblemTypeRegistry.INTERNAL_SERVER_ERROR.uri, problemDetail.type)
        assertEquals(ProblemTypeRegistry.INTERNAL_SERVER_ERROR.title, problemDetail.title)
        assertEquals(ProblemTypeRegistry.INTERNAL_SERVER_ERROR.status.value(), problemDetail.status)
        assertEquals("Something went wrong", problemDetail.detail)
    }

    @Test
    fun `should create custom problem detail`() {
        // Given
        val exception = Exception("Custom error")
        val type = URI.create("https://jikvict.org/problems/custom-error")
        val title = "Custom Error"
        val status = HttpStatus.CONFLICT
        val detail = "A custom error occurred"
        val instance = URI.create("/api/custom/123")

        // When
        val problemDetail = factory.createCustomProblemDetail(
            exception = exception,
            type = type,
            title = title,
            status = status,
            detail = detail,
            instance = instance
        )

        // Then
        assertEquals(type, problemDetail.type)
        assertEquals(title, problemDetail.title)
        assertEquals(status.value(), problemDetail.status)
        assertEquals(detail, problemDetail.detail)
        assertEquals(instance, problemDetail.instance)
    }

    @Test
    fun `should use default message when exception message is null`() {
        // Given
        val exception = IllegalArgumentException()

        // When
        val problemDetail = factory.createProblemDetail(
            exception = exception,
            problemType = ProblemTypeRegistry.VALIDATION_ERROR
        )

        // Then
        assertEquals("Invalid argument provided", problemDetail.detail)
    }

    @Test
    fun `should include stack trace when configured`() {
        // Given
        val propertiesWithStackTrace = ProblemDetailProperties(
            includeStackTrace = true
        )
        val factoryWithStackTrace = ProblemDetailFactory(propertiesWithStackTrace)
        val exception = RuntimeException("Test exception")

        // When
        val problemDetail = factoryWithStackTrace.createProblemDetail(
            exception = exception,
            problemType = ProblemTypeRegistry.INTERNAL_SERVER_ERROR
        )

        // Then
        val stackTrace = problemDetail.getAdditionalProperties()["stackTrace"] as String
        assertTrue(stackTrace.contains("RuntimeException: Test exception"))
        assertTrue(stackTrace.contains("at org.jikvict.problem.factory.ProblemDetailFactoryTest"))
    }
}
