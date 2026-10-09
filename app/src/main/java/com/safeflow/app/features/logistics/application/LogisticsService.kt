package com.safeflow.app.features.logistics.application

import com.safeflow.app.core.domain.*
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.inventory.domain.*
import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.logistics.domain.*
import javax.inject.Inject

class LogisticsService @Inject constructor(val shipments: ShipmentRepository, private val products: ProductRepository,
    private val lots: LotRepository, private val sensors: SensorRepository, private val readings: ReadingRepository,
    private val access: Access, private val transaction: Transaction, private val clock: Clock) {
    suspend fun create(productId: String, lotId: String, quantity: String, destination: String, sensorId: String): String = transaction.run {
        val author = access.require(Permission.CREATE_SHIPMENT)
        ensure(destination.isNotBlank(), "Ingresa el destino del despacho.")
        val amount = positiveQuantity(quantity)
        val product = products.find(productId) ?: throw BusinessException("Selecciona un producto existente.")
        ensure(amount <= product.available, "La cantidad supera el stock disponible (${product.available}).")
        val lot = lots.find(lotId) ?: throw BusinessException("Selecciona un lote existente.")
        ensure(lot.productId == product.id, "El lote no pertenece al producto seleccionado.")
        val sensor = sensors.find(sensorId) ?: throw BusinessException("Selecciona un sensor registrado. La integración de sensores está pendiente.")
        ensure(sensor.productId == product.id, "El sensor no corresponde al producto.")
        ensure(shipments.all().none { it.sensorId == sensorId && it.status != ShipmentStatus.DELIVERED }, "El sensor está asociado a otro despacho activo.")
        val id = newId()
        products.save(product.id, product.copy(reserved = product.reserved + amount))
        shipments.save(id, Shipment(id, product.id, lot.id, amount, destination.trim(), sensorId, clock.now(), author.email))
        id
    }
    suspend fun depart(id: String, acceptRisk: Boolean) = transaction.run {
        val author = access.require(Permission.READ_SHIPMENTS)
        val shipment = shipments.find(id) ?: throw BusinessException("El despacho no existe.")
        ensure(shipment.status == ShipmentStatus.PREPARATION, "El despacho ya fue enviado.")
        val sensor = sensors.find(shipment.sensorId)
        ensure(sensor?.connected == true, "No se puede enviar: sensor desconectado o sin temperatura actual.")
        val reading = readings.all().filter { it.sensorId == shipment.sensorId }.maxByOrNull { it.at }
        ensure(reading != null && clock.now() - reading.at <= 300_000, "No hay una temperatura actual. La lectura debe tener menos de 5 minutos.")
        val product = products.find(shipment.productId) ?: throw BusinessException("El producto no existe.")
        val condition = ThermalRules.evaluate(reading!!.temperature, product.minimum, product.maximum)
        val risk = condition in setOf(ThermalCondition.RISK, ThermalCondition.CRITICAL)
        ensure(!risk || acceptRisk, "El producto presenta riesgo térmico. Confirma expresamente para continuar.")
        shipments.save(id, shipment.copy(status = ShipmentStatus.TRANSIT, departedAt = clock.now(), departedBy = author.email, riskAccepted = risk))
    }
    suspend fun deliver(id: String) = transaction.run {
        val author = access.require(Permission.DELIVER)
        val shipment = shipments.find(id) ?: throw BusinessException("El despacho no existe.")
        ensure(shipment.status != ShipmentStatus.DELIVERED, "El despacho ya fue entregado. Se conserva su fecha original.")
        ensure(shipment.status == ShipmentStatus.TRANSIT, "Solo se puede entregar un despacho en tránsito.")
        val product = products.find(shipment.productId) ?: throw BusinessException("El producto no existe.")
        ensure(product.reserved >= shipment.quantity && product.stock >= shipment.quantity, "El stock reservado no es consistente.")
        products.save(product.id, product.copy(stock = product.stock - shipment.quantity, reserved = product.reserved - shipment.quantity))
        shipments.save(id, shipment.copy(status = ShipmentStatus.DELIVERED, deliveredAt = clock.now(), deliveredBy = author.email))
    }
}
