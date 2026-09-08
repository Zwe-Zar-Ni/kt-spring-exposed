package com.vaddshah2626.springexposed.springexposed.features.products.dtos

data class ProductFilter(
    val search: String? = null,
    val stock: Int? = null,
    val page: Int = 1,
    val size: Int = 10,
)