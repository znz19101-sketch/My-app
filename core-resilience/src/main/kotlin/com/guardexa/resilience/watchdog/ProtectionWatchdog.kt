
package com.guardexa.resilience.watchdog

import com.guardexa.resilience.model.WatchdogTargetState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface WatchdogTarget {
    val id: String
    suspend fun isHealthy(): Boolean
    suspend fun restart()
}

class ProtectionWatchdog(
    targets: Set<WatchdogTarget>,
    private val elapsedRealtime: () -> Long,
    private val checkIntervalMillis: Long = 5_000L,
    private val minimumRestartIntervalMillis: Long = 10_000L,
    private val maximumRestartAttempts: Int = 3,
    private val onUnrecoverableFailure: suspend (String) -> Unit
) {
    private val targetsById = targets.associateBy { it.id }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _states = MutableStateFlow(
        targets.associate {
            it.id to WatchdogTargetState(
                id = it.id,
                healthy = true,
                lastHeartbeatElapsedMillis = null,
                restartAttempts = 0,
                lastRestartElapsedMillis = null
            )
        }
    )
    val states: StateFlow<Map<String, WatchdogTargetState>> =
        _states.asStateFlow()

    fun start() {
        if (job?.isActive == true) return

        job = scope.launch {
            while (isActive) {
                targetsById.values.forEach { target ->
                    checkTarget(target)
                }
                delay(checkIntervalMillis)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private suspend fun checkTarget(target: WatchdogTarget) {
        val now = elapsedRealtime()
        val current = _states.value[target.id] ?: return

        val healthy = runCatching { target.isHealthy() }
            .getOrDefault(false)

        if (healthy) {
            update(
                current.copy(
                    healthy = true,
                    lastHeartbeatElapsedMillis = now,
                    restartAttempts = 0
                )
            )
            return
        }

        val restartTooSoon =
            current.lastRestartElapsedMillis != null &&
                now - current.lastRestartElapsedMillis <
                minimumRestartIntervalMillis

        if (restartTooSoon) {
            update(current.copy(healthy = false))
            return
        }

        if (current.restartAttempts >= maximumRestartAttempts) {
            update(current.copy(healthy = false))
            onUnrecoverableFailure(target.id)
            return
        }

        runCatching { target.restart() }

        update(
            current.copy(
                healthy = false,
                restartAttempts = current.restartAttempts + 1,
                lastRestartElapsedMillis = now
            )
        )
    }

    private fun update(next: WatchdogTargetState) {
        _states.value = _states.value + (next.id to next)
    }
}
