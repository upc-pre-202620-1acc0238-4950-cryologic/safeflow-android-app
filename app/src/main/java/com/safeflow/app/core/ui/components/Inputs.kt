package com.safeflow.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import com.safeflow.app.core.ui.icons.SafeFlowSymbol
import com.safeflow.app.core.ui.theme.SafeFlowRadius
import com.safeflow.app.core.ui.theme.SafeFlowSizes
import com.safeflow.app.core.ui.theme.SafeFlowSpacing
import com.safeflow.app.core.ui.theme.SafeFlowTextStyles
import com.safeflow.app.core.ui.theme.SafeFlowTheme

@Composable
fun SafeFlowTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    helper: String? = null,
    error: String? = null,
    password: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    inputModifier: Modifier = Modifier,
    required: Boolean = false,
) {
    val colors = SafeFlowTheme.colors
    var passwordVisible by remember { mutableStateOf(false) }
    var focusedOnce by remember { mutableStateOf(false) }
    var touched by rememberSaveable { mutableStateOf(false) }
    val displayedError = error ?: if (required && touched && value.isBlank()) "Este dato es requerido." else null
    Column(modifier.background(colors.surface, RoundedCornerShape(SafeFlowRadius.medium)), verticalArrangement = Arrangement.spacedBy(SafeFlowSpacing.sm)) {
        SafeFlowText(label, variant = SafeFlowTextVariant.Label)
        BasicTextField(
            value = value, onValueChange = onValueChange, enabled = enabled,
            modifier = inputModifier.fillMaxWidth().onFocusChanged { if (it.isFocused) focusedOnce = true else if (focusedOnce) touched = true }
                .semantics { contentDescription = label; displayedError?.let { error(it) } }, textStyle = SafeFlowTextStyles.bodyMedium.copy(color = colors.textPrimary),
            singleLine = singleLine, keyboardOptions = keyboardOptions,
            cursorBrush = SolidColor(colors.primary),
            visualTransformation = if (password && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { inner ->
                Surface(color = colors.surface, shape = RoundedCornerShape(SafeFlowRadius.medium),
                    border = BorderStroke(1.dp, if (displayedError != null) colors.critical else colors.outline)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = SafeFlowSizes.touchTarget).padding(horizontal = SafeFlowSpacing.md),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).padding(vertical = SafeFlowSpacing.md)) {
                            if (value.isEmpty()) SafeFlowText(placeholder, color = colors.textSecondary)
                            inner()
                        }
                        if (password) SafeFlowIconButton(
                            if (passwordVisible) SafeFlowSymbol.Hidden else SafeFlowSymbol.Visible,
                            if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            { passwordVisible = !passwordVisible }, enabled = enabled)
                    }
                }
            },
        )
        if (displayedError != null || helper != null) SafeFlowText(displayedError ?: helper.orEmpty(), variant = SafeFlowTextVariant.Caption,
            color = if (displayedError != null) colors.critical else colors.textSecondary)
    }
}

@Composable
fun SafeFlowCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SafeFlowTheme.colors
    Surface(modifier = modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange), color = colors.surface, shape = RoundedCornerShape(SafeFlowRadius.medium)) {
        Row(
            modifier = Modifier
                .heightIn(min = SafeFlowSizes.navigationItem)
                .padding(horizontal = SafeFlowSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SafeFlowSpacing.md),
        ) {
            SafeFlowIcon(if (checked) SafeFlowSymbol.CheckboxChecked else SafeFlowSymbol.CheckboxEmpty, null,
                tint = if (!enabled) colors.textSecondary else if (checked) colors.primary else colors.textSecondary)
            SafeFlowText(label, modifier = Modifier.weight(1f), variant = SafeFlowTextVariant.BodyMedium)
        }
    }
}
