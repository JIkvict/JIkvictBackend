package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.annotation.AnyTeacher
import org.jikvict.jikvictbackend.annotation.RWTeacher
import org.jikvict.jikvictbackend.model.dto.UserDto
import org.jikvict.jikvictbackend.model.dto.UsersOfGroupsDto
import org.jikvict.jikvictbackend.model.mapper.UserMapper
import org.jikvict.jikvictbackend.service.UserService
import org.springframework.http.ResponseEntity
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
    @AnyTeacher
    @GetMapping("/{id}")
    fun getUserById(
        @PathVariable id: Long,
    ): ResponseEntity<UserDto> = ResponseEntity.ok(userService.getUserById(id))


    @AnyTeacher
    @PostMapping("/batch")
    fun getUsersByIds(
        @RequestBody ids: List<Long>,
    ): ResponseEntity<List<UserDto>> = ResponseEntity.ok(userService.getBatchByIds(ids))

    @AnyTeacher
    @GetMapping
    fun getAllUsers(): ResponseEntity<List<UserDto>> = ResponseEntity.ok(userService.getAllUsers())

    @AnyTeacher
    @PostMapping("/of-group")
    fun getUsersOfGroup(dto: UsersOfGroupsDto): ResponseEntity<List<UserDto>> = ResponseEntity.ok(userService.getUsersOfGroups(dto.groupIds))

    @RWTeacher
    @PostMapping
    fun registerUsers(
        @RequestBody aisIds: List<String>,
    ): ResponseEntity<List<UserDto>> {
        val imported = aisIds.mapNotNull { userService.importUserEntityByAisId(it) }
        val result = imported.map { userMapper.toUserDto(it) }
        return ResponseEntity.ok(result)
    }
}
