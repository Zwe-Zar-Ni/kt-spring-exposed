package com.vaddshah2626.springexposed.features.orders

import com.vaddshah2626.springexposed.features.users.UserTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object OrderTable : LongIdTable("orders") {
    val userId = reference("user_id", UserTable)
    val status = enumerationByName("status", 20, OrderStatus::class)
    val total = integer("total")
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestampWithTimeZone)
}