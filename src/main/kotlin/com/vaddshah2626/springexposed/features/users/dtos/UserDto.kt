package com.vaddshah2626.springexposed.features.users.dtos

data class UserDto(
    val id: Long? = null,
    val name: String,
    val email: String,
    val password: String,
)