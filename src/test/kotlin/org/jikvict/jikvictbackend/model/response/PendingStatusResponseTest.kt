package org.jikvict.jikvictbackend.model.response

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test

class PendingStatusResponseTest {
    @Test
    fun `should return unwrapped json`() {
        // Given
        val pendingStatus = PendingStatus.DONE
        val payload = ResponsePayload("Hello")
        val response = PendingStatusResponse(payload, pendingStatus)

        // When
        val json = jacksonObjectMapper().writeValueAsString(response)
        val result = jacksonObjectMapper().readValue(json, PendingStatusResponse::class.java)
        // Then
        assertNotNull(json)
        assertTrue(json.contains("\"data\":\"Hello\""))
        assertThat(result).isEqualTo(response)
    }
}
