package com.vaddshah2626.springexposed.features.products.dtos

data class ProductDto(
    val id: Long? = null,
    val name: String,
    val price: Double,
    val stock: Int
)