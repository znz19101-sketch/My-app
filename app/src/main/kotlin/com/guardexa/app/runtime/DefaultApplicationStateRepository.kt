
package com.guardexa.app.runtime

import com.guardexa.database.dao.GuardexaDao
import com.guardexa.database.entity.SystemStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultApplicationStateRepository(
    private val dao: GuardexaDao,
    private val epochTime: () -> Long
) : ApplicationStateRepository {

    override fun observe(): Flow<GuardexaRuntimeState> =
        dao.observeSystemState().map { entity ->
            entity.toRuntimeState()
        }

    override suspend fun current(): GuardexaRuntimeState =
        dao.getSystemState().toRuntimeState()

    override suspend fun markSetupInProgress() {
        val now = epochTime()
        val old = dao.getSystemState()

        dao.upsertSystemState(
            (old ?: defaultEntity(now)).copy(
                setupCompleted = false,
                protectionEnabled = false,
                updatedAt = now
            )
        )
    }

    override suspend fun markSetupComplete(activeProfileId: String) {
        val now = epochTime()
        val old = dao.getSystemState()

        dao.upsertSystemState(
            (old ?: defaultEntity(now)).copy(
                setupCompleted = true,
                activeProfileId = activeProfileId,
                updatedAt = now
            )
        )
    }

    override suspend fun setProtectionEnabled(enabled: Boolean) {
        val now = epochTime()
        val old = dao.getSystemState() ?: defaultEntity(now)

        dao.upsertSystemState(
            old.copy(
                protectionEnabled = enabled,
                updatedAt = now
            )
        )
    }

    override suspend fun setSafeMode(enabled: Boolean) {
        val now = epochTime()
        val old = dao.getSystemState() ?: defaultEntity(now)

        dao.upsertSystemState(
            old.copy(
                safeModeEnabled = enabled,
                updatedAt = now
            )
        )
    }

    private fun SystemStateEntity?.toRuntimeState(): GuardexaRuntimeState {
        if (this == null) {
            return GuardexaRuntimeState(
                launchState = GuardexaLaunchState.FIRST_RUN,
                activeProfileId = null,
                protectionEnabled = false,
                safeModeEnabled = false
            )
        }

        val launch = when {
            safeModeEnabled -> GuardexaLaunchState.SAFE_MODE
            !setupCompleted -> GuardexaLaunchState.SETUP_IN_PROGRESS
            protectionEnabled -> GuardexaLaunchState.PROTECTION_ACTIVE
            else -> GuardexaLaunchState.READY
        }

        return GuardexaRuntimeState(
            launchState = launch,
            activeProfileId = activeProfileId,
            protectionEnabled = protectionEnabled,
            safeModeEnabled = safeModeEnabled
        )
    }

    private fun defaultEntity(now: Long) =
        SystemStateEntity(
            id = 1,
            setupCompleted = false,
            protectionEnabled = false,
            safeModeEnabled = false,
            activeProfileId = null,
            lastSelfTestAt = null,
            lastBootAt = null,
            databaseVersionSeen = 3,
            updatedAt = now
        )
}
