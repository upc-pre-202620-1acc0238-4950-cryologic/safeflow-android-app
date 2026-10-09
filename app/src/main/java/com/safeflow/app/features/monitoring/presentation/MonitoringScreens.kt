package com.safeflow.app.features.monitoring.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.safeflow.app.core.ui.components.*
import com.safeflow.app.core.ui.icons.SafeFlowSymbol
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import com.safeflow.app.core.ui.theme.SafeFlowRadius
import com.safeflow.app.core.ui.theme.SafeFlowSpacing
import com.safeflow.app.core.ui.theme.SafeFlowTheme
import com.safeflow.app.features.monitoring.domain.*

fun currentCondition(sensors: List<Sensor>, readings: List<Reading>): ThermalCondition {
    if (sensors.isEmpty()) return ThermalCondition.UNKNOWN
    if (sensors.any { !it.connected }) return ThermalCondition.DISCONNECTED
    val ranking = listOf(ThermalCondition.UNKNOWN, ThermalCondition.NORMAL, ThermalCondition.WARNING, ThermalCondition.RISK, ThermalCondition.CRITICAL)
    return sensors.map { sensor -> readings.filter { it.sensorId == sensor.id }.maxByOrNull { it.at }?.condition ?: ThermalCondition.UNKNOWN }
        .maxByOrNull { ranking.indexOf(it) } ?: ThermalCondition.UNKNOWN
}
fun conditionTone(condition: ThermalCondition) = when(condition) {
    ThermalCondition.CRITICAL -> SafeFlowTone.Critical
    ThermalCondition.RISK, ThermalCondition.WARNING, ThermalCondition.DISCONNECTED -> SafeFlowTone.Warning
    ThermalCondition.UNKNOWN -> SafeFlowTone.Muted
    else -> SafeFlowTone.Success
}
@Composable
fun ThermalCard(sensors: List<Sensor>, readings: List<Reading>, minimum: Double, maximum: Double, history: () -> Unit) {
    SafeFlowText("Supervisión térmica", variant = SafeFlowTextVariant.Title)
    if (sensors.isEmpty()) SafeFlowCallout("Sin sensor vinculado", "No hay lecturas de temperatura para este producto.", tone = SafeFlowTone.Muted)
    sensors.forEach { sensor ->
        val reading = readings.filter { it.sensorId == sensor.id }.maxByOrNull { it.at }
        val condition = if (!sensor.connected) ThermalCondition.DISCONNECTED else reading?.condition ?: ThermalCondition.UNKNOWN
        SafeFlowListCard(sensor.location, "Sensor ${sensor.id} · Rango $minimum–$maximum °C",
            reading?.let { "${it.temperature} °C" } ?: "Sin temperatura", "${condition.label} · ${dateLabel(reading?.at)}",
            Modifier.fillMaxWidth(), conditionTone(condition))
    }
    SafeFlowButton("Consultar historial térmico", history, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
    SafeFlowButton("Vincular sensor", {}, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, enabled = false)
    SafeFlowButton("Revisar conectividad", {}, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, enabled = false)
}
private fun periodValue(text: String): Long? = try {
    val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.forLanguageTag("es-PE"))
    format.isLenient = false
    val position = java.text.ParsePosition(0)
    format.parse(text, position)?.time?.takeIf { position.index == text.length }
} catch (_: IllegalArgumentException) { null }
fun periodLabel(at: Long) = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.forLanguageTag("es-PE")).format(java.util.Date(at))
@Composable
fun HistoryScreen(title: String, readings: List<Reading>, rejections: List<RejectedReading>, now: Long,
    minimum: Double? = null, maximum: Double? = null) {
    var period by rememberSaveable { mutableStateOf("Hoy") }
    var picking by rememberSaveable { mutableStateOf(false) }
    var from by rememberSaveable { mutableStateOf(periodLabel(readings.minOfOrNull { it.at } ?: now)) }
    var to by rememberSaveable { mutableStateOf(periodLabel(now)) }
    var appliedFrom by rememberSaveable { mutableStateOf(0L) }
    var appliedTo by rememberSaveable { mutableStateOf(now) }
    SafeFlowText(title, variant = SafeFlowTextVariant.Headline)
    FilterChips(listOf("Hoy", "Periodo"), if (picking) "Periodo" else period) { selected -> if (selected == "Periodo") picking = true else { period = "Hoy"; picking = false } }
    if (picking) {
        val start = periodValue(from); val end = periodValue(to)
        FormField("Desde", from, { from = it }, "history-from", helper = "Inicio del periodo · dd/MM/yyyy HH:mm",
            error = if (start == null) "Ingresa una fecha y hora válidas." else null)
        FormField("Hasta", to, { to = it }, "history-to", helper = "Fin del periodo · dd/MM/yyyy HH:mm",
            error = when { end == null -> "Ingresa una fecha y hora válidas."; start != null && start > end -> "La fecha inicial no puede superar la final."; else -> null })
        SafeFlowButton("Consultar periodo", { appliedFrom = start!!; appliedTo = end!! + 59_999; period = "Periodo"; picking = false }, Modifier.fillMaxWidth(),
            enabled = start != null && end != null && start <= end)
        SafeFlowButton("Cancelar", { picking = false }, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
        return
    }
    val dayStart = java.util.Calendar.getInstance().apply { timeInMillis = now; set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis
    val start = if (period == "Hoy") dayStart else appliedFrom
    val end = if (period == "Hoy") now else appliedTo
    if (period == "Periodo") SafeFlowText("${periodLabel(start)} – ${periodLabel(end)}", variant = SafeFlowTextVariant.Caption)
    val filtered = readings.filter { it.at in start..end }.sortedBy { it.at }
    if (filtered.isEmpty()) SafeFlowCallout("Sin lecturas en este periodo", "Las temperaturas registradas aparecerán aquí.", tone = SafeFlowTone.Muted)
    else { ThermalChart(filtered, minimum, maximum); ReadingList(filtered) }
    val rejected = rejections.filter { it.at in start..end }.sortedBy { it.at }
    if (rejected.isNotEmpty()) {
        SafeFlowText("Mediciones rechazadas", variant = SafeFlowTextVariant.Title)
        rejected.forEach { SafeFlowCallout(dateLabel(it.at), "${it.raw} · ${it.reason}", tone = SafeFlowTone.Warning) }
    }
}
@Composable
fun ReadingList(readings: List<Reading>) {
    SafeFlowText("Lecturas · Orden cronológico", variant = SafeFlowTextVariant.Title)
    readings.sortedBy { it.at }.forEach {
        SafeFlowListCard(dateLabel(it.at), "Sensor ${it.sensorId}", "${it.temperature} °C", it.condition.label,
            Modifier.fillMaxWidth(), conditionTone(it.condition), icon = SafeFlowSymbol.Temperature)
    }
}
@Composable
fun ThermalChart(readings: List<Reading>, minimum: Double? = null, maximum: Double? = null) {
    if (readings.isEmpty()) return
    val sorted = readings.sortedBy { it.at }
    val colors = SafeFlowTheme.colors
    val low = minOf(sorted.minOf { it.temperature }, minimum ?: Double.POSITIVE_INFINITY)
    val high = maxOf(sorted.maxOf { it.temperature }, maximum ?: Double.NEGATIVE_INFINITY)
    val margin = ((high - low) * .1).coerceAtLeast(.5)
    val min = low - margin; val max = high + margin
    Surface(color = colors.surface, shape = RoundedCornerShape(SafeFlowRadius.medium)) {
        Column(Modifier.fillMaxWidth().padding(SafeFlowSpacing.lg), verticalArrangement = Arrangement.spacedBy(SafeFlowSpacing.sm)) {
            SafeFlowText("Evolución de temperatura (°C)", variant = SafeFlowTextVariant.Label)
            if (minimum != null && maximum != null) SafeFlowText("Rango permitido: $minimum–$maximum °C", variant = SafeFlowTextVariant.Caption)
            Row(horizontalArrangement = Arrangement.spacedBy(SafeFlowSpacing.sm)) {
                Column(Modifier.height(160.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    SafeFlowText(String.format(java.util.Locale.US, "%.1f", max), variant = SafeFlowTextVariant.Caption)
                    SafeFlowText(String.format(java.util.Locale.US, "%.1f", (max + min) / 2), variant = SafeFlowTextVariant.Caption)
                    SafeFlowText(String.format(java.util.Locale.US, "%.1f", min), variant = SafeFlowTextVariant.Caption)
                }
                Canvas(Modifier.weight(1f).height(160.dp)) {
                    val start = sorted.first().at; val timeRange = (sorted.last().at - start).coerceAtLeast(1)
                    fun y(value: Double) = ((max - value) / (max - min) * size.height).toFloat()
                    if (minimum != null && maximum != null) drawRect(colors.primaryContainer, topLeft = Offset(0f, y(maximum)), size = androidx.compose.ui.geometry.Size(size.width, (y(minimum) - y(maximum)).coerceAtLeast(1f)))
                    for (fraction in listOf(0f, .5f, 1f)) drawLine(colors.outline, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction))
                    drawLine(colors.textSecondary, Offset.Zero, Offset(0f, size.height))
                    drawLine(colors.textSecondary, Offset(0f, size.height), Offset(size.width, size.height))
                    listOfNotNull(minimum, maximum).forEach { bound -> drawLine(colors.primary, Offset(0f, y(bound)), Offset(size.width, y(bound)), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f))) }
                    sorted.groupBy { it.sensorId }.values.forEachIndexed { group, series ->
                        val tint = listOf(colors.primary, colors.warning, colors.critical)[group % 3]
                        val path = Path()
                        series.forEachIndexed { index, reading ->
                            val point = Offset(4f + ((reading.at - start).toDouble() / timeRange * (size.width - 8)).toFloat(), y(reading.temperature))
                            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                            drawCircle(tint, 3.dp.toPx(), point)
                        }
                        drawPath(path, tint, style = Stroke(2.dp.toPx()))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SafeFlowText(java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(sorted.first().at)), variant = SafeFlowTextVariant.Caption)
                SafeFlowText(java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(sorted.last().at)), variant = SafeFlowTextVariant.Caption)
            }
            SafeFlowText("Sensores: ${sorted.map { it.sensorId }.distinct().joinToString()}", variant = SafeFlowTextVariant.Caption)
        }
    }
}
data class RiskItem(val id: String, val name: String, val location: String, val minimum: Double, val maximum: Double, val reading: Reading)
@Composable
fun RiskScreen(items: List<RiskItem>, open: (String) -> Unit, history: () -> Unit) {
    SafeFlowText("Prioriza la atención", variant = SafeFlowTextVariant.Headline)
    SafeFlowText("El riesgo se evalúa con las reglas y el rango de cada producto.")
    if (items.isEmpty()) SafeFlowCallout("Sin productos en riesgo", "No hay desviaciones térmicas registradas que indiquen riesgo.", tone = SafeFlowTone.Muted)
    AdaptiveCards(items, { it.id }) { item, modifier ->
        SafeFlowListCard(item.name, "Producto ${item.id.take(8)} · ${item.location} · Rango ${item.minimum}–${item.maximum} °C",
            "${item.reading.temperature} °C", "${item.reading.condition.label} · ${dateLabel(item.reading.at)}", modifier,
            conditionTone(item.reading.condition), onClick = { open(item.id) }, icon = SafeFlowSymbol.Inventory)
    }
    SafeFlowButton("Consultar historial térmico", history, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
}
