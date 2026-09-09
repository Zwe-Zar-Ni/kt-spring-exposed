package com.vaddshah2626.springexposed.features.users.dtos

data class UserResponse(
    val id: Long? = null,
    val name: String,
    val email: String
)