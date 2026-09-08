package com.vaddshah2626.springexposed.springexposed.features.products

data class ProductDto(
    val id: Long? = null,
    val name: String,
    val price: Double,
    val stock: Int
)