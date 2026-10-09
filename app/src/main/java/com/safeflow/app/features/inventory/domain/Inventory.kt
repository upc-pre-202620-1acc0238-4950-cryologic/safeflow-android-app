package com.safeflow.app.features.inventory.domain
import com.safeflow.app.core.domain.Records

data class Product(val id: String, val name: String, val type: String?,  val minimum: Double,
    val maximum: Double, val stock: Int = 0, val reserved: Int = 0) {
    val available: Int get() = stock - reserved
}
data class Lot(val id: String, val productId: String, val code: String)
data class Income(val id: String, val productId: String, val quantity: Int, val author: String, val at: Long)
interface ProductRepository : Records<Product>
interface LotRepository : Records<Lot>
interface IncomeRepository : Records<Income>
