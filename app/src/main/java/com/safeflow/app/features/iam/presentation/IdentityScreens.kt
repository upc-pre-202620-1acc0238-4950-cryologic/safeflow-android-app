package com.safeflow.app.features.iam.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.safeflow.app.core.ui.components.*
import com.safeflow.app.core.ui.icons.SafeFlowSymbol
import com.safeflow.app.features.iam.domain.*

@Composable
fun LoginScreen(busy: Boolean, login: (String, String) -> Unit, register: () -> Unit, recover: () -> Unit, error: String? = null) {
    var email by rememberSaveable { mutableStateOf("") }; var password by rememberSaveable { mutableStateOf("") }
    SafeFlowBrand()
    SafeFlowText("Iniciar sesión", variant = SafeFlowTextVariant.Headline)
    SafeFlowText("Supervisa la cadena de frío de tus productos.", variant = SafeFlowTextVariant.BodyLarge)
    FormField("Correo electrónico", email, { email = it }, "login-email", helper = "Usa el correo de tu cuenta.",
        placeholder = "nombre@organizacion.com", error = if (email.isNotEmpty() && !IdentityRules.validEmail(email.trim())) "Ingresa un correo electrónico válido." else null)
    FormField("Contraseña", password, { password = it }, "login-password", password = true, helper = "Tu contraseña se mantiene oculta.", placeholder = "••••••••••", error = error)
    SafeFlowButton("Iniciar sesión", { login(email, password) }, Modifier.fillMaxWidth().testTag("login-submit"), enabled = !busy && IdentityRules.validEmail(email.trim()) && password.isNotEmpty())
    SafeFlowButton("Crear cuenta", register, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, !busy)
    SafeFlowButton("Recuperar acceso", recover, Modifier.fillMaxWidth(), SafeFlowButtonTone.Secondary, !busy)
}
@Composable
fun RegisterScreen(busy: Boolean, register: (String, String, Boolean) -> Unit, login: () -> Unit, error: String? = null) {
    var email by rememberSaveable { mutableStateOf("") }; var password by rememberSaveable { mutableStateOf("") }
    var terms by rememberSaveable { mutableStateOf(false) }
    SafeFlowBrand()
    SafeFlowText("Crear cuenta", variant = SafeFlowTextVariant.Headline)
    SafeFlowText("Crea tu identidad para acceder a SafeFlow.", variant = SafeFlowTextVariant.BodyLarge)
    val validEmail = IdentityRules.validEmail(email.trim()); val validPassword = IdentityRules.validPassword(password)
    FormField("Correo electrónico", email, { email = it }, "register-email", helper = "Será tu identificador de acceso.", placeholder = "persona@ejemplo.com",
        error = if (email.isNotEmpty() && !validEmail) "Ingresa un correo electrónico válido." else error?.takeIf { it.contains("correo", true) })
    FormField("Contraseña", password, { password = it }, "register-password", password = true,
        helper = "Mínimo 8 caracteres, una letra, un número y un símbolo.", error = if (password.isNotEmpty() && !validPassword) "La contraseña no cumple las reglas de seguridad." else null)
    SafeFlowCheckbox("Acepto los términos y condiciones.", terms, { terms = it }, Modifier.fillMaxWidth().testTag("register-terms"), !busy)
    SafeFlowButton("Crear cuenta", { register(email, password, terms) }, Modifier.fillMaxWidth().testTag("register-submit"), enabled = !busy && validEmail && validPassword && terms)
    SafeFlowButton("Ya tengo cuenta · Iniciar sesión", login, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
}
@Composable
fun VerificationScreen(login: () -> Unit) {
    SafeFlowBrand()
    SafeFlowText("Crear cuenta", variant = SafeFlowTextVariant.Headline)
    SafeFlowCallout("Cuenta creada", "Tu cuenta de OPERADOR y la solicitud de verificación quedaron registradas. El envío de correos está pendiente. No has iniciado sesión.", tone = SafeFlowTone.Success)
    SafeFlowButton("Volver a iniciar sesión", login, Modifier.fillMaxWidth())
}
@Composable
fun RecoveryScreen(busy: Boolean, recover: (String) -> Unit, back: () -> Unit, result: String? = null) {
    var email by rememberSaveable { mutableStateOf("") }
    SafeFlowBrand()
    SafeFlowText("Recuperar acceso", variant = SafeFlowTextVariant.Headline)
    SafeFlowText("Ingresa el correo de tu cuenta.")
    FormField("Correo electrónico", email, { email = it }, "recovery-email", placeholder = "persona@ejemplo.com",
        error = if (email.isNotEmpty() && !IdentityRules.validEmail(email.trim())) "Ingresa un correo electrónico válido." else null)
    SafeFlowCallout(if (result == null) "Recuperación de cuenta" else "Solicitud registrada", result ?: "El envío de correos de recuperación aún no está disponible.", tone = SafeFlowTone.Muted)
    SafeFlowButton("Solicitar recuperación", { recover(email) }, Modifier.fillMaxWidth(), enabled = !busy && IdentityRules.validEmail(email.trim()))
    SafeFlowButton("Volver a iniciar sesión", back, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
}
@Composable
fun AccountScreen(account: Account, permissions: Set<Permission>, back: () -> Unit, logout: () -> Unit, busy: Boolean) {
    SafeFlowText("Tu sesión", variant = SafeFlowTextVariant.Headline)
    SafeFlowListCard(account.role.name, "Usuario ${account.email}", "Sesión activa",
        if (account.role == Role.ADMIN) "La administración de permisos es exclusiva de ADMIN." else "Las operaciones disponibles dependen de los permisos del rol.", Modifier.fillMaxWidth(), icon = SafeFlowSymbol.Inventory)
    if (permissions.isEmpty() && account.role != Role.ADMIN)
        SafeFlowCallout("Sin operaciones asignadas", "Comunícate con un administrador para revisar los permisos de tu rol.", tone = SafeFlowTone.Muted)
    SafeFlowButton("Cerrar sesión", logout, Modifier.fillMaxWidth().testTag("logout"), SafeFlowButtonTone.Danger, !busy)
    SafeFlowButton("Volver", back, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined)
}
@Composable
fun PermissionsScreen(roles: List<RolePermissions>, busy: Boolean, save: (Role, Set<Permission>) -> Unit, result: String? = null) {
    var selected by rememberSaveable { mutableStateOf(Role.OPERADOR.name) }
    val op = roles.find { it.role == Role.OPERADOR }?.granted.orEmpty()
    val sup = roles.find { it.role == Role.SUPERVISOR }?.granted.orEmpty()
    var opDraft by rememberSaveable(op) { mutableStateOf(op.joinToString(",") { it.name }) }
    var supDraft by rememberSaveable(sup) { mutableStateOf(sup.joinToString(",") { it.name }) }
    val role = Role.valueOf(selected)
    val draft = (if (role == Role.OPERADOR) opDraft else supDraft).split(",").mapNotNull { name -> Permission.entries.find { it.name == name } }.toSet()
    val stored = if (role == Role.OPERADOR) op else sup
    fun change(value: Set<Permission>) { val text = value.joinToString(",") { it.name }; if (role == Role.OPERADOR) opDraft = text else supDraft = text }
    SafeFlowCallout("Administración exclusiva de ADMIN", "Solo ADMIN puede configurar permisos. Esta capacidad forma parte del rol y no puede concederse a OPERADOR ni SUPERVISOR.")
    SafeFlowText(role.name, variant = SafeFlowTextVariant.Headline)
    Row(horizontalArrangement = Arrangement.spacedBy(com.safeflow.app.core.ui.theme.SafeFlowSpacing.sm)) {
        listOf(Role.OPERADOR, Role.SUPERVISOR).forEach { item -> SafeFlowChip(item.name, role == item, { selected = item.name }, Modifier.width(130.dp).testTag("role-${item.name}"), !busy) }
    }
    SafeFlowText("Define las operaciones que este rol puede realizar.")
    IdentityRules.defaults(role).forEach { permission ->
        SafeFlowCheckbox(permission.label, permission in draft, { checked -> change(if (checked) draft + permission else draft - permission) },
            Modifier.fillMaxWidth().testTag("permission-${role}-${permission.name}"), !busy)
    }
    if (result != null) SafeFlowCallout("Permisos guardados", result, tone = SafeFlowTone.Success)
    SafeFlowButton("Guardar permisos", { save(role, draft) }, Modifier.fillMaxWidth(), enabled = !busy && draft != stored)
    if (draft != stored) SafeFlowButton("Descartar cambios", { change(stored) }, Modifier.fillMaxWidth(), SafeFlowButtonTone.Outlined, !busy)
}
