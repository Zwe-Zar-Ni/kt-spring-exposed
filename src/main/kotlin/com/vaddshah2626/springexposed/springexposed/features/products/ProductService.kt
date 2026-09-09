package com.vaddshah2626.springexposed.springexposed.features.products

import com.vaddshah2626.springexposed.springexposed.common.api.PageResponse
import com.vaddshah2626.springexposed.springexposed.features.products.dtos.CreateProductRequest
import com.vaddshah2626.springexposed.springexposed.features.products.dtos.ProductDto
import com.vaddshah2626.springexposed.springexposed.features.products.dtos.ProductFilter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProductService(private val productRepository: ProductRepository) {

    fun getAllProducts(filter : ProductFilter): PageResponse<ProductDto> {
        return productRepository.findAll(filter)
    }

    fun getProductById(id: Long): ProductDto? {
        return productRepository.findById(id)
    }

    fun createProduct(product: CreateProductRequest): ProductDto {
        return productRepository.save(product)
    }

    fun deleteProduct(id: Long): Boolean {
        return productRepository.deleteById(id)
    }

}