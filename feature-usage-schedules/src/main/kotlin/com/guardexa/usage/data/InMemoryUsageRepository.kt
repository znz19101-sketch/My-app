
package com.guardexa.usage.data

import com.guardexa.usage.model.*
import com.guardexa.usage.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryUsageRepository : UsageRepository {

    private val sessions = MutableStateFlow<List<UsageSession>>(emptyList())
    private val usages = MutableStateFlow<List<DailyUsage>>(emptyList())
    private val grants = MutableStateFlow<List<BonusTimeGrant>>(emptyList())

    override fun observeDailyUsage(
        usageDateKey: String,
        profileId: String
    ): Flow<List<DailyUsage>> =
        usages.map { list ->
            list.filter {
                it.usageDateKey == usageDateKey &&
                    it.profileId == profileId
            }
        }

    override fun observeBonusGrants(): Flow<List<BonusTimeGrant>> =
        grants

    override suspend fun saveSession(session: UsageSession) {
        sessions.update { current ->
            current.filterNot { it.id == session.id } + session
        }
    }

    override suspend fun saveDailyUsage(usage: DailyUsage) {
        usages.update { current ->
            current.filterNot {
                it.usageDateKey == usage.usageDateKey &&
                    it.packageName == usage.packageName &&
                    it.profileId == usage.profileId
            } + usage
        }
    }

    override suspend fun saveBonusGrant(grant: BonusTimeGrant) {
        grants.update { current ->
            current.filterNot { it.id == grant.id } + grant
        }
    }

    override suspend fun deactivateBonusGrant(id: String) {
        grants.update { current ->
            current.map {
                if (it.id == id) it.copy(active = false) else it
            }
        }
    }

    override suspend fun getOpenSessions(): List<UsageSession> =
        sessions.value.filter { it.endedAtEpochMillis == null }

    override suspend fun deleteEventsOlderThan(
        cutoffEpochMillis: Long
    ): Int {
        val before = sessions.value.size
        sessions.update { current ->
            current.filter {
                it.startedAtEpochMillis >= cutoffEpochMillis
            }
        }
        return before - sessions.value.size
    }
}
