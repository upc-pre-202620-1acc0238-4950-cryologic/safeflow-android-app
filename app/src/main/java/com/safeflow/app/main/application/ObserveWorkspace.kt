package com.safeflow.app.main.application

import com.safeflow.app.core.domain.Clock
import com.safeflow.app.features.iam.application.IdentityService
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.inventory.domain.*
import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.alerts.domain.*
import com.safeflow.app.features.logistics.domain.*
import com.safeflow.app.features.reporting.application.ReportingService
import com.safeflow.app.features.reporting.domain.Summary
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class WorkspaceData(val now: Long, val account: Account?, val permissions: Set<Permission>,
    val products: List<Product>, val lots: List<Lot>, val sensors: List<Sensor>, val readings: List<Reading>,
    val rejections: List<RejectedReading>, val incidents: List<Incident>, val shipments: List<Shipment>,
    val roles: List<RolePermissions>, val summary: Summary?)
private data class InventoryData(val products: List<Product>, val lots: List<Lot>)
private data class MonitoringData(val sensors: List<Sensor>, val readings: List<Reading>, val rejections: List<RejectedReading>)
private data class OperationData(val incidents: List<Incident>, val shipments: List<Shipment>)

class ObserveWorkspace @Inject constructor(private val identity: IdentityService,
    private val products: ProductRepository, private val lots: LotRepository,
    private val sensors: SensorRepository, private val readings: ReadingRepository, private val rejections: RejectionRepository,
    private val incidents: IncidentRepository, private val shipments: ShipmentRepository,
    private val reporting: ReportingService, private val clock: Clock) {
    operator fun invoke(): Flow<WorkspaceData> {
        val inventory = combine(products.observe(), lots.observe(), ::InventoryData)
        val monitoring = combine(sensors.observe(), readings.observe(), rejections.observe(), ::MonitoringData)
        val operations = combine(incidents.observe(), shipments.observe(), ::OperationData)
        val ticker = flow { while (currentCoroutineContext().isActive) { emit(clock.now()); delay(1000) } }
        val access = combine(identity.sessions.sessions, identity.identities.observe(), identity.roles.observe(), ticker) { _, _, roles, _ -> roles }
        return combine(inventory, monitoring, operations, access) { inv, mon, ops, roles ->
            val account = identity.account()
            val permissions = identity.permissions()
            val canInventory = Permission.READ_INVENTORY in permissions || Permission.WRITE_INVENTORY in permissions
            val canShipments = Permission.READ_SHIPMENTS in permissions || Permission.CREATE_SHIPMENT in permissions || Permission.DELIVER in permissions
            val canMonitoring = canInventory || Permission.RISKS in permissions || canShipments || Permission.ALERTS in permissions || Permission.RESOLVE in permissions
            val canAlerts = Permission.ALERTS in permissions || Permission.RESOLVE in permissions
            WorkspaceData(clock.now(), account, permissions,
                if (canInventory || canMonitoring || Permission.CREATE_SHIPMENT in permissions) inv.products else emptyList(),
                if (canInventory || Permission.CREATE_SHIPMENT in permissions || Permission.READ_SHIPMENTS in permissions || Permission.DELIVER in permissions) inv.lots else emptyList(),
                if (canMonitoring || Permission.CREATE_SHIPMENT in permissions || Permission.DELIVER in permissions) mon.sensors else emptyList(),
                if (canMonitoring || Permission.DELIVER in permissions) mon.readings else emptyList(),
                if (canMonitoring) mon.rejections else emptyList(), if (canAlerts) ops.incidents else emptyList(),
                if (Permission.READ_SHIPMENTS in permissions || Permission.CREATE_SHIPMENT in permissions || Permission.DELIVER in permissions) ops.shipments else emptyList(),
                if (account?.role == Role.ADMIN) roles else emptyList(),
                if (Permission.SUMMARY in permissions) reporting.summary() else null)
        }
    }
}
