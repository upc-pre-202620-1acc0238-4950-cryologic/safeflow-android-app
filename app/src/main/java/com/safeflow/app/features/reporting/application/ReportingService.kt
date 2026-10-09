package com.safeflow.app.features.reporting.application

import com.safeflow.app.features.reporting.domain.Summary
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.inventory.domain.ProductRepository
import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.alerts.domain.IncidentRepository
import com.safeflow.app.features.logistics.domain.*
import javax.inject.Inject

class ReportingService @Inject constructor(private val access: Access, private val products: ProductRepository,
    private val sensors: SensorRepository, private val readings: ReadingRepository,
    private val incidents: IncidentRepository, private val shipments: ShipmentRepository) {
    suspend fun summary(): Summary {
        access.require(Permission.SUMMARY)
        val inventory = products.all()
        val latest = readings.all().groupBy { it.sensorId }.values.mapNotNull { values -> values.maxByOrNull { it.at } }
        val attentionIds = latest.filter { it.condition in setOf(ThermalCondition.WARNING, ThermalCondition.RISK, ThermalCondition.CRITICAL) }
            .mapNotNull { reading -> sensors.find(reading.sensorId)?.productId }.toSet()
        val active = incidents.all().filter { it.active }
        val dispatches = shipments.all()
        // SUMMARY authorizes consolidated indicators; it never grants detailed operations.
        return Summary(inventory.size, inventory.sumOf { it.stock }, attentionIds.size, active.size,
            dispatches.count { it.status == ShipmentStatus.TRANSIT },
            active.count { it.severity == ThermalCondition.CRITICAL },
            active.count { it.severity != ThermalCondition.CRITICAL }, dispatches.size,
            dispatches.count { it.status == ShipmentStatus.PREPARATION },
            dispatches.count { it.status == ShipmentStatus.DELIVERED })
    }
}
