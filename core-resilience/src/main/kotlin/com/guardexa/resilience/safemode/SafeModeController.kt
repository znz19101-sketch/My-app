
package com.guardexa.resilience.safemode

import com.guardexa.resilience.model.SafeModeReason
import com.guardexa.resilience.model.SafeModeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SafeModePersistence {
    suspend fun load(): SafeModeState
    suspend fun save(state: SafeModeState)
}

class SafeModeController(
    private val persistence: SafeModePersistence,
    private val epochTime: () -> Long,
    private val emergencyPackages: Set<String>,
    private val systemPackages: Set<String>
) {
    private val _state = MutableStateFlow(
        SafeModeState(
            enabled = false,
            reason = null,
            enabledAtEpochMillis = null,
            allowedPackages = emergencyPackages + systemPackages,
            recoveryAttempts = 0,
            lastRecoveryAtEpochMillis = null
        )
    )
    val state: StateFlow<SafeModeState> = _state.asStateFlow()

    suspend fun initialize() {
        _state.value = persistence.load()
    }

    suspend fun enable(
        reason: SafeModeReason,
        additionalAllowedPackages: Set<String> = emptySet()
    ) {
        val next = SafeModeState(
            enabled = true,
            reason = reason,
            enabledAtEpochMillis = epochTime(),
            allowedPackages = emergencyPackages +
                systemPackages +
                additionalAllowedPackages,
            recoveryAttempts = _state.value.recoveryAttempts,
            lastRecoveryAtEpochMillis = _state.value.lastRecoveryAtEpochMillis
        )
        _state.value = next
        persistence.save(next)
    }

    suspend fun disableAfterVerifiedRecovery() {
        val next = _state.value.copy(
            enabled = false,
            reason = null,
            enabledAtEpochMillis = null,
            recoveryAttempts = 0,
            lastRecoveryAtEpochMillis = epochTime()
        )
        _state.value = next
        persistence.save(next)
    }

    suspend fun registerRecoveryAttempt() {
        val next = _state.value.copy(
            recoveryAttempts = _state.value.recoveryAttempts + 1,
            lastRecoveryAtEpochMillis = epochTime()
        )
        _state.value = next
        persistence.save(next)
    }

    fun isPackageAllowed(packageName: String): Boolean =
        !_state.value.enabled ||
            packageName in _state.value.allowedPackages
}
