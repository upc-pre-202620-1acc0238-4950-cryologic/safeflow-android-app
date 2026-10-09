package com.safeflow.app.features.logistics.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import com.safeflow.app.core.ui.components.*
import com.safeflow.app.core.ui.icons.SafeFlowSymbol
import androidx.compose.ui.platform.testTag
import com.safeflow.app.features.logistics.domain.*
import com.safeflow.app.features.inventory.domain.*
import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.monitoring.presentation.*

@Composable
fun ShipmentsScreen(shipments: List<Shipment>, productName: (String) -> String, temperature: (String) -> String,
    canCreate: Boolean, create: () -> Unit, open: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("Todos") }
    SafeFlowText("Seguimiento de envíos", variant = SafeFlowTextVariant.Headline)
    FilterChips(listOf("Todos", "En tránsito", "Entregados"), filter, width = 130.dp) { filter = it }
    if (canCreate) SafeFlowButton("Registrar despacho", create, Modifier.fillMaxWidth())
    val filtered = shipments.filter { filter == "Todos" || it.status.label == filter || (filter == "Entregados" && it.status == ShipmentStatus.DELIVERED) }.sortedByDescending { it.createdAt }
    if (filtered.isEmpty()) SafeFlowCallout("Sin despachos", "No hay despachos registrados para este estado.", tone = SafeFlowTone.Muted)
    AdaptiveCards(filtered, { it.id }) { shipment, modifier ->
        SafeFlowListCard("Despacho ${shipment.id.take(8)}", "${productName(shipment.productId)} · ${shipment.destination}",
            temperature(shipment.sensorId), shipment.status.label, modifier, onClick = { open(shipment.id) }, icon = SafeFlowSymbol.Shipment)
    }
}
data class ShipmentDraft(val productId: String = "", val lotId: String = "", val sensorId: String = "",
    val quantity: String = "", val destination: String = "")
