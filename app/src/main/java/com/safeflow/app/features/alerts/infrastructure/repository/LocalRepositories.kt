package com.safeflow.app.features.alerts.infrastructure.repository

import com.safeflow.app.core.infrastructure.local.*
import com.safeflow.app.features.alerts.domain.*
import javax.inject.Inject

class LocalIncidentRepository @Inject constructor(dao: RecordDao) :
    JsonRecords<Incident>(dao, "incidents", Incident::class.java), IncidentRepository

