package com.safeflow.app.features.reporting.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.safeflow.app.core.ui.components.*
import com.safeflow.app.core.ui.icons.SafeFlowSymbol
import com.safeflow.app.features.reporting.domain.Summary

@Composable
fun SummaryScreen(summary: Summary?, history: (() -> Unit)?) {
    if (summary == null) return
    data class Indicator(val title: String, val detail: String, val value: String, val status: String, val icon: SafeFlowSymbol, val tone: SafeFlowTone = SafeFlowTone.Normal)
    val indicators = listOf(
        Indicator("Productos registrados", "Información autorizada para SUPERVISOR", "${summary.products} productos", "${summary.risks} requieren atención", SafeFlowSymbol.Inventory),
        Indicator("Alertas activas", "Una alerta por anomalía; repeticiones agrupadas", "${summary.activeIncidents} alertas", "${summary.critical} críticas · ${summary.warnings} advertencias", SafeFlowSymbol.Inventory, if (summary.activeIncidents > 0) SafeFlowTone.Warning else SafeFlowTone.Normal),
        Indicator("Despachos", "Información operativa autorizada", "${summary.shipments} despachos", "${summary.preparation} en preparación · ${summary.inTransit} en tránsito · ${summary.delivered} entregados", SafeFlowSymbol.Shipment))
    AdaptiveCards(indicators, { it.title }) { item, modifier -> SafeFlowListCard(item.title, item.detail, item.value, item.status, modifier, tone = item.tone, icon = item.icon) }
    if (summary.products == 0 && summary.activeIncidents == 0 && summary.shipments == 0)
        SafeFlowCallout("Sin actividad registrada", "Los indicadores se actualizarán al registrar productos, mediciones y despachos.", tone = SafeFlowTone.Muted)
    if (history != null) SafeFlowButton("Consultar historial térmico", history, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
}
