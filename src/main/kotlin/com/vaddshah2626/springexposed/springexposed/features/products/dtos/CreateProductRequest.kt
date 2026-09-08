package com.vaddshah2626.springexposed.springexposed.features.products.dtos

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class CreateProductRequest(
    @field:NotBlank(message = "Product name is required")
    val name: String,

    @field:NotNull(message = "Price is required")
    @field:PositiveOrZero(message = "Price must be zero or positive")
    val price: Double,

    @field:NotNull(message = "Stock is required")
    @field:Min(value = 0, message = "Stock cannot be negative")
    val stock: Int
)