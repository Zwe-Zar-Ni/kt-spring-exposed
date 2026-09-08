package com.vaddshah2626.springexposed.springexposed.features.products

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ProductRepository {

    fun findAll(): List<ProductDto> {
        return ProductTable.selectAll().map {
            ProductDto(
                id = it[ProductTable.id].value,
                name = it[ProductTable.name],
                price = it[ProductTable.price],
                stock = it[ProductTable.stock]
            )
        }
    }

    fun findById(id: Long): ProductDto? {
        return ProductTable.selectAll()
            .where { ProductTable.id eq id }
            .map {
                ProductDto(
                    id = it[ProductTable.id].value,
                    name = it[ProductTable.name],
                    price = it[ProductTable.price],
                    stock = it[ProductTable.stock]
                )
            }
            .singleOrNull()
    }

    fun save(product: ProductDto): ProductDto {
        val newId = ProductTable.insertAndGetId {
            it[name] = product.name
            it[price] = product.price
            it[stock] = product.stock
        }
        return product.copy(id = newId.value)
    }

    fun deleteById(id: Long): Boolean {
        return ProductTable.deleteWhere { ProductTable.id eq id } > 0
    }
}