
package com.guardexa.device.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ForegroundAppState(
    val packageName: String?,
    val changedAtEpochMillis: Long,
    val monitorHealthy: Boolean
)

class ForegroundAppMonitor(
    context: Context,
    private val epochTime: () -> Long = { System.currentTimeMillis() },
    private val pollingIntervalMillis: Long = 750L
) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _state = MutableStateFlow(
        ForegroundAppState(
            packageName = null,
            changedAtEpochMillis = epochTime(),
            monitorHealthy = true
        )
    )
    val state: StateFlow<ForegroundAppState> = _state.asStateFlow()

    fun start() {
        if (job?.isActive == true) return

        job = scope.launch {
            while (isActive) {
                runCatching { resolveForegroundPackage() }
                    .onSuccess { packageName ->
                        val old = _state.value
                        if (old.packageName != packageName) {
                            _state.value = ForegroundAppState(
                                packageName = packageName,
                                changedAtEpochMillis = epochTime(),
                                monitorHealthy = true
                            )
                        }
                    }
                    .onFailure {
                        _state.value = _state.value.copy(
                            monitorHealthy = false
                        )
                    }

                delay(pollingIntervalMillis)
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

    private fun resolveForegroundPackage(): String? {
        val end = epochTime()
        val begin = end - 10_000L
        val events = usageStatsManager.queryEvents(begin, end)
        val event = UsageEvents.Event()

        var latestPackage: String? = null
        var latestTimestamp = Long.MIN_VALUE

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val foreground = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> true
                else -> false
            }

            if (foreground && event.timeStamp >= latestTimestamp) {
                latestTimestamp = event.timeStamp
                latestPackage = event.packageName
            }
        }

        return latestPackage
    }
}
