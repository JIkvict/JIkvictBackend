package org.jikvict.problem.advice

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jikvict.problem.factory.ProblemDetailFactory
import org.jikvict.problem.model.forType
import org.jikvict.problem.type.ProblemTypeRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.NoHandlerFoundException
import java.net.URI

class ProblemDetailExceptionHandlerTest {

    private lateinit var exceptionHandler: ProblemDetailExceptionHandler
    private lateinit var problemDetailFactory: ProblemDetailFactory
    private lateinit var request: WebRequest
    private lateinit var servletRequest: MockHttpServletRequest

    @BeforeEach
    fun setUp() {
        problemDetailFactory = mockk(relaxed = true)
        exceptionHandler = ProblemDetailExceptionHandler(problemDetailFactory)
        servletRequest = MockHttpServletRequest("GET", "/api/users/123")
        request = ServletWebRequest(servletRequest)
    }

    @Test
    fun `should handle IllegalArgumentException`() {
        // Given
        val exception = IllegalArgumentException("Invalid argument")
        val problemDetail = createMockProblemDetail(HttpStatus.BAD_REQUEST)

        every {
            problemDetailFactory.createProblemDetail(
                exception = any(),
                problemType = any(),
                detail = any(),
            )
        } returns problemDetail

        // When
        val response = exceptionHandler.handleIllegalArgumentException(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(MediaType.valueOf("application/problem+json"), response.headers.contentType)
        assertEquals(problemDetail, response.body)

        verify {
            problemDetailFactory.createProblemDetail(
                exception = exception,
                problemType = ProblemTypeRegistry.VALIDATION_ERROR,
            )
        }
    }

    @Test
    fun `should handle IllegalStateException`() {
        // Given
        val exception = IllegalStateException("Invalid state")
        val problemDetail = createMockProblemDetail(HttpStatus.BAD_REQUEST)

        every {
            problemDetailFactory.createProblemDetail(
                exception = any(),
                problemType = any(),
                detail = any(),
            )
        } returns problemDetail

        // When
        val response = exceptionHandler.handleIllegalStateException(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(MediaType.valueOf("application/problem+json"), response.headers.contentType)
        assertEquals(problemDetail, response.body)

        verify {
            problemDetailFactory.createProblemDetail(
                exception = exception,
                problemType = ProblemTypeRegistry.VALIDATION_ERROR,
            )
        }
    }

    @Test
    fun `should handle MethodArgumentNotValidException`() {
        // Given
        val bindingResult = mockk<BindingResult>()
        val fieldErrors = listOf(
            FieldError("user", "name", "Name is required"),
            FieldError("user", "email", "Email must be valid"),
        )
        every { bindingResult.fieldErrors } returns fieldErrors

        val exception = mockk<MethodArgumentNotValidException>()
        every { exception.bindingResult } returns bindingResult
        every { exception.message } returns "Validation failed"

        val problemDetail = createMockProblemDetail(HttpStatus.BAD_REQUEST)

        every {
            problemDetailFactory.createValidationProblemDetail(
                exception = any(),
                errors = any(),
            )
        } returns problemDetail

        // When
        val response = exceptionHandler.handleMethodArgumentNotValid(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response?.statusCode)
        assertEquals(MediaType.valueOf("application/problem+json"), response?.headers?.contentType)
        assertEquals(problemDetail, response?.body)

        verify {
            problemDetailFactory.createValidationProblemDetail(
                exception = exception,
                errors = mapOf(
                    "name" to "Name is required",
                    "email" to "Email must be valid",
                ),
            )
        }
    }

    @Test
    fun `should handle NoHandlerFoundException`() {
        // Given
        val exception = NoHandlerFoundException("GET", "/api/unknown", HttpHeaders())
        val problemDetail = createMockProblemDetail(HttpStatus.NOT_FOUND)

        every {
            problemDetailFactory.createProblemDetail(
                exception = any(),
                problemType = any(),
                detail = any(),
            )
        } returns problemDetail

        // When
        val response = exceptionHandler.handleNoHandlerFoundException(exception)

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response?.statusCode)
        assertEquals(MediaType.valueOf("application/problem+json"), response?.headers?.contentType)
        assertEquals(problemDetail, response?.body)

        verify {
            problemDetailFactory.createProblemDetail(
                exception = exception,
                problemType = ProblemTypeRegistry.NOT_FOUND,
                detail = "No handler found for GET /api/unknown",
            )
        }
    }

    @Test
    fun `should handle HttpRequestMethodNotSupportedException`() {
        // Given
        val exception = HttpRequestMethodNotSupportedException("POST", listOf("GET", "PUT"))
        val problemDetail = createMockProblemDetail(HttpStatus.METHOD_NOT_ALLOWED)

        every {
            problemDetailFactory.createProblemDetail(
                exception = any(),
                problemType = any(),
                detail = any(),
            )
        } returns problemDetail

        // When
        val response = exceptionHandler.handleHttpRequestMethodNotSupported(exception)

        // Then
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response?.statusCode)
        assertEquals(MediaType.valueOf("application/problem+json"), response?.headers?.contentType)
        assertEquals(problemDetail, response?.body)

        verify {
            problemDetailFactory.createProblemDetail(
                exception = exception,
                problemType = ProblemTypeRegistry.METHOD_NOT_ALLOWED,
                detail = "Method POST is not supported for this request. Supported methods are: GET, PUT",
            )
        }
    }

