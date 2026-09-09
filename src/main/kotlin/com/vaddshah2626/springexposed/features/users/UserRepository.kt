package com.vaddshah2626.springexposed.features.users

import com.vaddshah2626.springexposed.features.users.dtos.UserDto
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class UserRepository {
    @Transactional(readOnly = true)
    fun findByEmail(email: String): UserDto? {
        return UserTable.selectAll().where { UserTable.email eq email }.map {
            UserDto(
                id = it[UserTable.id].value,
                name = it[UserTable.name],
                email = it[UserTable.email],
                password = it[UserTable.password],
            )
        }.singleOrNull()
    }

    @Transactional
    fun save(user: UserDto): UserDto {
        val newId = UserTable.insertAndGetId {
            it[name] = user.name
            it[email] = user.email
            it[password] = user.password
        }
        return UserDto(
            id = newId.value,
            name = user.name,
            email = user.email,
            password = user.password
        )
    }
}