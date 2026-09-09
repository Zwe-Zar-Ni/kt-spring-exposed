package com.vaddshah2626.springexposed.features.orders

import com.vaddshah2626.springexposed.common.api.PageResponse
import com.vaddshah2626.springexposed.features.orders.dtos.CreateOrderRequest
import com.vaddshah2626.springexposed.features.orders.dtos.OrderFilter
import com.vaddshah2626.springexposed.features.orders.dtos.OrderResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService) {

    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateOrderRequest,
        authentication: Authentication
    ): ResponseEntity<OrderResponse> {
        val created = orderService.createOrder(authentication.name, request)
        return ResponseEntity(created, HttpStatus.CREATED)
    }

    @GetMapping
    fun list(
        filter: OrderFilter,
        authentication: Authentication
    ): ResponseEntity<PageResponse<OrderResponse>> {
        return ResponseEntity.ok(orderService.listOrders(authentication.name, filter))
    }

    @GetMapping("/{id}")
    fun details(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<OrderResponse> {
        return ResponseEntity.ok(orderService.getOrderDetails(id, authentication.name))
    }
}