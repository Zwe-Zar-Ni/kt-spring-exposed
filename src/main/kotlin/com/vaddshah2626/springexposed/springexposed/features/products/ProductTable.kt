package com.vaddshah2626.springexposed.springexposed.features.products

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object ProductTable : LongIdTable("products") {
    val name = varchar("name", 255)
    val price = double("price")
    val stock = integer("stock")
}