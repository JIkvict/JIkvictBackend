package org.jikvict.jikvictbackend.service

import org.jikvict.jikvictbackend.service.assignment.AssignmentService
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@SpringBootTest
class AssignmentServiceTest(
    private val assignmentService: AssignmentService,
) {
    @Test
    fun `should return description`() {
        // Given
        val assignmentNumber = 1
        // When
        val description = assignmentService.getAssignmentDescription(assignmentNumber)
        // Then
        println(description)
    }
}
