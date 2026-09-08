package com.vaddshah2626.springexposed.springexposed.features.products

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProductService(private val productRepository: ProductRepository) {

    @Transactional(readOnly = true)
    fun getAllProducts(): List<ProductDto> {
        return productRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun getProductById(id: Long): ProductDto? {
        return productRepository.findById(id)
    }

    fun createProduct(product: ProductDto): ProductDto {
        require(product.price >= 0.0) { "Price cannot be negative" }
        require(product.stock >= 0) { "Stock cannot be negative" }

        return productRepository.save(product)
    }

    fun deleteProduct(id: Long): Boolean {
        return productRepository.deleteById(id)
    }
}