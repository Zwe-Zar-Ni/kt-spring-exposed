package com.vaddshah2626.springexposed

import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.jetbrains.exposed.v1.spring.boot4.autoconfigure.ExposedAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration

@SpringBootApplication
@ImportAutoConfiguration(
	value = [ExposedAutoConfiguration::class],
	exclude = [DataSourceTransactionManagerAutoConfiguration::class]
)
class ExposedSpringApplication

fun main() {
	runApplication<com.vaddshah2626.springexposed.ExposedSpringApplication>()
}