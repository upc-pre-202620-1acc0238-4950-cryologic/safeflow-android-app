package com.safeflow.app.features.inventory.infrastructure.repository

import com.safeflow.app.core.infrastructure.local.*
import com.safeflow.app.features.inventory.domain.*
import javax.inject.Inject

class LocalProductRepository @Inject constructor(dao: RecordDao) :
    JsonRecords<Product>(dao, "products", Product::class.java), ProductRepository

class LocalLotRepository @Inject constructor(dao: RecordDao) :
    JsonRecords<Lot>(dao, "lots", Lot::class.java), LotRepository

class LocalIncomeRepository @Inject constructor(dao: RecordDao) :
    JsonRecords<Income>(dao, "incomes", Income::class.java), IncomeRepository

