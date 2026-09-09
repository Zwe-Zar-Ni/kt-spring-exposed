package com.vaddshah2626.springexposed.features.orders.dtos

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class OrderItemRequest(
    @field:NotNull(message = "Product id is required")
    val productId: Long,

    @field:NotNull(message = "Quantity is required")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Int
)

data class CreateOrderRequest(
    @field:NotEmpty(message = "Order must contain at least one item")
    @field:Valid
    val items: List<OrderItemRequest>
)