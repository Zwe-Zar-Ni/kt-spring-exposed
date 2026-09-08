package com.vaddshah2626.springexposed.springexposed.common.api


import java.time.Instant

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val fieldErrors: Map<String, String>? = null
)