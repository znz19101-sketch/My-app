
package com.guardexa.app.runtime

import kotlinx.coroutines.flow.Flow

enum class GuardexaLaunchState {
    FIRST_RUN,
    SETUP_IN_PROGRESS,
    READY,
    PROTECTION_ACTIVE,
    SAFE_MODE
}

data class GuardexaRuntimeState(
    val launchState: GuardexaLaunchState,
    val activeProfileId: String?,
    val protectionEnabled: Boolean,
    val safeModeEnabled: Boolean
)

interface ApplicationStateRepository {
    fun observe(): Flow<GuardexaRuntimeState>
    suspend fun current(): GuardexaRuntimeState
    suspend fun markSetupInProgress()
    suspend fun markSetupComplete(activeProfileId: String)
    suspend fun setProtectionEnabled(enabled: Boolean)
    suspend fun setSafeMode(enabled: Boolean)
}
