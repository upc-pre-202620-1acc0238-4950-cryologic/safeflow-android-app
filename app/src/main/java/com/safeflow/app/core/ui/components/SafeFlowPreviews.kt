package com.safeflow.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.safeflow.app.core.ui.icons.SafeFlowSymbol
import com.safeflow.app.core.ui.theme.SafeFlowSpacing
import com.safeflow.app.core.ui.theme.SafeFlowTheme

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    SafeFlowTheme {
        Surface(color = SafeFlowTheme.colors.background) {
            Column(Modifier.padding(SafeFlowSpacing.xl), verticalArrangement = Arrangement.spacedBy(SafeFlowSpacing.lg)) {
                content()
            }
        }
    }
}

@Preview(name = "Botones · celular", widthDp = 390)
@Composable
private fun ButtonVariantsPreview() = PreviewSurface {
    SafeFlowButtonTone.entries.forEach {
        SafeFlowButton(it.name, {}, Modifier.fillMaxWidth(), tone = it)
    }
    SafeFlowButton("Deshabilitado", {}, Modifier.fillMaxWidth(), enabled = false)
    SafeFlowButton("Con icono", {}, Modifier.fillMaxWidth(), icon = SafeFlowSymbol.Add)
}

@Preview(name = "Tipografía · celular", widthDp = 390)
@Composable
private fun TypographyVariantsPreview() = PreviewSurface {
    SafeFlowTextVariant.entries.forEach { SafeFlowText(it.name, variant = it) }
    Row(horizontalArrangement = Arrangement.spacedBy(SafeFlowSpacing.md)) {
        SafeFlowIcon(SafeFlowSymbol.Snowflake, "Marca")
        SafeFlowIcon(SafeFlowSymbol.Temperature, "Temperatura")
        SafeFlowIconButton(SafeFlowSymbol.Close, "Cerrar", {})
    }
}

@Preview(name = "Campos y selección · celular", widthDp = 390)
@Composable
private fun InputVariantsPreview() = PreviewSurface {
    var value by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    SafeFlowTextField("Campo vacío", value, { value = it }, placeholder = "Ingresa un valor", helper = "Dato requerido")
    SafeFlowTextField("Campo completo", "Texto de ejemplo", {}, helper = "Información complementaria")
    SafeFlowTextField("Campo con error", "", {}, error = "Completa este campo")
    SafeFlowTextField("Contraseña", "Ejemplo", {}, password = true)
    SafeFlowCheckbox("Opción genérica", checked, { checked = it }, Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(SafeFlowSpacing.sm)) {
        SafeFlowChip("Activo", true, {})
        SafeFlowChip("Inactivo", false, {})
    }
}

@Preview(name = "Tarjetas · celular", widthDp = 390)
@Composable
private fun CardsPreview() = PreviewSurface {
    listOf(SafeFlowTone.Normal, SafeFlowTone.Warning, SafeFlowTone.Critical, SafeFlowTone.Muted).forEach {
        SafeFlowListCard("Título de ejemplo", "Descripción del contenido", "Valor", "Estado: ${it.name}", Modifier.fillMaxWidth(), tone = it)
    }
}

@Preview(name = "Avisos · celular", widthDp = 390)
@Composable
private fun CalloutsPreview() = PreviewSurface {
    listOf(SafeFlowTone.Info, SafeFlowTone.Warning, SafeFlowTone.Critical, SafeFlowTone.Success).forEach {
        SafeFlowCallout("Aviso ${it.name}", "Descripción y siguiente paso.", tone = it)
    }
}

private val PreviewNavigation = listOf(
    SafeFlowNavigationItem("first", "Sección 1", SafeFlowSymbol.Inventory),
    SafeFlowNavigationItem("second", "Sección 2", SafeFlowSymbol.Shipment),
    SafeFlowNavigationItem("account", "Cuenta", SafeFlowSymbol.Account),
)

@Composable
private fun NavigationSample() {
    SafeFlowBrand(tagline = "Supervisión de cadena de frío")
    SafeFlowAppHeader("Encabezado genérico", onBackClick = {}, onAccountClick = {})
    SafeFlowBottomNavigation(PreviewNavigation, "first", {})
}

@Preview(name = "Navegación · celular", widthDp = 390)
@Preview(name = "Navegación · tableta", widthDp = 768)
@Composable
private fun NavigationPreview() = PreviewSurface { NavigationSample() }

@Preview(name = "Composición · celular", widthDp = 390)
@Preview(name = "Composición · tableta", widthDp = 768)
@Composable
private fun CompositionPreview() = PreviewSurface {
    SafeFlowAppHeader("Componentes", onBackClick = {}, onAccountClick = {})
    SafeFlowCallout("Información", "Estos datos solo ilustran los componentes reutilizables.")
    SafeFlowListCard("Título de ejemplo", "Descripción genérica", "Valor", "Estado", Modifier.fillMaxWidth())
    SafeFlowButton("Acción", {}, Modifier.fillMaxWidth())
    SafeFlowBottomNavigation(PreviewNavigation, "account", {})
}
