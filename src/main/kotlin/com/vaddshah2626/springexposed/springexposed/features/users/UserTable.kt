package com.vaddshah2626.springexposed.springexposed.features.users

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object UserTable : LongIdTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 50)
    val password = varchar("password", 255)
}