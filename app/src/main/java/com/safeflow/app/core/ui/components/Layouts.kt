package com.safeflow.app.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.safeflow.app.core.ui.theme.SafeFlowSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScreenContent(key: String, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 900.dp).fillMaxWidth().testTag("screen-$key"),
            contentPadding = PaddingValues(SafeFlowSpacing.xl), verticalArrangement = Arrangement.spacedBy(SafeFlowSpacing.lg)) {
            item { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SafeFlowSpacing.lg), content = content) }
        }
    }
}
@Composable
fun FormField(label: String, value: String, onChange: (String) -> Unit, id: String,
    password: Boolean = false, numeric: Boolean = false, enabled: Boolean = true,
    helper: String? = null, error: String? = null, placeholder: String = "Ingresa un valor") {
    SafeFlowTextField(label, value, onChange, Modifier.fillMaxWidth(), password = password,
        enabled = enabled, helper = helper, error = error, required = true, placeholder = placeholder, inputModifier = Modifier.testTag(id),
        keyboardOptions = KeyboardOptions(keyboardType = when { password -> KeyboardType.Password; numeric -> KeyboardType.Decimal; else -> KeyboardType.Text }))
}
@Composable
fun <T> AdaptiveCards(items: List<T>, key: (T) -> String, card: @Composable (T, Modifier) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 600.dp) 2 else 1
        val cardWidth = ((maxWidth - SafeFlowSpacing.lg) / 2).coerceAtMost(342.dp)
        Column(verticalArrangement = Arrangement.spacedBy(SafeFlowSpacing.lg)) {
            items.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SafeFlowSpacing.lg)) {
                    row.forEach { value -> androidx.compose.runtime.key(key(value)) { card(value, if (columns == 2) Modifier.width(cardWidth) else Modifier.fillMaxWidth()) } }
                }
            }
        }
    }
}
@Composable
fun <T> ChoiceField(label: String, values: List<T>, selected: String, id: (T) -> String,
    text: (T) -> String, onSelect: (String) -> Unit, empty: String = "No hay opciones disponibles.") {
    SafeFlowText(label, variant = SafeFlowTextVariant.Label)
    if (values.isEmpty()) SafeFlowCallout(label, empty, tone = SafeFlowTone.Muted)
    else FlowRow(horizontalArrangement = Arrangement.spacedBy(SafeFlowSpacing.sm), verticalArrangement = Arrangement.spacedBy(SafeFlowSpacing.sm)) {
        values.forEach { value -> SafeFlowChip(text(value), selected == id(value), { onSelect(id(value)) }) }
    }
}
fun dateLabel(at: Long?) = at?.let { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("es-PE")).format(Date(it)) } ?: "—"

@Composable
fun FilterChips(values: List<String>, selected: String, width: androidx.compose.ui.unit.Dp = 108.dp, select: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(SafeFlowSpacing.sm), verticalArrangement = Arrangement.spacedBy(SafeFlowSpacing.sm)) {
        values.forEach { value -> SafeFlowChip(value, selected == value, { select(value) }, Modifier.width(width)) }
    }
}
