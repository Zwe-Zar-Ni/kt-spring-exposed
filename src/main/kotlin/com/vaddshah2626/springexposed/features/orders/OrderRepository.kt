package com.vaddshah2626.springexposed.features.orders

import com.vaddshah2626.springexposed.common.api.PageResponse
import com.vaddshah2626.springexposed.features.orders.dtos.OrderFilter
import com.vaddshah2626.springexposed.features.orders.dtos.OrderItemDto
import com.vaddshah2626.springexposed.features.orders.dtos.OrderResponse
import com.vaddshah2626.springexposed.features.products.ProductTable
import com.vaddshah2626.springexposed.features.users.UserTable
import com.vaddshah2626.springexposed.features.users.dtos.UserResponse
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import kotlin.math.ceil

@Repository
class OrderRepository {

    @Transactional
    fun create(userId: Long, items: List<OrderItemDto>, total: Int): OrderResponse {
        val newOrderId = OrderTable.insertAndGetId {
            it[OrderTable.userId] = userId
            it[status] = OrderStatus.PENDING
            it[OrderTable.total] = total
        }
        items.forEach { item ->
            OrderItemTable.insertAndGetId {
                it[orderId] = newOrderId.value
                it[productId] = item.productId
                it[quantity] = item.quantity
            }
        }
        return OrderResponse(
            id = newOrderId.value,
            status = OrderStatus.PENDING,
            total = total,
            items = null,
            user = null,
            createdAt = OffsetDateTime.now()
        )
    }

    @Transactional(readOnly = true)
    fun findAllByUser(userId: Long, filter: OrderFilter): PageResponse<OrderResponse> {
        val query = OrderTable.selectAll().where { OrderTable.userId eq userId }
        val totalElements = OrderTable.selectAll().where { OrderTable.userId eq userId }.count()

        val offsetValue = ((filter.page - 1) * filter.size).toLong()

        val content = query.limit(filter.size).offset(offsetValue).map {
            OrderResponse(
                id = it[OrderTable.id].value,
                status = it[OrderTable.status],
                total = it[OrderTable.total],
                items = null,
                user = null,
                createdAt = it[OrderTable.createdAt]
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
    fun getOrderDetails(orderId: Long, userId: Long): OrderResponse {

        // 1. Join OrderTable -> UserTable AND OrderTable -> OrderItemTable -> ProductTable
        val rows =
            (OrderTable leftJoin UserTable leftJoin OrderItemTable leftJoin ProductTable).selectAll()
                .where { OrderTable.id eq orderId }
                .andWhere { OrderTable.userId eq userId }

        // 2. Extract the parent Order details from the first row
        val firstRow = rows.firstOrNull() ?: throw NoSuchElementException("Order not found.")

        // 3. Map the flat rows into a list of nested Order Items
        val items = rows.mapNotNull { row ->
            row.getOrNull(OrderItemTable.id) ?: return@mapNotNull null
            OrderItemDto(
                productId = row[OrderItemTable.productId].value,
                quantity = row[OrderItemTable.quantity],
                price = row[ProductTable.price],
                productName = row[ProductTable.name]
            )
        }

        // 4. Construct and return the full DTO
        return OrderResponse(
            id = firstRow[OrderTable.id].value,
            status = firstRow[OrderTable.status],
            total = firstRow[OrderTable.total],
            createdAt = firstRow[OrderTable.createdAt],
            items = items,
            user = UserResponse(
                id = firstRow[UserTable.id].value,
                name = firstRow[UserTable.name],
                email = firstRow[UserTable.email],
            )
        )
    }
}