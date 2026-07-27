
package com.guardexa.feature.apps.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Add the entities from this pack to AppDatabase:
 *
 * entities = [
 *   ...,
 *   InstalledAppEntity::class,
 *   AppPolicyEntity::class
 * ]
 *
 * and expose:
 *
 * abstract fun appsDao(): AppsDao
 */
object AppsDatabaseIntegration {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS installed_apps (
                    packageName TEXT NOT NULL PRIMARY KEY,
                    originalName TEXT NOT NULL,
                    versionCode INTEGER NOT NULL,
                    versionName TEXT,
                    installerPackage TEXT,
                    isSystemApp INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    firstDetectedAt INTEGER NOT NULL,
                    lastUpdatedAt INTEGER NOT NULL,
                    lastSeenAt INTEGER NOT NULL,
                    reviewed INTEGER NOT NULL,
                    isRemoved INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            database.execSQL("""
                CREATE TABLE IF NOT EXISTS app_policies (
                    packageName TEXT NOT NULL,
                    profileId TEXT NOT NULL,
                    accessPolicy TEXT NOT NULL,
                    dailyLimitMinutes INTEGER,
                    requiresGlasses INTEGER NOT NULL,
                    alwaysAllowed INTEGER NOT NULL,
                    emergencyApp INTEGER NOT NULL,
                    allowCurrentActivityToFinish INTEGER NOT NULL,
                    gracePeriodSeconds INTEGER NOT NULL,
                    categoryOverride TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    recordVersion INTEGER NOT NULL,
                    isDeleted INTEGER NOT NULL,
                    deletedAt INTEGER,
                    PRIMARY KEY(packageName, profileId),
                    FOREIGN KEY(packageName)
                        REFERENCES installed_apps(packageName)
                        ON UPDATE CASCADE
                        ON DELETE CASCADE
                )
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_installed_apps_originalName
                ON installed_apps(originalName)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_installed_apps_category
                ON installed_apps(category)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_installed_apps_reviewed
                ON installed_apps(reviewed)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_installed_apps_lastUpdatedAt
                ON installed_apps(lastUpdatedAt)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_app_policies_packageName
                ON app_policies(packageName)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_app_policies_profileId
                ON app_policies(profileId)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_app_policies_accessPolicy
                ON app_policies(accessPolicy)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_app_policies_requiresGlasses
                ON app_policies(requiresGlasses)
            """.trimIndent())

            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_app_policies_alwaysAllowed
                ON app_policies(alwaysAllowed)
            """.trimIndent())
        }
    }
}
