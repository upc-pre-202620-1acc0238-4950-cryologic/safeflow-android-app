package com.safeflow.app.features.monitoring.application

import com.safeflow.app.core.domain.*
import com.safeflow.app.features.inventory.domain.ProductRepository
import com.safeflow.app.features.alerts.domain.AnomalySink
import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.logistics.domain.ShipmentRepository
import com.safeflow.app.features.logistics.domain.ShipmentStatus
import javax.inject.Inject

class MonitoringService @Inject constructor(val sensors: SensorRepository, val readings: ReadingRepository,
    val rejections: RejectionRepository, private val products: ProductRepository,
    private val shipments: ShipmentRepository, private val sink: AnomalySink,
    private val transaction: Transaction, private val clock: Clock) {
    // No production sensor adapter calls these ports yet. Instrumentation fixtures exercise them.
    suspend fun ingest(sensorId: String, raw: String, at: Long = clock.now()): Boolean = transaction.run {
        val sensor = sensors.find(sensorId) ?: throw BusinessException("El sensor no está registrado.")
        val product = products.find(sensor.productId) ?: throw BusinessException("El producto del sensor no existe.")
        val value = raw.toDoubleOrNull()
        if (value == null || !value.isFinite() || at > clock.now() || at <= 0) {
            val id = newId(); rejections.save(id, RejectedReading(id, sensorId, clock.now(), raw, "Medición o fecha inválida"))
            return@run false
        }
        val condition = ThermalRules.evaluate(value, product.minimum, product.maximum)
        sensors.save(sensorId, sensor.copy(connected = true))
        val shipment = shipments.all().filter { it.sensorId == sensorId && it.status != ShipmentStatus.DELIVERED }
            .maxByOrNull { it.createdAt }
        val id = newId(); readings.save(id, Reading(id, sensorId, at, value, condition, shipment?.takeIf { at >= it.createdAt }?.id))
        if (condition != ThermalCondition.NORMAL) sink.record(sensorId, product.id,
            shipment?.destination ?: sensor.location, "temperature", condition,
            "${product.name}: $value °C; rango ${product.minimum}–${product.maximum} °C.", at)
        true
    }
    suspend fun disconnect(sensorId: String) = transaction.run {
        val sensor = sensors.find(sensorId) ?: throw BusinessException("El sensor no está registrado.")
        sensors.save(sensorId, sensor.copy(connected = false))
        val destination = shipments.all().find { it.sensorId == sensorId && it.status != ShipmentStatus.DELIVERED }?.destination
        sink.record(sensorId, sensor.productId, destination ?: sensor.location, "connectivity",
            ThermalCondition.DISCONNECTED, "Sensor sin conexión. La última lectura no representa una temperatura actual.", clock.now())
    }
}
