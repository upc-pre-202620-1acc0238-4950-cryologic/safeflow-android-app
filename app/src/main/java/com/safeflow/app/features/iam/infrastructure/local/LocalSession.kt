package com.safeflow.app.features.iam.infrastructure.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.safeflow.app.features.iam.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalSession @Inject constructor(private val store: DataStore<Preferences>) : SessionRepository {
    private val account = stringPreferencesKey("account")
    private val expires = longPreferencesKey("expires")
    override val sessions = store.data.map { p -> p[account]?.let { Session(it, p[expires] ?: 0) } }
    override suspend fun current() = sessions.first()
    override suspend fun save(session: Session?) {
        store.edit { p ->
            if (session == null) { p.remove(account); p.remove(expires) }
            else { p[account] = session.accountId; p[expires] = session.expiresAt }
        }
    }
}
