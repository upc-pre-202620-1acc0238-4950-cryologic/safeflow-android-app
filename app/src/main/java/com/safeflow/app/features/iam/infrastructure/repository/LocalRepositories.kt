package com.safeflow.app.features.iam.infrastructure.repository

import com.safeflow.app.core.infrastructure.local.*
import com.safeflow.app.features.iam.domain.*
import javax.inject.Inject

class LocalIdentityRepository @Inject constructor(dao: RecordDao) :
    JsonRecords<Identity>(dao, "identities", Identity::class.java), IdentityRepository

class LocalRolePermissionsRepository @Inject constructor(dao: RecordDao) :
    JsonRecords<RolePermissions>(dao, "permissions", RolePermissions::class.java), PermissionsRepository

