package com.vaddshah2626.springexposed.features.products

import com.vaddshah2626.springexposed.common.api.PageResponse
import com.vaddshah2626.springexposed.features.products.dtos.CreateProductRequest
import com.vaddshah2626.springexposed.features.products.dtos.ProductDto
import com.vaddshah2626.springexposed.features.products.dtos.ProductFilter
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/products")
class ProductController(private val productService: ProductService) {

    @GetMapping
    fun getAllPaginated(filter: ProductFilter): ResponseEntity<PageResponse<ProductDto>> {
        val result = productService.getAllProducts(filter)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<ProductDto> {
        val product = productService.getProductById(id)
        return if (product != null) ResponseEntity.ok(product)
        else ResponseEntity.notFound().build()
    }

    @PostMapping
    fun create(@Valid @RequestBody request: CreateProductRequest): ResponseEntity<ProductDto> {
        val created = productService.createProduct(request)
        return ResponseEntity(created, HttpStatus.CREATED)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        return if (productService.deleteProduct(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
    }
}