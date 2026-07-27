
package com.guardexa.database.dao

import androidx.room.*
import com.guardexa.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GuardexaDao {

    @Query("SELECT * FROM system_state WHERE id = 1 LIMIT 1")
    fun observeSystemState(): Flow<SystemStateEntity?>

    @Query("SELECT * FROM system_state WHERE id = 1 LIMIT 1")
    suspend fun getSystemState(): SystemStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSystemState(entity: SystemStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(entity: ActivityLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLogs(entities: List<ActivityLogEntity>)

    @Query("""
        SELECT * FROM activity_logs
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getActivityLogs(
        limit: Int,
        offset: Int
    ): List<ActivityLogEntity>

    @Query("""
        SELECT * FROM activity_logs
        WHERE eventType = :eventType
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun getActivityLogsByType(
        eventType: String,
        limit: Int
    ): List<ActivityLogEntity>

    @Query("""
        DELETE FROM activity_logs
        WHERE createdAt < :cutoff
    """)
    suspend fun deleteActivityLogsOlderThan(cutoff: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecurityEvent(entity: SecurityEventEntity)

    @Query("""
        SELECT * FROM security_events
        ORDER BY createdAt DESC
    """)
    fun observeSecurityEvents(): Flow<List<SecurityEventEntity>>

    @Query("""
        UPDATE security_events
        SET resolved = 1,
            resolvedAt = :resolvedAt,
            resolutionNote = :note
        WHERE id = :id
    """)
    suspend fun resolveSecurityEvent(
        id: String,
        resolvedAt: Long,
        note: String?
    )

    @Query("""
        DELETE FROM security_events
        WHERE createdAt < :cutoff
          AND resolved = 1
    """)
    suspend fun deleteResolvedSecurityEventsOlderThan(cutoff: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecurityEvidence(entity: SecurityEvidenceEntity)

    @Query("""
        SELECT * FROM security_evidence
        ORDER BY capturedAt ASC
    """)
    suspend fun getSecurityEvidenceOldestFirst(): List<SecurityEvidenceEntity>

    @Query("""
        SELECT * FROM security_evidence
        ORDER BY capturedAt DESC
    """)
    fun observeSecurityEvidenceNewestFirst(): Flow<List<SecurityEvidenceEntity>>

    @Query("DELETE FROM security_evidence WHERE id = :id")
    suspend fun deleteSecurityEvidence(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSelfTestResult(entity: SelfTestResultEntity)

    @Query("""
        SELECT * FROM self_test_results
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentSelfTests(limit: Int): List<SelfTestResultEntity>

    @Query("""
        DELETE FROM self_test_results
        WHERE createdAt < :cutoff
    """)
    suspend fun deleteSelfTestsOlderThan(cutoff: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueSoftDelete(entity: SoftDeleteQueueEntity)

    @Query("""
        SELECT * FROM soft_delete_queue
        WHERE deleteAfter <= :now
        ORDER BY deleteAfter ASC
    """)
    suspend fun getExpiredSoftDeletes(now: Long): List<SoftDeleteQueueEntity>

    @Query("""
        SELECT * FROM soft_delete_queue
        WHERE recordType = :recordType
          AND recordId = :recordId
        LIMIT 1
    """)
    suspend fun getSoftDelete(
        recordType: String,
        recordId: String
    ): SoftDeleteQueueEntity?

    @Query("DELETE FROM soft_delete_queue WHERE id = :id")
    suspend fun removeSoftDelete(id: String)

    @Transaction
    suspend fun trimEvidenceToLimit(maximum: Int): List<SecurityEvidenceEntity> {
        val all = getSecurityEvidenceOldestFirst()
        if (all.size <= maximum) return emptyList()
        val excess = all.take(all.size - maximum)
        excess.forEach { deleteSecurityEvidence(it.id) }
        return excess
    }
}
