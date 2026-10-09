package com.safeflow.app.features.inventory.application

import com.safeflow.app.core.domain.*
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.inventory.domain.*
import javax.inject.Inject

class InventoryService @Inject constructor(val products: ProductRepository, val lots: LotRepository,
    private val incomes: IncomeRepository, private val access: Access,
    private val transaction: Transaction, private val clock: Clock) {
    suspend fun create(name: String, type: String, minimum: String, maximum: String): String = transaction.run {
        access.require(Permission.WRITE_INVENTORY)
        ensure(name.isNotBlank(), "Completa el nombre del producto.")
        val min = minimum.toDoubleOrNull(); val max = maximum.toDoubleOrNull()
        ensure(min != null && max != null && min.isFinite() && max.isFinite(), "Ingresa temperaturas numéricas válidas.")
        ensure(min!! <= max!!, "La temperatura mínima no puede superar a la máxima.")
        val id = newId(); products.save(id, Product(id, name.trim(), type.trim().ifEmpty { null }, min, max)); id
    }
    suspend fun addLot(productId: String, code: String): String = transaction.run {
        access.require(Permission.WRITE_INVENTORY)
        ensure(products.find(productId) != null, "Selecciona un producto existente.")
        ensure(code.isNotBlank(), "Ingresa el código del lote.")
        val duplicate = lots.all().find { it.productId == productId && it.code.equals(code.trim(), true) }
        ensure(duplicate == null, "El lote ${code.trim()} ya existe. Consulta el lote registrado.")
        val id = newId(); lots.save(id, Lot(id, productId, code.trim())); id
    }
    suspend fun income(productId: String, quantity: String) = transaction.run {
        val author = access.require(Permission.WRITE_INVENTORY)
        val amount = positiveQuantity(quantity)
        val product = products.find(productId) ?: throw BusinessException("Selecciona un producto existente.")
        ensure(product.stock.toLong() + amount <= Int.MAX_VALUE, "La cantidad supera el límite admitido.")
        products.save(productId, product.copy(stock = product.stock + amount))
        val id = newId(); incomes.save(id, Income(id, productId, amount, author.email, clock.now()))
    }
}
