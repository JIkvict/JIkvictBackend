package org.jikvict.jikvictbackend.problem.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.jikvict.problem.model.forType
import org.jikvict.problem.model.getAdditionalProperties
import org.jikvict.problem.model.withProperty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.net.URI

class ProblemDetailTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun `should create problem detail with required fields`() {
        // Given
        val type = URI.create("https://jikvict.org/problems/validation-error")
        val title = "Validation Error"
        val status = HttpStatus.BAD_REQUEST

        // When
        val problemDetail = ProblemDetail.forStatus(status)
        problemDetail.type = type
        problemDetail.title = title

        // Then
        assertEquals(type, problemDetail.type)
        assertEquals(title, problemDetail.title)
        assertEquals(status.value(), problemDetail.status)
        assertNull(problemDetail.detail)
        assertNull(problemDetail.instance)
    }

    @Test
    fun `should create problem detail with all fields`() {
        // Given
        val type = URI.create("https://jikvict.org/problems/validation-error")
        val title = "Validation Error"
        val status = HttpStatus.BAD_REQUEST
        val detail = "Invalid input provided"
        val instance = URI.create("/api/users/123")

        // When
        val problemDetail = ProblemDetail.forStatus(status)
        problemDetail.type = type
        problemDetail.title = title
        problemDetail.detail = detail
        problemDetail.instance = instance

        // Then
        assertEquals(type, problemDetail.type)
        assertEquals(title, problemDetail.title)
        assertEquals(status.value(), problemDetail.status)
        assertEquals(detail, problemDetail.detail)
        assertEquals(instance, problemDetail.instance)
    }

    @Test
    fun `should create problem detail with additional properties`() {
        // Given
        val type = URI.create("https://jikvict.org/problems/validation-error")
        val title = "Validation Error"
        val status = HttpStatus.BAD_REQUEST
        val detail = "Invalid input provided"
        val instance = URI.create("/api/users/123")

        // When
        val problemDetail = ProblemDetail.forStatus(status)
        problemDetail.type = type
        problemDetail.title = title
        problemDetail.detail = detail
        problemDetail.instance = instance
        problemDetail.withProperty("timestamp", "2023-01-01T12:00:00Z")
        problemDetail.withProperty("errors", mapOf("name" to "Name is required"))

        // Then
        assertEquals(type, problemDetail.type)
        assertEquals(title, problemDetail.title)
        assertEquals(status.value(), problemDetail.status)
        assertEquals(detail, problemDetail.detail)
        assertEquals(instance, problemDetail.instance)
        assertEquals("2023-01-01T12:00:00Z", problemDetail.getAdditionalProperties()["timestamp"])
        assertEquals(mapOf("name" to "Name is required"), problemDetail.getAdditionalProperties()["errors"])
    }

    @Test
    fun `should serialize to JSON according to RFC 9457`() {
        // Given
        val type = URI.create("https://jikvict.org/problems/validation-error")
        val title = "Validation Error"
        val status = HttpStatus.BAD_REQUEST
        val detail = "Invalid input provided"
        val instance = URI.create("/api/users/123")

        val problemDetail = ProblemDetail.forStatus(status)
        problemDetail.type = type
        problemDetail.title = title
        problemDetail.detail = detail
        problemDetail.instance = instance
        problemDetail.withProperty("timestamp", "2023-01-01T12:00:00Z")
        problemDetail.withProperty("errors", mapOf("name" to "Name is required"))

        // When
        val json = objectMapper.writeValueAsString(problemDetail)

        // Then
        val jsonNode = objectMapper.readTree(json)
        assertEquals("https://jikvict.org/problems/validation-error", jsonNode.get("type").asText())
        assertEquals("Validation Error", jsonNode.get("title").asText())
        assertEquals(400, jsonNode.get("status").asInt())
        assertEquals("Invalid input provided", jsonNode.get("detail").asText())
        assertEquals("/api/users/123", jsonNode.get("instance").asText())

        val propertiesNode = jsonNode.get("properties")
        assertNotNull(propertiesNode, "Properties node should not be null")
        assertEquals("2023-01-01T12:00:00Z", propertiesNode.get("timestamp").asText())
        assertNotNull(propertiesNode.get("errors"))
        assertEquals("Name is required", propertiesNode.get("errors").get("name").asText())
    }

    @Test
    fun `should create problem detail for status`() {
        // Given
        val status = HttpStatus.BAD_REQUEST
        val detail = "Invalid input provided"

        // When
        val problemDetail = ProblemDetail.forStatus(status)
        problemDetail.detail = detail

        // Then
        assertEquals(URI.create("about:blank"), problemDetail.type)
        assertEquals("Bad Request", problemDetail.title)
        assertEquals(status.value(), problemDetail.status)
        assertEquals(detail, problemDetail.detail)
        assertNull(problemDetail.instance)
    }

    @Test
    fun `should create problem detail for type`() {
        // Given
        val type = URI.create("https://jikvict.org/problems/validation-error")
        val title = "Validation Error"
        val status = HttpStatus.BAD_REQUEST
        val detail = "Invalid input provided"
        val instance = URI.create("/api/users/123")

        // When
        val problemDetail = forType(type, title, status, detail, instance)

        // Then
        assertEquals(type, problemDetail.type)
        assertEquals(title, problemDetail.title)
        assertEquals(status.value(), problemDetail.status)
        assertEquals(detail, problemDetail.detail)
        assertEquals(instance, problemDetail.instance)
    }
}