@Composable
fun ShipmentForm(products: List<Product>, lots: List<Lot>, sensors: List<Sensor>, busy: Boolean,
    draft: ShipmentDraft, change: (ShipmentDraft) -> Unit, select: (String) -> Unit,
    save: () -> Unit, cancel: () -> Unit, error: String? = null) {
    val product = products.find { it.id == draft.productId }
    val lot = lots.find { it.id == draft.lotId }
    val sensor = sensors.find { it.id == draft.sensorId }
    val amount = draft.quantity.toIntOrNull()
    val quantityError = when {
        draft.quantity.isEmpty() -> null
        amount == null || amount <= 0 -> "Ingresa un entero mayor que cero."
        product != null && amount > product.available -> "La cantidad supera el stock disponible (${product.available})."
        else -> error?.takeIf { it.contains("stock", true) || it.contains("cantidad", true) }
    }
    SafeFlowText("Identifica la carga y el destino", variant = SafeFlowTextVariant.Headline)
    SafeFlowButton(product?.name ?: "Seleccionar producto", { select("shipment-product") }, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, !busy)
    if (product != null) SafeFlowText("${product.available} unidades disponibles", variant = SafeFlowTextVariant.Caption)
    SafeFlowText("Lote", variant = SafeFlowTextVariant.Label)
    SafeFlowButton(lot?.code ?: "Seleccionar lote", { select("shipment-lot") }, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, !busy && product != null)
    if (product == null) SafeFlowText("Selecciona primero un producto.", variant = SafeFlowTextVariant.Caption)
    FormField("Cantidad de unidades", draft.quantity, { change(draft.copy(quantity = it)) }, "shipment-quantity", numeric = true,
        helper = "Ingresa una cantidad positiva.", error = quantityError)
    FormField("Destino", draft.destination, { change(draft.copy(destination = it)) }, "shipment-destination", helper = "Requerido")
    SafeFlowButton(sensor?.id ?: "Seleccionar sensor", { select("shipment-sensor") }, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, !busy && product != null)
    if (error != null && quantityError == null) SafeFlowCallout("No se pudo registrar", error, tone = SafeFlowTone.Warning)
    SafeFlowButton("Registrar despacho", save, Modifier.fillMaxWidth().testTag("shipment-submit"),
        enabled = !busy && product != null && lot != null && sensor != null && draft.destination.isNotBlank() && amount != null && amount > 0 && quantityError == null)
    SafeFlowButton("Cancelar", cancel, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, !busy)
}
@Composable
fun ShipmentSelection(kind: String, products: List<Product>, lots: List<Lot>, sensors: List<Sensor>, readings: List<Reading>,
    shipments: List<Shipment>, draft: ShipmentDraft, choose: (String) -> Unit, cancel: () -> Unit) {
    val product = products.find { it.id == draft.productId }
    when (kind) {
        "shipment-product" -> {
            SafeFlowText("Productos registrados", variant = SafeFlowTextVariant.Headline)
            if (products.isEmpty()) SafeFlowCallout("Sin productos", "Registra un producto antes de crear un despacho.", tone = SafeFlowTone.Muted)
            products.forEach { item -> SafeFlowListCard(item.name, "Producto ${item.id.take(8)} · ${item.available} unidades disponibles",
                "${item.minimum}–${item.maximum} °C", "Rango de conservación", Modifier.fillMaxWidth(), onClick = { choose(item.id) }, icon = SafeFlowSymbol.Inventory) }
        }
        "shipment-lot" -> {
            SafeFlowText("${product?.name ?: "Producto"} · ${product?.id?.take(8).orEmpty()}", variant = SafeFlowTextVariant.Headline)
            val available = lots.filter { it.productId == draft.productId }
            if (available.isEmpty()) SafeFlowCallout("Sin lotes", "Selecciona un producto con lotes registrados.", tone = SafeFlowTone.Muted)
            available.forEach { item -> SafeFlowListCard("Lote ${item.code}", "Vinculado al producto ${item.productId.take(8)}",
                "${product?.available ?: 0} unidades", "Stock del producto asociado", Modifier.fillMaxWidth(), onClick = { choose(item.id) }, icon = SafeFlowSymbol.Inventory) }
        }
        "shipment-sensor" -> {
            SafeFlowText("Sensores disponibles", variant = SafeFlowTextVariant.Headline)
            val available = sensors.filter { sensor -> sensor.productId == draft.productId && shipments.none { it.sensorId == sensor.id && it.status != ShipmentStatus.DELIVERED } }
            if (available.isEmpty()) SafeFlowCallout("Sin sensores disponibles", "No hay sensores registrados. La integración de sensores está pendiente; no se puede completar el despacho.", tone = SafeFlowTone.Muted)
            available.forEach { item -> val latest = readings.filter { it.sensorId == item.id }.maxByOrNull { it.at }
                SafeFlowListCard("Sensor ${item.id}", item.location, if (item.connected) "Disponible" else "Desconectado",
                    latest?.let { "Última lectura: ${it.temperature} °C · ${dateLabel(it.at)}" } ?: "Sin lecturas", Modifier.fillMaxWidth(),
                    onClick = { choose(item.id) }, icon = SafeFlowSymbol.Temperature)
            }
        }
    }
    SafeFlowButton("Cancelar", cancel, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
}
@Composable
fun ShipmentScreen(shipment: Shipment?, product: Product?, lot: Lot?, sensor: Sensor?, readings: List<Reading>,
    canValidate: Boolean, canDeliver: Boolean, busy: Boolean, validate: () -> Unit, deliver: () -> Unit, history: () -> Unit) {
    if (shipment == null) { SafeFlowCallout("Despacho no disponible", "Regresa a la lista de despachos."); return }
    SafeFlowListCard("Despacho ${shipment.id.take(8)}", "${product?.name ?: "Producto"} · Lote ${lot?.code ?: "—"}",
        shipment.status.label, "Sensor ${sensor?.id ?: "—"} · ${if (sensor?.connected == true) "Disponible" else "Sin conexión"}", Modifier.fillMaxWidth(), icon = SafeFlowSymbol.Shipment)
    SafeFlowText("Destino: ${shipment.destination}")
    SafeFlowText("Registrado: ${dateLabel(shipment.createdAt)} · ${shipment.createdBy}")
    SafeFlowText("Salida: ${dateLabel(shipment.departedAt)}")
    if (shipment.deliveredAt != null) SafeFlowCallout("Entrega confirmada", "${dateLabel(shipment.deliveredAt)} · ${shipment.deliveredBy}", tone = SafeFlowTone.Success)
    if (shipment.riskAccepted) SafeFlowCallout("Riesgo confirmado antes del envío", "${shipment.departedBy} · ${dateLabel(shipment.departedAt)}", tone = SafeFlowTone.Warning)
    val latest = readings.maxByOrNull { it.at }
    SafeFlowListCard("Última temperatura", "Rango del producto: ${product?.minimum}–${product?.maximum} °C",
        latest?.let { "${it.temperature} °C" } ?: "Sin temperatura", "${dateLabel(latest?.at)} · Sensor ${sensor?.id ?: "—"}", Modifier.fillMaxWidth(), icon = SafeFlowSymbol.Inventory)
    if (shipment.status == ShipmentStatus.PREPARATION && canValidate) SafeFlowButton("Validar temperatura", validate, Modifier.fillMaxWidth(), enabled = !busy)
    if (shipment.status == ShipmentStatus.TRANSIT && canDeliver) SafeFlowButton("Confirmar entrega", deliver, Modifier.fillMaxWidth(), enabled = !busy)
    if (readings.isNotEmpty()) {
        ThermalChart(readings.sortedBy { it.at }, product?.minimum, product?.maximum)
        ReadingList(readings.sortedBy { it.at })
    } else SafeFlowCallout("Sin lecturas", "Las mediciones de este despacho aparecerán en orden cronológico.", tone = SafeFlowTone.Muted)
    SafeFlowButton("Consultar historial térmico", history, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
}
@Composable
fun ValidationScreen(shipment: Shipment?, product: Product?, sensor: Sensor?, reading: Reading?, now: Long, busy: Boolean, depart: (Boolean) -> Unit, confirmRisk: () -> Unit, back: () -> Unit) {
    val available = sensor?.connected == true && reading != null && now - reading.at <= 300_000
    val condition = if (available && product != null) ThermalRules.evaluate(reading.temperature, product.minimum, product.maximum) else ThermalCondition.UNKNOWN
    val risk = condition == ThermalCondition.RISK || condition == ThermalCondition.CRITICAL
    SafeFlowListCard("Despacho ${shipment?.id?.take(8).orEmpty()}", "${product?.name ?: "Producto"} · Rango del producto: ${product?.minimum}–${product?.maximum} °C", reading?.let { "${it.temperature} °C" } ?: "Sin temperatura actual",
        if (available) "Sensor ${sensor?.id} · ${condition.label} · ${dateLabel(reading?.at)}" else "Sin temperatura vigente", Modifier.fillMaxWidth(), conditionTone(condition), icon = SafeFlowSymbol.Shipment)
    if (available && !risk) SafeFlowCallout("Condición térmica dentro del rango", "Las mediciones disponibles permiten continuar con el envío.")
    if (!available) SafeFlowCallout("No se puede continuar", "Se necesita un sensor conectado con una lectura válida de los últimos 5 minutos.", tone = SafeFlowTone.Warning)
    if (risk) {
        SafeFlowCallout("Se requiere confirmación autorizada", "El envío está en condición de riesgo. Revisa su condición antes de confirmar la continuación.", tone = SafeFlowTone.Warning)
        SafeFlowButton("Revisar y confirmar", confirmRisk, Modifier.fillMaxWidth(), enabled = !busy && available)
    } else SafeFlowButton("Continuar con el despacho", { depart(false) }, Modifier.fillMaxWidth(), enabled = !busy && available)
    SafeFlowButton("Volver a despachos", back, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
}

@Composable
fun DeliveryConfirmation(shipment: Shipment?, product: Product?, busy: Boolean, confirm: () -> Unit, cancel: () -> Unit) {
    SafeFlowText("¿Confirmar la entrega?", variant = SafeFlowTextVariant.Headline)
    SafeFlowListCard("Despacho ${shipment?.id?.take(8).orEmpty()}", "${product?.name ?: "Producto"} · Destino: ${shipment?.destination.orEmpty()}",
        shipment?.status?.label ?: "No disponible", "Se registrará la fecha y hora de la confirmación.", Modifier.fillMaxWidth(), icon = SafeFlowSymbol.Shipment)
    SafeFlowButton("Confirmar entrega", confirm, Modifier.fillMaxWidth(), enabled = !busy && shipment?.status == ShipmentStatus.TRANSIT)
    SafeFlowButton("Cancelar", cancel, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, !busy)
}
@Composable
fun RiskConfirmation(shipment: Shipment?, product: Product?, reading: Reading?, busy: Boolean, confirm: () -> Unit, cancel: () -> Unit) {
    SafeFlowText("¿Continuar con el envío en riesgo?", variant = SafeFlowTextVariant.Headline)
    SafeFlowCallout("Confirmación requerida", "Tu sesión tiene permiso para confirmar esta continuación. Revisa la condición térmica antes de decidir.", tone = SafeFlowTone.Warning)
    SafeFlowListCard("Despacho ${shipment?.id?.take(8).orEmpty()}", "${product?.name ?: "Producto"} · Rango: ${product?.minimum}–${product?.maximum} °C",
        reading?.let { "${it.temperature} °C" } ?: "Sin temperatura", "${reading?.condition?.label ?: "Sin lecturas"} · ${dateLabel(reading?.at)}", Modifier.fillMaxWidth(), icon = SafeFlowSymbol.Shipment)
    SafeFlowButton("Confirmar continuación", confirm, Modifier.fillMaxWidth(), enabled = !busy && shipment?.status == ShipmentStatus.PREPARATION && reading != null)
    SafeFlowButton("Cancelar", cancel, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, !busy)
}
