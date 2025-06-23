package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.model.response.CustomResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1")
@RestController()
class BasicController {
    @GetMapping("/ping")
    fun ping(): ResponseEntity<String> = ResponseEntity.ok("pong")

    @GetMapping("/hello")
    fun hello(): ResponseEntity<CustomResponse> = ResponseEntity.ok(CustomResponse("Hello, World!"))
}
