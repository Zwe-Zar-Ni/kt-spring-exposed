package com.vaddshah2626.springexposed.springexposed.features.products

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
    fun getAll(): List<ProductDto> = productService.getAllProducts()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<ProductDto> {
        val product = productService.getProductById(id)
        return if (product != null) ResponseEntity.ok(product)
        else ResponseEntity.notFound().build()
    }

    @PostMapping
    fun create(@RequestBody product: ProductDto): ResponseEntity<ProductDto> {
        val created = productService.createProduct(product)
        return ResponseEntity(created, HttpStatus.CREATED)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        return if (productService.deleteProduct(id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
    }
}