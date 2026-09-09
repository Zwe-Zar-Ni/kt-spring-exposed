package com.vaddshah2626.springexposed.features.orders

import com.vaddshah2626.springexposed.features.users.UserTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object OrderTable : LongIdTable("orders") {
    val userId = reference("user_id", UserTable)
}