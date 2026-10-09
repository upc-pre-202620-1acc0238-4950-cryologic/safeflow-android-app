package com.safeflow.app.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeflow.app.core.ui.components.*
import com.safeflow.app.core.ui.icons.SafeFlowSymbol
import com.safeflow.app.core.ui.theme.SafeFlowTheme
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.iam.presentation.*
import com.safeflow.app.features.inventory.presentation.*
import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.monitoring.presentation.*
import com.safeflow.app.features.alerts.presentation.*
import com.safeflow.app.features.logistics.presentation.*
import com.safeflow.app.features.reporting.presentation.*

private data class Operation(val id: String, val label: String, val icon: SafeFlowSymbol, val permission: Permission?)
private val operations = listOf(
    Operation("summary", "Resumen", SafeFlowSymbol.Summary, Permission.SUMMARY),
    Operation("inventory", "Inventario", SafeFlowSymbol.Inventory, Permission.READ_INVENTORY),
    Operation("risks", "Riesgos", SafeFlowSymbol.Warning, Permission.RISKS),
    Operation("alerts", "Alertas", SafeFlowSymbol.Notifications, Permission.ALERTS),
    Operation("shipments", "Despachos", SafeFlowSymbol.Shipment, Permission.READ_SHIPMENTS),
    Operation("permissions", "Permisos", SafeFlowSymbol.Permissions, null),
)
private fun available(state: WorkspaceState) = operations.filter {
    if (it.id == "permissions") state.account?.role == Role.ADMIN else it.permission in state.permissions ||
        (it.id == "inventory" && Permission.WRITE_INVENTORY in state.permissions) ||
        (it.id == "alerts" && Permission.RESOLVE in state.permissions) ||
        (it.id == "shipments" && (Permission.CREATE_SHIPMENT in state.permissions || Permission.DELIVER in state.permissions))
}
private val titles = mapOf("login" to "Iniciar sesión", "register" to "Crear cuenta", "verify" to "Verificación",
    "recover" to "Recuperar acceso", "account" to "Cuenta", "permissions" to "Permisos por rol",
    "inventory" to "Inventario", "product-form" to "Registrar producto", "product" to "Detalle de producto",
    "lot-form" to "Registrar lote", "lot" to "Lote", "lot-success" to "Lote",
    "shipment-product" to "Seleccionar producto del despacho", "shipment-lot" to "Seleccionar lote del despacho", "shipment-sensor" to "Seleccionar sensor del despacho", "income-form" to "Registrar ingreso", "risks" to "Productos en riesgo",
    "history" to "Historial térmico", "history-list" to "Historial de productos", "alerts" to "Alertas",
    "incident" to "Detalle de incidente", "shipments" to "Despachos", "shipment-form" to "Registrar despacho",
    "shipment" to "Detalle de despacho", "delivery" to "Despacho", "risk-confirm" to "Validación del despacho", "validation" to "Validación del despacho", "summary" to "Resumen")

