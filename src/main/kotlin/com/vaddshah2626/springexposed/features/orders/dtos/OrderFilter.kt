package com.vaddshah2626.springexposed.features.orders.dtos

data class OrderFilter(
    val page: Int = 1,
    val size: Int = 10,
)