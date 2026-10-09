package com.safeflow.app

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.DataStore
import kotlinx.coroutines.*
import java.util.UUID
import dagger.hilt.testing.TestInstallIn
import com.safeflow.app.main.di.LocalModule
import com.safeflow.app.core.domain.*
import com.safeflow.app.core.infrastructure.local.*
import com.safeflow.app.features.iam.application.IdentityService
import com.safeflow.app.features.iam.infrastructure.local.LocalSession
import com.safeflow.app.features.iam.infrastructure.repository.LocalPasswordHasher
import com.safeflow.app.features.alerts.application.AlertService
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.iam.infrastructure.repository.*
import com.safeflow.app.features.inventory.domain.*
import com.safeflow.app.features.inventory.infrastructure.repository.*
import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.monitoring.infrastructure.repository.*
import com.safeflow.app.features.alerts.domain.*
import com.safeflow.app.features.alerts.infrastructure.repository.*
import com.safeflow.app.features.logistics.domain.*
import com.safeflow.app.features.logistics.infrastructure.repository.*

class TestClock : Clock {
    var value = System.currentTimeMillis()
    override fun now() = value
}
class TestStorage(context: Context) {
    val dbFile = context.cacheDir.resolve("e2e-${UUID.randomUUID()}.db")
    val prefFile = context.cacheDir.resolve("e2e-${UUID.randomUUID()}.preferences_pb")
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db = LocalDatabase.open(context, dbFile.absolutePath)
    val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { prefFile })
    fun dispose() { db.close(); scope.cancel(); dbFile.delete(); prefFile.delete() }
}
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [LocalModule::class])
object TestLocalModule {
    @Provides @Singleton fun storage(@ApplicationContext context: Context) = TestStorage(context)
    @Provides @Singleton fun database(storage: TestStorage) = storage.db
    @Provides fun dao(database: LocalDatabase) = database.records()
    @Provides @Singleton fun transaction(database: LocalDatabase): Transaction = RoomTransaction(database)
    @Provides @Singleton fun testClock() = TestClock()
    @Provides fun clock(impl: TestClock): Clock = impl
    @Provides @Singleton fun sessions(storage: TestStorage): SessionRepository = LocalSession(storage.store)
    @Provides fun hasher(impl: LocalPasswordHasher): PasswordHasher = impl
    @Provides fun access(impl: IdentityService): Access = impl
    @Provides fun anomalies(impl: AlertService): AnomalySink = impl
    @Provides @Singleton fun identityRepository(impl: LocalIdentityRepository): IdentityRepository = impl
    @Provides @Singleton fun rolePermissionsRepository(impl: LocalRolePermissionsRepository): PermissionsRepository = impl
    @Provides @Singleton fun productRepository(impl: LocalProductRepository): ProductRepository = impl
    @Provides @Singleton fun lotRepository(impl: LocalLotRepository): LotRepository = impl
    @Provides @Singleton fun incomeRepository(impl: LocalIncomeRepository): IncomeRepository = impl
    @Provides @Singleton fun sensorRepository(impl: LocalSensorRepository): SensorRepository = impl
    @Provides @Singleton fun readingRepository(impl: LocalReadingRepository): ReadingRepository = impl
    @Provides @Singleton fun rejectedReadingRepository(impl: LocalRejectedReadingRepository): RejectionRepository = impl
    @Provides @Singleton fun incidentRepository(impl: LocalIncidentRepository): IncidentRepository = impl
    @Provides @Singleton fun shipmentRepository(impl: LocalShipmentRepository): ShipmentRepository = impl
}
