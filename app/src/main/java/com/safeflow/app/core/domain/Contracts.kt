package com.safeflow.app.core.domain

import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface Records<T> {
    fun observe(): Flow<List<T>>
    suspend fun all(): List<T>
    suspend fun find(id: String): T?
    suspend fun save(id: String, value: T)
}
interface Transaction { suspend fun <T> run(block: suspend () -> T): T }
fun interface Clock { fun now(): Long }
class BusinessException(message: String) : IllegalArgumentException(message)
fun ensure(condition: Boolean, message: String) { if (!condition) throw BusinessException(message) }
fun newId(): String = UUID.randomUUID().toString()
fun positiveQuantity(text: String): Int = text.toIntOrNull()?.takeIf { it > 0 }
    ?: throw BusinessException("La cantidad debe ser un entero mayor que cero.")
