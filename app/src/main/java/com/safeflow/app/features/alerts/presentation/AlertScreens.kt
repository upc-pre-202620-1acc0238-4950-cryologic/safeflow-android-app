package com.safeflow.app.features.alerts.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.safeflow.app.core.ui.components.*
import com.safeflow.app.core.ui.icons.SafeFlowSymbol
import com.safeflow.app.features.alerts.domain.Incident
import com.safeflow.app.features.inventory.domain.Product
import com.safeflow.app.features.monitoring.domain.Reading
import com.safeflow.app.features.monitoring.presentation.conditionTone

@Composable
fun AlertsScreen(incidents: List<Incident>, productName: (String) -> String, open: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("Activas") }
    SafeFlowText(if (filter == "Activas") "Incidencias pendientes" else "Historial de incidencias", variant = SafeFlowTextVariant.Headline)
    FilterChips(listOf("Activas", "Historial"), filter) { filter = it }
    val filtered = incidents.filter { if (filter == "Activas") it.active else !it.active }.sortedByDescending { it.openedAt }
    if (filtered.isEmpty()) SafeFlowCallout("Sin incidencias ${filter.lowercase()}", "Puedes consultar las incidencias atendidas desde Historial.", tone = SafeFlowTone.Muted)
    AdaptiveCards(filtered, { it.id }) { incident, modifier ->
        SafeFlowListCard("${incident.id.take(8)} · ${if (incident.type == "connectivity") "Desconexión del sensor" else "Anomalía térmica"}",
            "${productName(incident.productId)} · ${incident.location} · ${dateLabel(incident.openedAt)}", incident.severity.label,
            if (incident.active) "Activa · ${incident.repetitions} avisos agrupados" else "Resuelta", modifier,
            conditionTone(incident.severity), onClick = { open(incident.id) }, icon = SafeFlowSymbol.Warning)
    }
}
@Composable
fun IncidentScreen(incident: Incident?, product: Product?, reading: Reading?, canResolve: Boolean, busy: Boolean,
    resolve: (String) -> Unit, history: (() -> Unit)?) {
    if (incident == null) { SafeFlowCallout("Incidencia no disponible", "Regresa a la lista de alertas."); return }
    var action by rememberSaveable(incident.id) { mutableStateOf("") }
    SafeFlowListCard("${incident.id.take(8)} · ${if (incident.type == "connectivity") "Desconexión del sensor" else "Excursión térmica"}",
        "${product?.name ?: "Producto"} · ${incident.productId.take(8)} · ${incident.location}",
        if (incident.type == "connectivity") "Sensor desconectado" else reading?.let { "${it.temperature} °C" } ?: incident.severity.label,
        "${incident.severity.label} · ${if (incident.active) "Activa" else "Resuelta"} · ${dateLabel(incident.openedAt)}",
        Modifier.fillMaxWidth(), conditionTone(incident.severity), icon = SafeFlowSymbol.Warning)
    SafeFlowText("Sensor ${incident.sensorId} · Rango del producto: ${product?.minimum}–${product?.maximum} °C")
    SafeFlowCallout("Avisos de la misma anomalía", "${incident.repetitions} avisos agrupados. La anomalía conserva una única alerta activa.")
    if (incident.active && canResolve) {
        FormField("Acción correctiva", action, { action = it }, "incident-action", helper = "Describe la acción realizada antes de resolver.",
            error = if (action.isNotEmpty() && action.isBlank()) "Describe la acción correctiva antes de resolver." else null)
        SafeFlowButton("Resolver alerta", { resolve(action) }, Modifier.fillMaxWidth(), enabled = !busy && action.isNotBlank())
    }
    if (!incident.active) SafeFlowCallout("Acción correctiva registrada", "${incident.action}\n${incident.author} · ${dateLabel(incident.resolvedAt)}", tone = SafeFlowTone.Success)
    if (history != null) SafeFlowButton("Consultar historial térmico", history, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
}
