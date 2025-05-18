package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.model.CustomResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1")
@RestController()
class BasicController {
    @GetMapping("/ping")
    fun ping(): ResponseEntity<String> {
        return ResponseEntity.ok("pong")
    }
    @GetMapping("/hello")
    fun hello(): ResponseEntity<CustomResponse> {
        return ResponseEntity.ok(CustomResponse("Hello, World!"))
    }
}
