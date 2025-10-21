package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.model.dto.UserDto
import org.jikvict.jikvictbackend.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/teacher/users")
class UsersController(
    private val userService: UserService,
) {

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<UserDto> {
        return ResponseEntity.ok(userService.getUserById(id))
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping
    fun getAllUsers(): ResponseEntity<List<UserDto>> {
        return ResponseEntity.ok(userService.getAllUsers())
    }

}
