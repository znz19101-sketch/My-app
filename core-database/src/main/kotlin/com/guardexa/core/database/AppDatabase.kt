
package com.guardexa.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.guardexa.core.database.dao.ProtectionProfileDao
import com.guardexa.core.database.entity.ProtectionProfileEntity
import com.guardexa.database.dao.GuardexaDao
import com.guardexa.database.entity.*
import com.guardexa.feature.apps.data.local.dao.AppsDao
import com.guardexa.feature.apps.data.local.entity.AppPolicyEntity
import com.guardexa.feature.apps.data.local.entity.InstalledAppEntity

@Database(
    entities = [
        ProtectionProfileEntity::class,
        InstalledAppEntity::class,
        AppPolicyEntity::class,
        SystemStateEntity::class,
        ActivityLogEntity::class,
        SecurityEventEntity::class,
        SecurityEvidenceEntity::class,
        SelfTestResultEntity::class,
        SoftDeleteQueueEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun protectionProfileDao(): ProtectionProfileDao
    abstract fun appsDao(): AppsDao
    abstract fun guardexaDao(): GuardexaDao
}
