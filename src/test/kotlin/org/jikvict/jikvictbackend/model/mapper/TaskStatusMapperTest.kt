package org.jikvict.jikvictbackend.model.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TaskStatusMapperTest(
    val taskStatusMapper: TaskStatusMapper,
    val objectMapper: ObjectMapper,
) {

    @Test
    fun test() {
        // Given
        val message = "Hello"
        val assignmentId = 123L
        val ts = TaskStatus().apply {
            id = 1
            this.message = message
            this.parameters = objectMapper.createObjectNode().put("assignmentId", assignmentId).toString()
        }
        // When
        val result = taskStatusMapper.toUnacceptedSubmission(ts)
        // Then
        assertThat(result.message).isEqualTo(message)
        assertThat(result.assignmentId).isEqualTo(assignmentId)
    }

}
