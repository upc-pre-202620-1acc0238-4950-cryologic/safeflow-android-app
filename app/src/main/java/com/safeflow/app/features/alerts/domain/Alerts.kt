package com.safeflow.app.features.alerts.domain
import com.safeflow.app.core.domain.Records
import com.safeflow.app.features.monitoring.domain.ThermalCondition

data class Incident(val id: String, val sensorId: String, val productId: String,
    val location: String, val type: String, val severity: ThermalCondition, val description: String,
    val openedAt: Long, val lastSeenAt: Long, val repetitions: Int = 1,
    val action: String? = null, val author: String? = null, val resolvedAt: Long? = null) {
    val active: Boolean get() = resolvedAt == null
}
interface IncidentRepository : Records<Incident>
// Internal port for Monitoring. No presentation action can create an incident.
interface AnomalySink { suspend fun record(sensorId: String, productId: String, location: String,
    type: String, severity: ThermalCondition, description: String, at: Long) }
