
package com.guardexa.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object GuardexaMigrations {

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS system_state (
                    id INTEGER NOT NULL PRIMARY KEY,
                    setupCompleted INTEGER NOT NULL,
                    protectionEnabled INTEGER NOT NULL,
                    safeModeEnabled INTEGER NOT NULL,
                    activeProfileId TEXT,
                    lastSelfTestAt INTEGER,
                    lastBootAt INTEGER,
                    databaseVersionSeen INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())

            database.execSQL("""
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id TEXT NOT NULL PRIMARY KEY,
                    createdAt INTEGER NOT NULL,
                    eventType TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    packageName TEXT,
                    profileId TEXT,
                    titleCode TEXT NOT NULL,
                    messageCode TEXT NOT NULL,
                    metadataJson TEXT,
                    securityRelevant INTEGER NOT NULL
                )
            """.trimIndent())

            database.execSQL("""
                CREATE TABLE IF NOT EXISTS security_events (
                    id TEXT NOT NULL PRIMARY KEY,
                    createdAt INTEGER NOT NULL,
                    eventType TEXT NOT NULL,
                    reasonCode TEXT NOT NULL,
                    packageName TEXT,
                    evidenceId TEXT,
                    resolved INTEGER NOT NULL,
                    resolvedAt INTEGER,
                    resolutionNote TEXT
                )
            """.trimIndent())

            database.execSQL("""
                CREATE TABLE IF NOT EXISTS security_evidence (
                    id TEXT NOT NULL PRIMARY KEY,
                    encryptedFileName TEXT NOT NULL,
                    capturedAt INTEGER NOT NULL,
                    eventType TEXT NOT NULL,
                    width INTEGER NOT NULL,
                    height INTEGER NOT NULL,
                    encryptedSizeBytes INTEGER NOT NULL,
                    encryptionVersion INTEGER NOT NULL
                )
            """.trimIndent())

            database.execSQL("""
                CREATE TABLE IF NOT EXISTS self_test_results (
                    id TEXT NOT NULL PRIMARY KEY,
                    createdAt INTEGER NOT NULL,
                    testType TEXT NOT NULL,
                    resultLevel TEXT NOT NULL,
                    reasonCode TEXT,
                    detailsJson TEXT
                )
            """.trimIndent())

            database.execSQL("""
                CREATE TABLE IF NOT EXISTS soft_delete_queue (
                    id TEXT NOT NULL PRIMARY KEY,
                    recordType TEXT NOT NULL,
                    recordId TEXT NOT NULL,
                    deletedAt INTEGER NOT NULL,
                    deleteAfter INTEGER NOT NULL,
                    restorationPayloadJson TEXT
                )
            """.trimIndent())

            listOf(
                "CREATE INDEX IF NOT EXISTS index_activity_logs_createdAt ON activity_logs(createdAt)",
                "CREATE INDEX IF NOT EXISTS index_activity_logs_eventType ON activity_logs(eventType)",
                "CREATE INDEX IF NOT EXISTS index_activity_logs_severity ON activity_logs(severity)",
                "CREATE INDEX IF NOT EXISTS index_activity_logs_packageName ON activity_logs(packageName)",
                "CREATE INDEX IF NOT EXISTS index_security_events_createdAt ON security_events(createdAt)",
                "CREATE INDEX IF NOT EXISTS index_security_events_eventType ON security_events(eventType)",
                "CREATE INDEX IF NOT EXISTS index_security_events_resolved ON security_events(resolved)",
                "CREATE INDEX IF NOT EXISTS index_security_evidence_capturedAt ON security_evidence(capturedAt)",
                "CREATE INDEX IF NOT EXISTS index_security_evidence_eventType ON security_evidence(eventType)",
                "CREATE INDEX IF NOT EXISTS index_self_test_results_createdAt ON self_test_results(createdAt)",
                "CREATE INDEX IF NOT EXISTS index_self_test_results_resultLevel ON self_test_results(resultLevel)",
                "CREATE INDEX IF NOT EXISTS index_soft_delete_queue_deleteAfter ON soft_delete_queue(deleteAfter)",
                "CREATE INDEX IF NOT EXISTS index_soft_delete_queue_recordType ON soft_delete_queue(recordType)"
            ).forEach(database::execSQL)
        }
    }
}
