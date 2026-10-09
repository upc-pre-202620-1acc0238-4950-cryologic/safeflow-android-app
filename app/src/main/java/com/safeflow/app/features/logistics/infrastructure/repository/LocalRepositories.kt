package com.safeflow.app.features.logistics.infrastructure.repository

import com.safeflow.app.core.infrastructure.local.*
import com.safeflow.app.features.logistics.domain.*
import javax.inject.Inject

class LocalShipmentRepository @Inject constructor(dao: RecordDao) :
    JsonRecords<Shipment>(dao, "shipments", Shipment::class.java), ShipmentRepository

