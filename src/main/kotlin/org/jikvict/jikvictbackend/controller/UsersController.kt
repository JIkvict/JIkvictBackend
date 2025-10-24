package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.model.dto.UserDto
import org.jikvict.jikvictbackend.model.mapper.UserMapper
import org.jikvict.jikvictbackend.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/teacher/users")
class UsersController(
    private val userService: UserService,
    private val userMapper: UserMapper,
) {
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{id}")
    fun getUserById(
        @PathVariable id: Long,
    ): ResponseEntity<UserDto> = ResponseEntity.ok(userService.getUserById(id))

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping
    fun getAllUsers(): ResponseEntity<List<UserDto>> = ResponseEntity.ok(userService.getAllUsers())

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping
    fun registerUsers(
        @RequestBody aisIds: List<String>,
    ): ResponseEntity<List<UserDto>> {
        val imported = aisIds.mapNotNull { userService.importUserEntityByAisId(it) }
        val result = imported.map { userMapper.toUserDto(it) }
        return ResponseEntity.ok(result)
    }
}
