package com.safeflow.app.features.inventory.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.safeflow.app.core.ui.components.*
import com.safeflow.app.core.ui.icons.SafeFlowSymbol
import com.safeflow.app.features.inventory.domain.*

@Composable
fun InventoryScreen(products: List<Product>, condition: (Product) -> String, temperature: (Product) -> String, canWrite: Boolean,
    open: (String) -> Unit, create: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("Todos") }
    SafeFlowText("Productos registrados", variant = SafeFlowTextVariant.Headline)
    FilterChips(listOf("Todos", "Normal", "En riesgo"), filter) { filter = it }
    if (canWrite) SafeFlowButton("Registrar producto", create, Modifier.fillMaxWidth())
    val filtered = products.filter { filter == "Todos" || if (filter == "Normal") condition(it) == "Normal" else condition(it) in listOf("Advertencia", "Riesgo", "Crítico", "Desconectado") }
    if (filtered.isEmpty()) SafeFlowCallout("Sin productos", if (products.isEmpty()) "Registra un producto para comenzar a controlar tu inventario." else "No hay productos que coincidan con el estado seleccionado.", tone = SafeFlowTone.Muted)
    AdaptiveCards(filtered, { it.id }) { product, modifier ->
        SafeFlowListCard(product.name, "Producto ${product.id.take(8)} · ${product.stock} unidades", temperature(product),
            "${condition(product)} · Rango del producto: ${product.minimum}–${product.maximum} °C", modifier, tone = when (condition(product)) { "Crítico" -> SafeFlowTone.Critical; "Advertencia", "Riesgo", "Desconectado" -> SafeFlowTone.Warning; else -> SafeFlowTone.Normal }, onClick = { open(product.id) }, icon = SafeFlowSymbol.Inventory)
    }
}
@Composable
fun ProductForm(busy: Boolean, save: (String, String, String, String) -> Unit, cancel: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var min by rememberSaveable { mutableStateOf("") }; var max by rememberSaveable { mutableStateOf("") }
    val low = min.toDoubleOrNull(); val high = max.toDoubleOrNull()
    val minimumError = when { min.isEmpty() -> null; low == null || !low.isFinite() -> "Ingresa una temperatura numérica válida."; high != null && low > high -> "La temperatura mínima no puede superar a la máxima."; else -> null }
    val maximumError = if (max.isNotEmpty() && (high == null || !high.isFinite())) "Ingresa una temperatura numérica válida." else null
    SafeFlowText("Datos del producto", variant = SafeFlowTextVariant.Headline)
    FormField("Nombre del producto", name, { name = it }, "product-name", helper = "Requerido")
    FormField("Temperatura mínima (°C)", min, { min = it }, "product-min", numeric = true, helper = "Requerido", error = minimumError)
    FormField("Temperatura máxima (°C)", max, { max = it }, "product-max", numeric = true, helper = "Requerido", error = maximumError)
    SafeFlowButton("Registrar producto", { save(name, "", min, max) }, Modifier.fillMaxWidth().testTag("product-submit"),
        enabled = !busy && name.isNotBlank() && low != null && high != null && low.isFinite() && high.isFinite() && low <= high)
    SafeFlowButton("Cancelar", cancel, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, !busy)
}
@Composable
fun ProductScreen(product: Product?, lots: List<Lot>, canWrite: Boolean, addLot: () -> Unit,
    income: () -> Unit, openLot: (String) -> Unit, temperature: String, status: String, history: () -> Unit, hasSensor: Boolean) {
    if (product == null) { SafeFlowCallout("Producto no disponible", "Regresa al inventario."); return }
    SafeFlowListCard(product.name, "${product.id.take(8)} · ${product.stock} unidades · Rango: ${product.minimum}–${product.maximum} °C",
        temperature, status, Modifier.fillMaxWidth(), icon = SafeFlowSymbol.Inventory)
    if (hasSensor) SafeFlowCallout("Lectura actualizada", "La temperatura se recibe automáticamente del sensor. No requiere transcripción manual.")
    else SafeFlowCallout("Sin sensor vinculado", "No hay lecturas de temperatura para este producto.", tone = SafeFlowTone.Muted)
    if (canWrite) {
        SafeFlowButton("Registrar lote", addLot, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
        SafeFlowButton("Registrar ingreso", income, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
    }
    SafeFlowText("Lotes del producto", variant = SafeFlowTextVariant.Title)
    if (lots.isEmpty()) SafeFlowCallout("Sin lotes", "Registra un lote para identificar los productos que despacharás.", tone = SafeFlowTone.Muted)
    lots.forEach { lot -> SafeFlowListCard("Lote ${lot.code}", "Producto ${product.id.take(8)} · ${product.name}",
        "${product.stock} unidades", "Stock del producto asociado", Modifier.fillMaxWidth(), onClick = { openLot(lot.id) }, icon = SafeFlowSymbol.Inventory) }
    SafeFlowButton("Consultar historial térmico", history, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
    if (!hasSensor) {
        SafeFlowButton("Vincular sensor", {}, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, enabled = false)
        SafeFlowButton("Revisar conectividad", {}, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, enabled = false)
    }
}
@Composable
fun LotForm(product: Product?, busy: Boolean, save: (String) -> Unit, lots: List<Lot>, open: (String) -> Unit, cancel: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val existing = lots.find { it.code.equals(code.trim(), true) }
    SafeFlowListCard(product?.name ?: "Producto no disponible", "Producto ${product?.id?.take(8).orEmpty()}",
        "${product?.minimum}–${product?.maximum} °C", "Producto asociado", Modifier.fillMaxWidth(), icon = SafeFlowSymbol.Inventory)
    FormField("Identificación del lote", code, { code = it }, "lot-code", helper = "Requerido",
        error = if (existing != null) "El lote ${existing.code} ya existe. Consulta el lote registrado." else null)
    if (existing != null) {
        SafeFlowListCard("Lote ${existing.code}", "Producto ${product?.name}", "${product?.stock ?: 0} unidades", "Stock del producto asociado", Modifier.fillMaxWidth(), icon = SafeFlowSymbol.Inventory)
        SafeFlowButton("Consultar lote existente", { open(existing.id) }, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
    }
    SafeFlowButton("Registrar lote", { save(code) }, Modifier.fillMaxWidth().testTag("lot-submit"), enabled = !busy && product != null && code.isNotBlank() && existing == null)
    SafeFlowButton("Cancelar", cancel, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, !busy)
}
@Composable
fun LotScreen(product: Product?, lot: Lot?, created: Boolean, income: () -> Unit, back: () -> Unit) {
    if (lot == null || product == null) { SafeFlowCallout("Lote no disponible", "Regresa al producto asociado."); return }
    if (created) SafeFlowCallout("Lote registrado", "El lote quedó vinculado al producto. El ingreso de unidades se registra por separado.", tone = SafeFlowTone.Success)
    SafeFlowListCard("Lote ${lot.code}", "Producto ${product.id.take(8)} · ${product.name}", "${product.stock} unidades",
        "Stock del producto asociado · ${product.available} disponibles · ${product.reserved} reservadas", Modifier.fillMaxWidth(), icon = SafeFlowSymbol.Inventory)
    SafeFlowText("Rango del producto: ${product.minimum}–${product.maximum} °C")
    if (created) SafeFlowButton("Registrar ingreso", income, Modifier.fillMaxWidth())
    SafeFlowButton("Volver al producto", back, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
}
@Composable
fun IncomeForm(product: Product?, busy: Boolean, save: (String) -> Unit, cancel: () -> Unit) {
    var quantity by rememberSaveable { mutableStateOf("") }
    val amount = quantity.toIntOrNull()
    val error = when { quantity.isEmpty() -> null; amount == null || amount <= 0 -> "Ingresa un entero mayor que cero."; product != null && product.stock.toLong() + amount > Int.MAX_VALUE -> "La cantidad supera el límite admitido."; else -> null }
    SafeFlowListCard(product?.name ?: "Producto no disponible", "Producto ${product?.id?.take(8).orEmpty()}",
        "${product?.stock ?: 0} unidades", "Stock antes del ingreso", Modifier.fillMaxWidth(), icon = SafeFlowSymbol.Inventory)
    FormField("Cantidad de unidades", quantity, { quantity = it }, "income-quantity", numeric = true, helper = "Ingresa una cantidad positiva.", error = error)
    SafeFlowButton("Registrar ingreso", { save(quantity) }, Modifier.fillMaxWidth().testTag("income-submit"), enabled = !busy && product != null && amount != null && amount > 0 && error == null)
    SafeFlowButton("Cancelar", cancel, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, !busy)
}
