package com.safeflow.app.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeflow.app.core.domain.BusinessException
import com.safeflow.app.main.application.ObserveWorkspace
import com.safeflow.app.features.iam.application.IdentityService
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.inventory.application.InventoryService
import com.safeflow.app.features.inventory.domain.*
import com.safeflow.app.features.monitoring.application.MonitoringService
import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.alerts.application.AlertService
import com.safeflow.app.features.alerts.domain.Incident
import com.safeflow.app.features.logistics.application.LogisticsService
import com.safeflow.app.features.logistics.domain.Shipment
import com.safeflow.app.features.reporting.domain.Summary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.safeflow.app.features.logistics.presentation.ShipmentDraft

data class Destination(val name: String, val id: String = "")
data class WorkspaceState(val now: Long = 0, val loading: Boolean = true, val busy: Boolean = false,
    val account: Account? = null, val permissions: Set<Permission> = emptySet(),
    val destination: Destination = Destination("login"), val message: String? = null,
    val products: List<Product> = emptyList(), val lots: List<Lot> = emptyList(),
    val sensors: List<Sensor> = emptyList(), val readings: List<Reading> = emptyList(),
    val rejections: List<RejectedReading> = emptyList(), val incidents: List<Incident> = emptyList(),
    val shipments: List<Shipment> = emptyList(), val roles: List<RolePermissions> = emptyList(),
    val summary: Summary? = null, val shipmentDraft: ShipmentDraft = ShipmentDraft())
