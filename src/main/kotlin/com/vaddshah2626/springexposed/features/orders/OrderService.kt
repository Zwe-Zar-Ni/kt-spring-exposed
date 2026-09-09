package com.vaddshah2626.springexposed.features.orders

import com.vaddshah2626.springexposed.common.api.PageResponse
import com.vaddshah2626.springexposed.features.orders.dtos.CreateOrderRequest
import com.vaddshah2626.springexposed.features.orders.dtos.OrderFilter
import com.vaddshah2626.springexposed.features.orders.dtos.OrderItemDto
import com.vaddshah2626.springexposed.features.orders.dtos.OrderResponse
import com.vaddshah2626.springexposed.features.products.ProductRepository
import com.vaddshah2626.springexposed.features.users.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToInt

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository
) {

    fun createOrder(email: String, request: CreateOrderRequest): OrderResponse {
        val user = userRepository.findByEmail(email)
            ?: throw NoSuchElementException("User not found with email: $email")

        if (request.items.isEmpty()) {
            throw IllegalStateException("Items should not be empty")
        }

        val items = request.items.map { item ->
            val product = productRepository.findById(item.productId)
                ?: throw NoSuchElementException("Product not found with id: ${item.productId}")
            if (product.stock < item.quantity) {
                throw IllegalStateException("${product.name} only has ${product.stock} in stock.")
            }
            OrderItemDto(
                productId = item.productId,
                quantity = item.quantity,
                price = product.price,
                productName = product.name
            )
        }

        val total = items.sumOf { (it.price * it.quantity).roundToInt() }
        return orderRepository.create(user.id!!, items, total)
    }

    fun listOrders(email: String, filter: OrderFilter): PageResponse<OrderResponse> {
        val user = userRepository.findByEmail(email)
            ?: throw NoSuchElementException("User not found with email: $email")
        return orderRepository.findAllByUser(user.id!!, filter)
    }

    fun getOrderDetails(orderId: Long, email: String): OrderResponse {
        val user = userRepository.findByEmail(email)
            ?: throw NoSuchElementException("User not found with email: $email")
        return orderRepository.getOrderDetails(orderId, user.id!!)
    }
}