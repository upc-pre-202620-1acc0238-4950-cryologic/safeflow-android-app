package com.safeflow.app.features.alerts.application

import com.safeflow.app.core.domain.*
import com.safeflow.app.features.alerts.domain.*
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.monitoring.domain.ThermalCondition
import javax.inject.Inject

class AlertService @Inject constructor(val incidents: IncidentRepository, private val access: Access,
    private val transaction: Transaction, private val clock: Clock) : AnomalySink {
    // Caller owns the transaction, making measurement and incident one atomic operation.
    override suspend fun record(sensorId: String, productId: String, location: String, type: String,
        severity: ThermalCondition, description: String, at: Long) {
        val existing = incidents.all().find { it.active && it.sensorId == sensorId && it.location == location && it.type == type }
        if (existing != null) {
            val priority = listOf(ThermalCondition.WARNING, ThermalCondition.RISK, ThermalCondition.CRITICAL, ThermalCondition.DISCONNECTED)
            incidents.save(existing.id, existing.copy(lastSeenAt = at, repetitions = existing.repetitions + 1,
                severity = if (priority.indexOf(severity) > priority.indexOf(existing.severity)) severity else existing.severity,
                description = description))
        } else {
            val id = newId(); incidents.save(id, Incident(id, sensorId, productId, location, type, severity, description, at, at))
        }
    }
    suspend fun resolve(id: String, action: String) = transaction.run {
        val author = access.require(Permission.RESOLVE)
        ensure(action.isNotBlank(), "Describe la acción correctiva para resolver la incidencia.")
        val incident = incidents.find(id) ?: throw BusinessException("La incidencia no existe.")
        ensure(incident.active, "La incidencia ya fue resuelta.")
        incidents.save(id, incident.copy(action = action.trim(), author = author.email, resolvedAt = clock.now()))
    }
}