@HiltViewModel
class WorkspaceViewModel @Inject constructor(private val identity: IdentityService,
    private val inventory: InventoryService, private val monitoring: MonitoringService,
    private val alerts: AlertService, private val logistics: LogisticsService,
    private val workspace: ObserveWorkspace) : ViewModel() {
    private val mutable = MutableStateFlow(WorkspaceState())
    val state: StateFlow<WorkspaceState> = mutable.asStateFlow()
    private val history = mutableListOf<Destination>()
    init {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { identity.initialize() }
                workspace().collect { data ->
                    mutable.update { current ->
                        val changed = current.account?.id != data.account?.id
                        val requested = if (changed) Destination(home(data.account, data.permissions)) else current.destination
                        val destination = if (allowed(requested.name, data.account, data.permissions)) requested else Destination(home(data.account, data.permissions))
                        if (changed || destination != requested) history.clear()
                        current.copy(now = data.now, loading = false, account = data.account,
                            permissions = data.permissions, destination = destination, shipmentDraft = if (changed) ShipmentDraft() else current.shipmentDraft, products = data.products,
                            lots = data.lots, sensors = data.sensors, readings = data.readings,
                            rejections = data.rejections, incidents = data.incidents, shipments = data.shipments,
                            roles = data.roles, summary = data.summary,
                            message = if (changed && current.account != null && data.account == null) "Tu sesión terminó. Inicia sesión nuevamente." else current.message)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.update { it.copy(loading = false, message = "No se pudo abrir el almacenamiento local. Cierra y vuelve a abrir la aplicación.") } }
        }
    }
    private fun home(account: Account?, permissions: Set<Permission>) = when {
        account == null -> "login"
        account.role == Role.ADMIN -> "permissions"
        Permission.SUMMARY in permissions -> "summary"
        Permission.READ_INVENTORY in permissions || Permission.WRITE_INVENTORY in permissions -> "inventory"
        Permission.RISKS in permissions -> "risks"
        Permission.ALERTS in permissions || Permission.RESOLVE in permissions -> "alerts"
        Permission.READ_SHIPMENTS in permissions || Permission.CREATE_SHIPMENT in permissions || Permission.DELIVER in permissions -> "shipments"
        else -> "account"
    }
    private fun allowed(name: String, account: Account?, permissions: Set<Permission>): Boolean = when(name) {
        "login", "register", "verify", "recover" -> account == null
        "account" -> account != null
        "permissions" -> account?.role == Role.ADMIN
        "product-form", "lot-form", "income-form" -> Permission.WRITE_INVENTORY in permissions
        "inventory", "product", "lot", "lot-success" -> Permission.READ_INVENTORY in permissions || Permission.WRITE_INVENTORY in permissions
        "history" -> Permission.RISKS in permissions || Permission.READ_INVENTORY in permissions || Permission.WRITE_INVENTORY in permissions || Permission.READ_SHIPMENTS in permissions || Permission.DELIVER in permissions || Permission.CREATE_SHIPMENT in permissions
        "risks", "history-list" -> Permission.RISKS in permissions
        "alerts", "incident" -> Permission.ALERTS in permissions || Permission.RESOLVE in permissions
        "delivery" -> Permission.DELIVER in permissions
        "risk-confirm" -> Permission.READ_SHIPMENTS in permissions
        "shipment-form", "shipment-product", "shipment-lot", "shipment-sensor" -> Permission.CREATE_SHIPMENT in permissions
        "shipments", "shipment", "validation" -> Permission.READ_SHIPMENTS in permissions || Permission.DELIVER in permissions || Permission.CREATE_SHIPMENT in permissions
        "summary" -> Permission.SUMMARY in permissions
        else -> false
    }
    fun navigate(name: String, id: String = "", root: Boolean = false) {
        val current = mutable.value
        if (!allowed(name, current.account, current.permissions)) { message("No tienes acceso a esta operación."); return }
        if (current.destination == Destination(name, id)) return
        if (root) history.clear() else history.add(current.destination)
        mutable.update { it.copy(destination = Destination(name, id), message = null) }
    }
    fun back() {
        var previous = history.removeLastOrNull()
        while (previous != null && !allowed(previous.name, state.value.account, state.value.permissions)) previous = history.removeLastOrNull()
        val destination = previous ?: Destination(home(state.value.account, state.value.permissions))
        mutable.update { it.copy(destination = destination, message = null) }
    }
    fun message(value: String?) { mutable.update { it.copy(message = value) } }
    fun perform(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { block() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: BusinessException) { message(error.message) }
            catch (_: Exception) { message("No se pudo guardar la operación. Intenta nuevamente.") }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun login(email: String, password: String) = perform { identity.login(email, password) }
    fun register(email: String, password: String, terms: Boolean) = perform {
        identity.register(email, password, terms); withContext(Dispatchers.Main) { navigate("verify") }
    }
    fun recover(email: String) = perform { identity.recover(email); message("Solicitud registrada. El envío de correos está pendiente; tu contraseña no ha cambiado.") }
    fun logout() = perform { identity.logout() }
    fun permissions(role: Role, granted: Set<Permission>) = perform { identity.update(role, granted); message("Permisos guardados.") }
    fun product(name: String, type: String, min: String, max: String) = perform {
        val id = inventory.create(name, type, min, max); withTimeout(5_000) { state.first { data -> data.products.any { it.id == id } } }; withContext(Dispatchers.Main) { navigate("product", id) }; message("Producto registrado.")
    }
    fun lot(product: String, code: String) = perform { val id = inventory.addLot(product, code); withTimeout(5_000) { state.first { data -> data.lots.any { it.id == id } } }; withContext(Dispatchers.Main) { navigate("lot-success", id) } }
    fun income(product: String, quantity: String) = perform { inventory.income(product, quantity); withContext(Dispatchers.Main) { back() }; message("Ingreso registrado.") }
    fun shipmentDraft(draft: ShipmentDraft) { mutable.update { it.copy(shipmentDraft = draft, message = null) } }
    fun newShipment() { shipmentDraft(ShipmentDraft()); navigate("shipment-form") }
    fun selectShipment(value: String) {
        val draft = state.value.shipmentDraft
        shipmentDraft(when (state.value.destination.name) {
            "shipment-product" -> draft.copy(productId = value, lotId = "", sensorId = "")
            "shipment-lot" -> draft.copy(lotId = value)
            else -> draft.copy(sensorId = value)
        }); back()
    }
    fun submitShipment() { val draft = state.value.shipmentDraft; shipment(draft.productId, draft.lotId, draft.quantity, draft.destination, draft.sensorId) }
    fun shipment(product: String, lot: String, quantity: String, destination: String, sensor: String) = perform {
        val id = logistics.create(product, lot, quantity, destination, sensor)
        withTimeout(5_000) { state.first { data -> data.shipments.any { it.id == id } } }
        withContext(Dispatchers.Main) { navigate("shipment", id) }; message("Despacho registrado en preparación.")
    }
    fun depart(id: String, accepted: Boolean) = perform { logistics.depart(id, accepted); withContext(Dispatchers.Main) { navigate("shipment", id) }; message("Despacho en tránsito.") }
    fun deliver(id: String) = perform { logistics.deliver(id); withTimeout(5_000) { state.first { data -> data.shipments.any { it.id == id && it.status == com.safeflow.app.features.logistics.domain.ShipmentStatus.DELIVERED } } }; withContext(Dispatchers.Main) { navigate("shipment", id) }; message("Entrega confirmada.") }
    fun resolve(id: String, action: String) = perform { alerts.resolve(id, action); message("Incidencia resuelta. Se conserva su historial.") }
}
