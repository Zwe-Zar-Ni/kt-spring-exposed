package com.vaddshah2626.springexposed.features.users

import com.vaddshah2626.springexposed.features.users.dtos.UserResponse
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun getUserByEmail(email: String): UserResponse {
        val user =
            userRepository.findByEmail(email) ?: throw NoSuchElementException("User not found with email: $email")
        return UserResponse(user.id, user.name, user.email)
    }
}