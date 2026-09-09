package com.vaddshah2626.springexposed.springexposed.features.products

import com.vaddshah2626.springexposed.springexposed.common.api.PageResponse
import com.vaddshah2626.springexposed.springexposed.features.products.dtos.CreateProductRequest
import com.vaddshah2626.springexposed.springexposed.features.products.dtos.ProductDto
import com.vaddshah2626.springexposed.springexposed.features.products.dtos.ProductFilter
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import kotlin.math.ceil

@Repository
class ProductRepository {

    @Transactional(readOnly = true)
    fun findAll(filter: ProductFilter): PageResponse<ProductDto> {
        val query = ProductTable.selectAll()
        val totalElements = ProductTable.selectAll().count()

        val offsetValue = ((filter.page - 1) * filter.size).toLong()

        filter.search?.takeIf { it.isNotBlank() }?.let { term ->
            query.andWhere { ProductTable.name like "%$term%" }
        }

        filter.stock?.takeIf { it > 0 }?.let { term ->
            query.andWhere { ProductTable.stock greaterEq term }
        }

        val content = query.limit(filter.size).offset(offsetValue).map {
            ProductDto(
                id = it[ProductTable.id].value,
                name = it[ProductTable.name],
                price = it[ProductTable.price],
                stock = it[ProductTable.stock]
            )
        }

        val totalPages = if (filter.size > 0) ceil(totalElements.toDouble() / filter.size).toInt() else 0
        return PageResponse(
            content = content,
            page = filter.page,
            size = filter.size,
            totalElements = totalElements,
            totalPages = totalPages
        )
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): ProductDto? {
        return ProductTable.selectAll().where { ProductTable.id eq id }.map {
            ProductDto(
                id = it[ProductTable.id].value,
                name = it[ProductTable.name],
                price = it[ProductTable.price],
                stock = it[ProductTable.stock]
            )
        }.singleOrNull()
    }

    @Transactional
    fun save(product: CreateProductRequest): ProductDto {
        val newId = ProductTable.insertAndGetId {
            it[name] = product.name
            it[price] = product.price
            it[stock] = product.stock
        }
        return ProductDto(
            id = newId.value, name = product.name, price = product.price, stock = product.stock
        )
    }

    @Transactional
    fun deleteById(id: Long): Boolean {
        return ProductTable.deleteWhere { ProductTable.id eq id } > 0
    }
}