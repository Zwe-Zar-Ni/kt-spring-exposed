package com.vaddshah2626.springexposed.features.auth

import com.vaddshah2626.springexposed.common.security.JwtUtil
import com.vaddshah2626.springexposed.features.auth.dtos.AuthResponse
import com.vaddshah2626.springexposed.features.auth.dtos.LoginRequest
import com.vaddshah2626.springexposed.features.auth.dtos.RegisterRequest
import com.vaddshah2626.springexposed.features.users.UserRepository
import com.vaddshah2626.springexposed.features.users.dtos.UserDto
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )

        val token = jwtUtil.generateToken(request.email)
        return ResponseEntity.ok(AuthResponse(token = token))
    }

    companion object {
        // Creates a lazy logger tied to this class
        private val log = LoggerFactory.getLogger(AuthController::class.java)
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): Any? {
        val existingUser = userRepository.findByEmail(request.email)

        if (existingUser != null) {
            throw IllegalStateException("User ${existingUser.email} already exists.")
        }

        val user = UserDto(
            name = request.name,
            email = request.email,
            password = passwordEncoder.encode(request.password)!!
        )
        userRepository.save(user)

        val token = jwtUtil.generateToken(request.email)
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse(token = token))
    }
}