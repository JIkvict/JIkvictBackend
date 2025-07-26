package org.jikvict.jikvictbackend.model.mapper

import org.jikvict.jikvictbackend.entity.Task
import org.jikvict.jikvictbackend.model.dto.TaskDto
import org.mapstruct.Mapper
import org.mapstruct.MappingConstants
import org.mapstruct.ReportingPolicy

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING,
)
interface TaskMapper {
    fun toEntity(taskDto: TaskDto): Task

    fun toDto(task: Task): TaskDto
}
