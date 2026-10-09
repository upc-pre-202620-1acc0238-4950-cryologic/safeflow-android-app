package com.safeflow.app.features.iam.domain

import com.safeflow.app.core.domain.Records
import kotlinx.coroutines.flow.Flow

enum class Role { OPERADOR, SUPERVISOR, ADMIN }
enum class Permission(val label: String) {
    WRITE_INVENTORY("Registrar productos, lotes e ingresos"),
    READ_INVENTORY("Consultar inventario y temperaturas"),
    READ_SHIPMENTS("Consultar y validar despachos"),
    CREATE_SHIPMENT("Registrar despachos"),
    DELIVER("Confirmar entregas"),
    SUMMARY("Consultar resumen operativo"),
    RISKS("Consultar riesgos e historial térmico"),
    ALERTS("Recibir y consultar alertas"),
    RESOLVE("Registrar acciones correctivas")
}
data class Account(val id: String, val email: String, val role: Role)
data class Identity(val account: Account, val passwordHash: String, val failures: Int = 0,
    val blockedUntil: Long = 0, val verificationRequestedAt: Long? = null, val recoveryRequestedAt: Long? = null)
data class RolePermissions(val role: Role, val granted: Set<Permission>)
data class Session(val accountId: String, val expiresAt: Long)
interface IdentityRepository : Records<Identity>
interface PermissionsRepository : Records<RolePermissions>
interface SessionRepository {
    val sessions: Flow<Session?>
    suspend fun current(): Session?
    suspend fun save(session: Session?)
}
interface PasswordHasher { fun hash(password: String): String; fun matches(password: String, hash: String): Boolean }
interface Access {
    suspend fun account(): Account?
    suspend fun permissions(): Set<Permission>
    suspend fun require(permission: Permission): Account
    suspend fun requireAdmin(): Account
}
object IdentityRules {
    fun validEmail(email: String) = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)
    fun validPassword(value: String) = value.length >= 8 && value.any(Char::isLetter) &&
        value.any(Char::isDigit) && value.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    fun defaults(role: Role): Set<Permission> = when (role) {
        Role.OPERADOR -> setOf(Permission.WRITE_INVENTORY, Permission.READ_INVENTORY,
            Permission.READ_SHIPMENTS, Permission.CREATE_SHIPMENT, Permission.DELIVER)
        Role.SUPERVISOR -> setOf(Permission.SUMMARY, Permission.RISKS, Permission.ALERTS, Permission.RESOLVE)
        Role.ADMIN -> emptySet()
    }
}