@Composable
fun SafeFlowApp(viewModel: WorkspaceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val route = state.destination
    val permitted = available(state)
    val nav = permitted.take(3).map { SafeFlowNavigationItem(it.id, it.label, it.icon) } +
        SafeFlowNavigationItem("account", "Cuenta", SafeFlowSymbol.Account)
    val roots = permitted.map { it.id }.toSet() + setOf("account", "login")
    val navParent = when (route.name) {
        "product", "product-form", "lot", "lot-success", "lot-form", "income-form" -> "inventory"
        "shipment", "shipment-form", "shipment-product", "shipment-lot", "shipment-sensor", "validation", "delivery", "risk-confirm" -> "shipments"
        "incident" -> "alerts"
        "history", "history-list" -> if (state.account?.role == Role.SUPERVISOR) "risks" else if (state.shipments.any { it.id == route.id }) "shipments" else "inventory"
        else -> route.name
    }
    BackHandler(enabled = route.name !in roots || route.name == "account") { viewModel.back() }
    Surface(color = SafeFlowTheme.colors.background) {
        Scaffold(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().testTag("prototype-viewport"),
            containerColor = SafeFlowTheme.colors.background,
            topBar = { if (route.name !in setOf("login", "register", "recover", "verify")) SafeFlowAppHeader(titles[route.name] ?: "SafeFlow",
                onBackClick = if (route.name !in roots || route.name == "account") ({ viewModel.back() }) else null,
                onAccountClick = if (state.account != null && route.name != "account") ({ viewModel.navigate("account") }) else null) },
            bottomBar = {
                if (state.account != null) SafeFlowBottomNavigation(nav,
                    selectedId = navParent,
                    onItemClick = { if (route.name != it.id) viewModel.navigate(it.id, root = it.id != "account") })
            },
        ) { insets ->
            Column(Modifier.padding(insets).fillMaxSize()) {
                if (state.loading) {
                    SafeFlowCallout("Cargando SafeFlow", "Preparando tus datos locales."); return@Column
                }
                ScreenContent(route.name) {
                    state.message?.takeIf { route.name !in setOf("login", "register", "recover", "permissions", "shipment-form") }?.let {
                        SafeFlowCallout(if (it.endsWith("registrado.") || it.endsWith("confirmada.")) it.removeSuffix(".") else "Información de la operación", it, Modifier.testTag("result"))
                    }
                    key(route.name, route.id) {
                        val product = state.products.find { it.id == route.id }
                        val productSensors = state.sensors.filter { it.productId == route.id }
                        val sensorIds = productSensors.map { it.id }.toSet()
                        val productReadings = state.readings.filter { it.sensorId in sensorIds }
                        val shipment = state.shipments.find { it.id == route.id }
                        when(route.name) {
                            "login" -> LoginScreen(state.busy, viewModel::login, { viewModel.navigate("register") }, { viewModel.navigate("recover") }, state.message)
                            "register" -> RegisterScreen(state.busy, viewModel::register, { viewModel.navigate("login", root = true) }, state.message)
                            "verify" -> VerificationScreen { viewModel.navigate("login", root = true) }
                            "recover" -> RecoveryScreen(state.busy, viewModel::recover, { viewModel.navigate("login", root = true) }, state.message)
                            "account" -> state.account?.let { AccountScreen(it, state.permissions, viewModel::back, viewModel::logout, state.busy) }
                            "permissions" -> PermissionsScreen(state.roles, state.busy, viewModel::permissions, state.message)
                            "inventory" -> InventoryScreen(state.products,
                                { item -> currentCondition(state.sensors.filter { it.productId == item.id }, state.readings).label },
                                { item -> val ids = state.sensors.filter { it.productId == item.id }.map { it.id }.toSet(); state.readings.filter { it.sensorId in ids }.maxByOrNull { it.at }?.let { "${it.temperature} °C" } ?: "Sin temperatura" },
                                Permission.WRITE_INVENTORY in state.permissions, { viewModel.navigate("product", it) }, { viewModel.navigate("product-form") })
                            "product-form" -> ProductForm(state.busy, viewModel::product, viewModel::back)
                            "product" -> {
                                val latest = productReadings.maxByOrNull { it.at }
                                ProductScreen(product, state.lots.filter { it.productId == route.id }, Permission.WRITE_INVENTORY in state.permissions,
                                    { viewModel.navigate("lot-form", route.id) }, { viewModel.navigate("income-form", route.id) }, { viewModel.navigate("lot", it) },
                                    latest?.let { "${it.temperature} °C" } ?: "Sin temperatura",
                                    "${productSensors.firstOrNull()?.let { "Sensor ${it.id} · " }.orEmpty()}${currentCondition(productSensors, productReadings).label} · ${dateLabel(latest?.at)}",
                                    { viewModel.navigate("history", route.id) }, productSensors.isNotEmpty())
                            }
                            "lot-form" -> LotForm(product, state.busy, { viewModel.lot(route.id, it) }, state.lots.filter { it.productId == route.id },
                                { viewModel.navigate("lot", it) }, viewModel::back)
                            "lot", "lot-success" -> {
                                val lot = state.lots.find { it.id == route.id }
                                val associated = state.products.find { it.id == lot?.productId }
                                LotScreen(associated, lot, route.name == "lot-success", { viewModel.navigate("income-form", lot?.productId.orEmpty()) },
                                    { viewModel.navigate("product", lot?.productId.orEmpty(), root = true) })
                            }
                            "income-form" -> IncomeForm(product, state.busy, { viewModel.income(route.id, it) }, viewModel::back)
                            "history-list" -> {
                                if (state.products.isEmpty()) SafeFlowCallout("Sin productos", "Todavía no hay productos con historial térmico.", tone = SafeFlowTone.Muted)
                                state.products.forEach { item -> SafeFlowButton("${item.name} · ${currentCondition(state.sensors.filter { it.productId == item.id }, state.readings).label}", { viewModel.navigate("history", item.id) }, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined) }
                            }
                            "history" -> {
                                val currentShipment = state.shipments.find { it.id == route.id }
                                val targetReadings = if (currentShipment != null) state.readings.filter { it.shipmentId == currentShipment.id } else productReadings
                                HistoryScreen(product?.name ?: currentShipment?.destination ?: "Temperaturas", targetReadings,
                                    state.rejections.filter { it.sensorId in sensorIds || it.sensorId == currentShipment?.sensorId }, state.now,
                                    (product ?: state.products.find { it.id == currentShipment?.productId })?.minimum,
                                    (product ?: state.products.find { it.id == currentShipment?.productId })?.maximum)
                                val relevant = state.incidents.filter { it.productId == route.id }
                                if (relevant.isNotEmpty()) {
                                    SafeFlowText("Historial de anomalías", variant = SafeFlowTextVariant.Title)
                                    relevant.forEach { incident -> SafeFlowListCard(incident.severity.label, dateLabel(incident.openedAt), incident.description,
                                        if (incident.active) "Activa" else "Resuelta", Modifier.fillMaxWidth(), conditionTone(incident.severity), onClick = { viewModel.navigate("incident", incident.id) }) }
                                }
                            }
                            "risks" -> {
                                val riskItems = state.products.mapNotNull { item ->
                                    val ids = state.sensors.filter { it.productId == item.id }.map { it.id }.toSet()
                                    val latest = state.readings.filter { it.sensorId in ids }.groupBy { it.sensorId }.values.mapNotNull { it.maxByOrNull { r -> r.at } }
                                    val risk = latest.filter { it.condition in setOf(ThermalCondition.WARNING, ThermalCondition.RISK, ThermalCondition.CRITICAL) }
                                        .maxByOrNull { if (it.condition == ThermalCondition.CRITICAL) 3 else if (it.condition == ThermalCondition.RISK) 2 else 1 }
                                    risk?.let { reading -> RiskItem(item.id, item.name, state.sensors.find { it.id == reading.sensorId }?.location.orEmpty(), item.minimum, item.maximum, reading) }
                                }
                                RiskScreen(riskItems, { viewModel.navigate("history", it) }, { viewModel.navigate("history-list") })
                            }
                            "alerts" -> AlertsScreen(state.incidents, { id -> state.products.find { it.id == id }?.name ?: "Producto ${id.take(8)}" }) { viewModel.navigate("incident", it) }
                            "incident" -> {
                                val incident = state.incidents.find { it.id == route.id }
                                IncidentScreen(incident, state.products.find { it.id == incident?.productId },
                                    state.readings.filter { it.sensorId == incident?.sensorId && it.at <= (incident.lastSeenAt) }.maxByOrNull { it.at },
                                    Permission.RESOLVE in state.permissions, state.busy, { viewModel.resolve(route.id, it) },
                                    if (Permission.RISKS in state.permissions) ({ viewModel.navigate("history", incident?.productId.orEmpty()) }) else null)
                            }
                            "shipments" -> ShipmentsScreen(state.shipments, { id -> state.products.find { it.id == id }?.name ?: "Producto" },
                                { id -> state.readings.filter { it.sensorId == id }.maxByOrNull { it.at }?.let { "${it.temperature} °C" } ?: "Sin lecturas" },
                                Permission.CREATE_SHIPMENT in state.permissions, viewModel::newShipment, { viewModel.navigate("shipment", it) })
                            "shipment-form" -> ShipmentForm(state.products, state.lots, state.sensors, state.busy, state.shipmentDraft, viewModel::shipmentDraft,
                                { viewModel.navigate(it) }, viewModel::submitShipment, viewModel::back, state.message)
                            "shipment-product", "shipment-lot", "shipment-sensor" -> ShipmentSelection(route.name, state.products, state.lots, state.sensors,
                                state.readings, state.shipments, state.shipmentDraft, viewModel::selectShipment, viewModel::back)
                            "shipment" -> ShipmentScreen(shipment, state.products.find { it.id == shipment?.productId }, state.lots.find { it.id == shipment?.lotId },
                                state.sensors.find { it.id == shipment?.sensorId }, state.readings.filter { it.shipmentId == shipment?.id },
                                Permission.READ_SHIPMENTS in state.permissions, Permission.DELIVER in state.permissions, state.busy,
                                { viewModel.navigate("validation", route.id) }, { viewModel.navigate("delivery", route.id) }, { viewModel.navigate("history", route.id) })
                            "validation" -> ValidationScreen(shipment, state.products.find { it.id == shipment?.productId }, state.sensors.find { it.id == shipment?.sensorId },
                                state.readings.filter { it.sensorId == shipment?.sensorId }.maxByOrNull { it.at }, state.now, state.busy, { viewModel.depart(route.id, it) }, { viewModel.navigate("risk-confirm", route.id) }, { viewModel.navigate("shipments", root = true) })
                            "delivery" -> DeliveryConfirmation(shipment, state.products.find { it.id == shipment?.productId }, state.busy,
                                { viewModel.deliver(route.id) }, viewModel::back)
                            "risk-confirm" -> RiskConfirmation(shipment, state.products.find { it.id == shipment?.productId },
                                state.readings.filter { it.sensorId == shipment?.sensorId }.maxByOrNull { it.at }, state.busy,
                                { viewModel.depart(route.id, true) }, viewModel::back)
                            "summary" -> {
                                SafeFlowText("Situación operativa", variant = SafeFlowTextVariant.Headline)
                                if (Permission.ALERTS in state.permissions) state.incidents.filter { it.active }.maxByOrNull { it.openedAt }?.let { incident ->
                                    SafeFlowCallout("Aviso de anomalía térmica", "${incident.description} Consulta la incidencia para evaluar su atención.",
                                        modifier = Modifier.fillMaxWidth().testTag("alert-notice"), tone = conditionTone(incident.severity), onClick = { viewModel.navigate("incident", incident.id) })
                                }
                                SummaryScreen(state.summary, if (Permission.RISKS in state.permissions) ({ viewModel.navigate("history-list") }) else null)
                            }
                        }
                    }
                }
            }
        }
    }
}