    @Test
    fun `should handle HttpMediaTypeNotSupportedException`() {
        // Given
        val exception = HttpMediaTypeNotSupportedException(
            MediaType.APPLICATION_XML,
            listOf(MediaType.APPLICATION_JSON),
        )
        val problemDetail = createMockProblemDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE)

        every {
            problemDetailFactory.createProblemDetail(
                exception = any(),
                problemType = any(),
                detail = any(),
            )
        } returns problemDetail

        // When
        val response = exceptionHandler.handleHttpMediaTypeNotSupported(exception)

        // Then
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response?.statusCode)
        assertEquals(MediaType.valueOf("application/problem+json"), response?.headers?.contentType)
        assertEquals(problemDetail, response?.body)

        verify {
            problemDetailFactory.createProblemDetail(
                exception = exception,
                problemType = ProblemTypeRegistry.UNSUPPORTED_MEDIA_TYPE,
                detail = "Media type application/xml is not supported. Supported media types are: application/json",
            )
        }
    }

    @Test
    fun `should handle MaxUploadSizeExceededException`() {
        // Given
        val exception = MaxUploadSizeExceededException(1000)
        val problemDetail = createMockProblemDetail(HttpStatus.BAD_REQUEST)

        every {
            problemDetailFactory.createProblemDetail(
                exception = any(),
                problemType = any(),
                detail = any(),
            )
        } returns problemDetail

        // When
        val response = exceptionHandler.handleMaxUploadSizeExceededException(exception)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(MediaType.valueOf("application/problem+json"), response.headers.contentType)
        assertEquals(problemDetail, response.body)

        verify {
            problemDetailFactory.createProblemDetail(
                exception = exception,
                problemType = ProblemTypeRegistry.VALIDATION_ERROR,
                detail = "Maximum upload size exceeded",
            )
        }
    }

    @Test
    fun `should handle generic Exception`() {
        // Given
        val exception = Exception("Unexpected error")
        val problemDetail = createMockProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR)

        every {
            problemDetailFactory.createInternalServerErrorProblemDetail(
                exception = any(),
            )
        } returns problemDetail

        // When
        val response = exceptionHandler.handleException(exception)

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(MediaType.valueOf("application/problem+json"), response.headers.contentType)
        assertEquals(problemDetail, response.body)

        verify {
            problemDetailFactory.createInternalServerErrorProblemDetail(
                exception = exception,
            )
        }
    }

    private fun createMockProblemDetail(status: HttpStatus): ProblemDetail {
        return forType(
            type = URI.create("https://jikvict.org/problems/test"),
            "Test Problem",
            status = HttpStatus.valueOf(status.value()),
            "Test detail",
            instance = URI.create("/test"),
        )
    }
}
