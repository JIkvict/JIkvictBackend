package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.model.dto.TaskDto
import org.jikvict.jikvictbackend.model.mapper.TaskMapper
import org.jikvict.jikvictbackend.repository.TaskRepository
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tasks")
class TaskController(
    private val taskRepository: TaskRepository,
    private val taskMapper: TaskMapper,
) {
    @GetMapping
    fun getAllTasks(): ResponseEntity<List<TaskDto>> {
        val tasks = taskRepository.findAll()
        return ResponseEntity.ok(tasks.map { taskMapper.toDto(it) })
    }

    @GetMapping("/{id}")
    fun getTaskById(
        @PathVariable id: Long,
    ): ResponseEntity<TaskDto> {
        val task =
            taskRepository
                .findById(id)
                .orElseThrow { ServiceException(HttpStatus.NOT_FOUND, "Task with ID $id not found") }
        return ResponseEntity.ok(taskMapper.toDto(task))
    }

    @PostMapping
    fun createTask(
        @RequestBody taskDto: TaskDto,
    ): ResponseEntity<TaskDto> {
        val task = taskMapper.toEntity(taskDto)
        val savedTask = taskRepository.save(task)
        return ResponseEntity.status(HttpStatus.CREATED).body(taskMapper.toDto(savedTask))
    }

    @PutMapping("/{id}")
    fun updateTask(
        @PathVariable id: Long,
        @RequestBody taskDto: TaskDto,
    ): ResponseEntity<TaskDto> {
        if (!taskRepository.existsById(id)) {
            throw ServiceException(HttpStatus.NOT_FOUND, "Task with ID $id not found")
        }

        val task = taskMapper.toEntity(taskDto)
        task.id = id
        val updatedTask = taskRepository.save(task)
        return ResponseEntity.ok(taskMapper.toDto(updatedTask))
    }

    @DeleteMapping("/{id}")
    fun deleteTask(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        if (!taskRepository.existsById(id)) {
            throw ServiceException(HttpStatus.NOT_FOUND, "Task with ID $id not found")
        }

        taskRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
