package com.safeflow.app.features.logistics.domain
import com.safeflow.app.core.domain.Records

enum class ShipmentStatus(val label: String) { PREPARATION("En preparación"), TRANSIT("En tránsito"), DELIVERED("Entregado") }
data class Shipment(val id: String, val productId: String, val lotId: String, val quantity: Int,
    val destination: String, val sensorId: String, val createdAt: Long, val createdBy: String,
    val status: ShipmentStatus = ShipmentStatus.PREPARATION, val departedAt: Long? = null,
    val departedBy: String? = null, val riskAccepted: Boolean = false,
    val deliveredAt: Long? = null, val deliveredBy: String? = null)
interface ShipmentRepository : Records<Shipment>
