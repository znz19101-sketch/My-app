
package com.guardexa.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "system_state")
data class SystemStateEntity(
    @PrimaryKey
    val id: Int = 1,
    val setupCompleted: Boolean,
    val protectionEnabled: Boolean,
    val safeModeEnabled: Boolean,
    val activeProfileId: String?,
    val lastSelfTestAt: Long?,
    val lastBootAt: Long?,
    val databaseVersionSeen: Int,
    val updatedAt: Long
)

@Entity(
    tableName = "activity_logs",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["eventType"]),
        Index(value = ["severity"]),
        Index(value = ["packageName"])
    ]
)
data class ActivityLogEntity(
    @PrimaryKey
    val id: String,
    val createdAt: Long,
    val eventType: String,
    val severity: String,
    val packageName: String?,
    val profileId: String?,
    val titleCode: String,
    val messageCode: String,
    val metadataJson: String?,
    val securityRelevant: Boolean
)

@Entity(
    tableName = "security_events",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["eventType"]),
        Index(value = ["resolved"])
    ]
)
data class SecurityEventEntity(
    @PrimaryKey
    val id: String,
    val createdAt: Long,
    val eventType: String,
    val reasonCode: String,
    val packageName: String?,
    val evidenceId: String?,
    val resolved: Boolean,
    val resolvedAt: Long?,
    val resolutionNote: String?
)

@Entity(
    tableName = "security_evidence",
    indices = [
        Index(value = ["capturedAt"]),
        Index(value = ["eventType"])
    ]
)
data class SecurityEvidenceEntity(
    @PrimaryKey
    val id: String,
    val encryptedFileName: String,
    val capturedAt: Long,
    val eventType: String,
    val width: Int,
    val height: Int,
    val encryptedSizeBytes: Long,
    val encryptionVersion: Int
)

@Entity(
    tableName = "self_test_results",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["resultLevel"])
    ]
)
data class SelfTestResultEntity(
    @PrimaryKey
    val id: String,
    val createdAt: Long,
    val testType: String,
    val resultLevel: String,
    val reasonCode: String?,
    val detailsJson: String?
)

@Entity(
    tableName = "soft_delete_queue",
    indices = [
        Index(value = ["deleteAfter"]),
        Index(value = ["recordType"])
    ]
)
data class SoftDeleteQueueEntity(
    @PrimaryKey
    val id: String,
    val recordType: String,
    val recordId: String,
    val deletedAt: Long,
    val deleteAfter: Long,
    val restorationPayloadJson: String?
)
