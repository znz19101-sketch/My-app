
package com.guardexa.database.repository

import com.guardexa.database.dao.GuardexaDao
import com.guardexa.database.entity.SoftDeleteQueueEntity
import java.util.UUID

interface SoftDeleteHandler {
    val recordType: String
    suspend fun permanentlyDelete(recordId: String)
    suspend fun restore(recordId: String, restorationPayloadJson: String?)
}

class SoftDeleteManager(
    private val dao: GuardexaDao,
    handlers: Set<SoftDeleteHandler>,
    private val retentionMillis: Long = 7L * 24L * 60L * 60L * 1000L
) {
    private val handlersByType = handlers.associateBy { it.recordType }

    suspend fun enqueue(
        recordType: String,
        recordId: String,
        deletedAt: Long,
        restorationPayloadJson: String?
    ) {
        dao.enqueueSoftDelete(
            SoftDeleteQueueEntity(
                id = UUID.randomUUID().toString(),
                recordType = recordType,
                recordId = recordId,
                deletedAt = deletedAt,
                deleteAfter = deletedAt + retentionMillis,
                restorationPayloadJson = restorationPayloadJson
            )
        )
    }

    suspend fun restore(
        recordType: String,
        recordId: String
    ): Boolean {
        val queued = dao.getSoftDelete(recordType, recordId) ?: return false
        val handler = handlersByType[recordType] ?: return false

        handler.restore(
            recordId = recordId,
            restorationPayloadJson = queued.restorationPayloadJson
        )
        dao.removeSoftDelete(queued.id)
        return true
    }

    suspend fun purgeExpired(nowEpochMillis: Long): Int {
        val expired = dao.getExpiredSoftDeletes(nowEpochMillis)
        var count = 0

        for (entry in expired) {
            val handler = handlersByType[entry.recordType] ?: continue
            handler.permanentlyDelete(entry.recordId)
            dao.removeSoftDelete(entry.id)
            count++
        }

        return count
    }
}
