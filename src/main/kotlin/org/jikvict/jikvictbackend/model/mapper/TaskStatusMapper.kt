package org.jikvict.jikvictbackend.model.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import org.jikvict.jikvictbackend.entity.TaskStatus
import org.jikvict.jikvictbackend.model.domain.UnacceptedSubmission
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants
import org.mapstruct.ReportingPolicy
import org.springframework.beans.factory.annotation.Autowired


@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING,
)
abstract class TaskStatusMapper(
) {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Mapping(source = "createdAt", target = "time")
    @Mapping(target = "assignmentId", expression = "java(mapAssignmentId(taskStatus))")
    @Mapping(target = "message", expression = "java(mapMessage(taskStatus))")
    abstract fun toUnacceptedSubmission(taskStatus: TaskStatus): UnacceptedSubmission

    @Suppress("unused")
    protected fun mapAssignmentId(taskStatus: TaskStatus): Long {
        val node = objectMapper.readTree(taskStatus.parameters)
        return node?.get("assignmentId")?.asLong() ?: throw IllegalArgumentException("Assignment ID not found")
    }

    @Suppress("unused")
    protected fun mapMessage(taskStatus: TaskStatus): String {
        val message = taskStatus.message
        return message ?: "No information provided"
    }
}
