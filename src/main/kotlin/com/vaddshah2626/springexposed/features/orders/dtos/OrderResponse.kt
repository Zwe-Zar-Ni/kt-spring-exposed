package com.vaddshah2626.springexposed.features.orders.dtos

import com.vaddshah2626.springexposed.features.orders.OrderStatus
import com.vaddshah2626.springexposed.features.users.dtos.UserResponse
import java.time.OffsetDateTime

data class OrderItemDto(
    val productId: Long,
    val quantity: Int,
    val price : Double,
    val productName: String,
)

data class OrderResponse(
    val id: Long,
    val status: OrderStatus,
    val total: Int,
    val items: List<OrderItemDto>?,
    val user: UserResponse?,
    val createdAt: OffsetDateTime,
)