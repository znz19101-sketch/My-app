
package com.guardexa.database.repository

import com.guardexa.database.dao.GuardexaDao
import com.guardexa.database.entity.*
import com.guardexa.database.model.LogRetentionPolicy
import com.guardexa.security.evidence.SecurityEvidenceIndex
import com.guardexa.security.evidence.SecurityEvidenceMetadata
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ActivityLogRepository {
    suspend fun log(
        eventType: String,
        severity: String,
        titleCode: String,
        messageCode: String,
        packageName: String? = null,
        profileId: String? = null,
        metadataJson: String? = null,
        securityRelevant: Boolean = false
    )
}

class RoomActivityLogRepository(
    private val dao: GuardexaDao,
    private val clock: () -> Long
) : ActivityLogRepository {
    override suspend fun log(
        eventType: String,
        severity: String,
        titleCode: String,
        messageCode: String,
        packageName: String?,
        profileId: String?,
        metadataJson: String?,
        securityRelevant: Boolean
    ) {
        dao.insertActivityLog(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                createdAt = clock(),
                eventType = eventType,
                severity = severity,
                packageName = packageName,
                profileId = profileId,
                titleCode = titleCode,
                messageCode = messageCode,
                metadataJson = metadataJson,
                securityRelevant = securityRelevant
            )
        )
    }
}

class RoomSecurityEvidenceIndex(
    private val dao: GuardexaDao
) : SecurityEvidenceIndex {

    override suspend fun insert(metadata: SecurityEvidenceMetadata) {
        dao.insertSecurityEvidence(
            SecurityEvidenceEntity(
                id = metadata.id,
                encryptedFileName = metadata.encryptedFileName,
                capturedAt = metadata.capturedAtEpochMillis,
                eventType = metadata.eventType,
                width = metadata.width,
                height = metadata.height,
                encryptedSizeBytes = metadata.encryptedSizeBytes,
                encryptionVersion = metadata.encryptionVersion
            )
        )
    }

    override suspend fun listOldestFirst(): List<SecurityEvidenceMetadata> =
        dao.getSecurityEvidenceOldestFirst().map {
            SecurityEvidenceMetadata(
                id = it.id,
                encryptedFileName = it.encryptedFileName,
                capturedAtEpochMillis = it.capturedAt,
                eventType = it.eventType,
                width = it.width,
                height = it.height,
                encryptedSizeBytes = it.encryptedSizeBytes,
                encryptionVersion = it.encryptionVersion
            )
        }

    override suspend fun delete(id: String) {
        dao.deleteSecurityEvidence(id)
    }
}

data class CleanupResult(
    val deletedActivityLogs: Int,
    val deletedSecurityEvents: Int,
    val deletedSelfTests: Int
)

class DataRetentionManager(
    private val dao: GuardexaDao
) {
    suspend fun cleanup(
        nowEpochMillis: Long,
        policy: LogRetentionPolicy
    ): CleanupResult {
        val activityCutoff = nowEpochMillis - policy.activityLogRetentionMillis
        val securityCutoff = nowEpochMillis - policy.securityLogRetentionMillis
        val selfTestCutoff = nowEpochMillis - policy.selfTestRetentionMillis

        return CleanupResult(
            deletedActivityLogs = dao.deleteActivityLogsOlderThan(activityCutoff),
            deletedSecurityEvents = dao.deleteResolvedSecurityEventsOlderThan(securityCutoff),
            deletedSelfTests = dao.deleteSelfTestsOlderThan(selfTestCutoff)
        )
    }
}
