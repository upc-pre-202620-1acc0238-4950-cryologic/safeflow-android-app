package com.safeflow.app.main.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.safeflow.app.core.domain.*
import com.safeflow.app.core.infrastructure.local.*
import com.safeflow.app.features.iam.application.IdentityService
import com.safeflow.app.features.iam.infrastructure.local.LocalSession
import com.safeflow.app.features.iam.infrastructure.repository.LocalPasswordHasher
import com.safeflow.app.features.alerts.application.AlertService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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

private val Context.sessionStore by preferencesDataStore("safeflow_session")
@Module
@InstallIn(SingletonComponent::class)
object LocalModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context) = LocalDatabase.open(context)
    @Provides fun dao(database: LocalDatabase) = database.records()
    @Provides @Singleton fun transaction(database: LocalDatabase): Transaction = RoomTransaction(database)
    @Provides fun clock(): Clock = Clock { System.currentTimeMillis() }
    @Provides @Singleton fun sessions(@ApplicationContext context: Context): SessionRepository = LocalSession(context.sessionStore)
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
