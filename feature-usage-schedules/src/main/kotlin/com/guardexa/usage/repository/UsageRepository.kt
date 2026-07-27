
package com.guardexa.usage.repository

import com.guardexa.usage.model.*
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    fun observeDailyUsage(
        usageDateKey: String,
        profileId: String
    ): Flow<List<DailyUsage>>

    fun observeBonusGrants(): Flow<List<BonusTimeGrant>>

    suspend fun saveSession(session: UsageSession)
    suspend fun saveDailyUsage(usage: DailyUsage)
    suspend fun saveBonusGrant(grant: BonusTimeGrant)
    suspend fun deactivateBonusGrant(id: String)
    suspend fun getOpenSessions(): List<UsageSession>
    suspend fun deleteEventsOlderThan(cutoffEpochMillis: Long): Int
}
