package com.vaddshah2626.springexposed.features.orders

import com.vaddshah2626.springexposed.features.products.ProductTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object OrderItemTable : LongIdTable("order_items") {
    val orderId = reference("order_id", OrderTable , onDelete = ReferenceOption.CASCADE)
    val productId = reference("product_id", ProductTable)
    val quantity = integer("quantity")
}