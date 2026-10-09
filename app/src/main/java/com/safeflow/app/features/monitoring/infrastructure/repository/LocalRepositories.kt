package com.safeflow.app.features.monitoring.infrastructure.repository

import com.safeflow.app.core.infrastructure.local.*
import com.safeflow.app.features.monitoring.domain.*
import javax.inject.Inject

class LocalSensorRepository @Inject constructor(dao: RecordDao) :
    JsonRecords<Sensor>(dao, "sensors", Sensor::class.java), SensorRepository

class LocalReadingRepository @Inject constructor(dao: RecordDao) :
    JsonRecords<Reading>(dao, "readings", Reading::class.java), ReadingRepository

class LocalRejectedReadingRepository @Inject constructor(dao: RecordDao) :
    JsonRecords<RejectedReading>(dao, "rejections", RejectedReading::class.java), RejectionRepository

