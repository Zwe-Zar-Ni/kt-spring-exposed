package com.vaddshah2626.springexposed.features.users

import com.vaddshah2626.springexposed.features.users.dtos.UserResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {
    @GetMapping("/me")
    fun getCurrentUser(authentication: Authentication): ResponseEntity<UserResponse> {
        val response = userService.getUserByEmail(authentication.name)
        return ResponseEntity.ok(response)
    }
}