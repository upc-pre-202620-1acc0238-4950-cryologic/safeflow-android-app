package com.safeflow.app.features.iam.application

import com.safeflow.app.core.domain.*
import com.safeflow.app.features.iam.domain.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdentityService @Inject constructor(
    val identities: IdentityRepository, val roles: PermissionsRepository,
    val sessions: SessionRepository, private val hasher: PasswordHasher,
    private val transaction: Transaction, private val clock: Clock,
) : Access {
    suspend fun initialize() = transaction.run {
        for (role in Role.entries) {
            val id = "test-${role.name.lowercase()}-1"
            if (identities.find(id) == null) {
                val name = role.name.lowercase()
                val account = Account(id, "$name.test1@gmail.com", role)
                identities.save(id, Identity(account, hasher.hash("@${name}test1")))
            }
            run {
                val existing = roles.find(role.name)
                val permitted = existing?.granted?.intersect(IdentityRules.defaults(role)) ?: IdentityRules.defaults(role)
                if (existing == null || existing.granted != permitted)
                    roles.save(role.name, RolePermissions(role, permitted))
            }
        }
    }
    suspend fun register(emailInput: String, password: String, terms: Boolean) {
        val email = emailInput.trim().lowercase()
        ensure(IdentityRules.validEmail(email), "Ingresa un correo electrónico válido.")
        ensure(IdentityRules.validPassword(password), "La contraseña debe tener 8 caracteres como mínimo, una letra, un número y un símbolo.")
        ensure(terms, "Acepta los términos para crear tu cuenta.")
        transaction.run {
            ensure(identities.all().none { it.account.email == email }, "El correo ya tiene una cuenta. Inicia sesión o recupera el acceso.")
            val account = Account(newId(), email, Role.OPERADOR)
            identities.save(account.id, Identity(account, hasher.hash(password), verificationRequestedAt = clock.now()))
        }
    }
    suspend fun login(emailInput: String, password: String) {
        val email = emailInput.trim().lowercase()
        // Failure counters must commit before a denial is reported.
        val result: Pair<Account?, String?> = transaction.run {
            val identity = identities.all().find { it.account.email == email }
                ?: return@run null to "Correo o contraseña incorrectos."
            if (identity.blockedUntil > clock.now()) return@run null to "Acceso bloqueado temporalmente. Intenta en 5 minutos o solicita recuperar el acceso."
            if (!hasher.matches(password, identity.passwordHash)) {
                val failures = if (identity.blockedUntil != 0L) 1 else identity.failures + 1
                identities.save(identity.account.id, identity.copy(failures = failures,
                    blockedUntil = if (failures >= 5) clock.now() + 300_000 else 0))
                return@run null to if (failures >= 5) "Acceso bloqueado temporalmente por 5 minutos." else "Correo o contraseña incorrectos."
            }
            identities.save(identity.account.id, identity.copy(failures = 0, blockedUntil = 0))
            identity.account to null
        }
        result.second?.let { throw BusinessException(it) }
        sessions.save(Session(result.first!!.id, clock.now() + 1_800_000))
    }
    override suspend fun account(): Account? {
        val session = sessions.current() ?: return null
        if (session.expiresAt <= clock.now()) { sessions.save(null); return null }
        return identities.find(session.accountId)?.account
    }
    override suspend fun permissions(): Set<Permission> {
        val account = account() ?: return emptySet()
        // ADMIN administration is inherent; it is never a configurable permission.
        return roles.find(account.role.name)?.granted?.intersect(IdentityRules.defaults(account.role)) ?: emptySet()
    }
    override suspend fun require(permission: Permission): Account {
        val account = account() ?: throw BusinessException("Tu sesión terminó. Inicia sesión nuevamente.")
        ensure(permission in permissions(), "Tu rol no tiene permiso para realizar esta operación.")
        return account
    }
    override suspend fun requireAdmin(): Account {
        val account = account() ?: throw BusinessException("Inicia sesión nuevamente.")
        ensure(account.role == Role.ADMIN, "Solo ADMIN puede administrar permisos.")
        return account
    }
    suspend fun update(role: Role, granted: Set<Permission>) = transaction.run {
        requireAdmin()
        ensure(role != Role.ADMIN, "La capacidad administrativa de ADMIN no se puede modificar ni delegar.")
        ensure(IdentityRules.defaults(role).containsAll(granted), "Solo puedes asignar las operaciones propias de este rol.")
        roles.save(role.name, RolePermissions(role, granted))
    }
    suspend fun recover(emailInput: String) = transaction.run {
        val email = emailInput.trim().lowercase()
        ensure(IdentityRules.validEmail(email), "Ingresa un correo electrónico válido.")
        identities.all().find { it.account.email == email }?.let {
            identities.save(it.account.id, it.copy(recoveryRequestedAt = clock.now()))
        }
    }
    suspend fun logout() { sessions.save(null) }
}
