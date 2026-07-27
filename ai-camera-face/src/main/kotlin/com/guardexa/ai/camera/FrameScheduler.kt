
package com.guardexa.ai.camera

import com.guardexa.ai.core.model.AiPowerState
import java.util.concurrent.atomic.AtomicLong

class FrameScheduler {
    private val lastAcceptedAt = AtomicLong(Long.MIN_VALUE)

    fun shouldAnalyze(
        nowElapsedMillis: Long,
        powerState: AiPowerState
    ): Boolean {
        val interval = when (powerState) {
            AiPowerState.SLEEP -> Long.MAX_VALUE
            AiPowerState.PASSIVE -> 1_000L
            AiPowerState.ACTIVE -> 400L
            AiPowerState.INTENSIVE -> 200L
            AiPowerState.EMERGENCY -> 120L
        }

        if (interval == Long.MAX_VALUE) return false

        while (true) {
            val previous = lastAcceptedAt.get()
            if (previous != Long.MIN_VALUE && nowElapsedMillis - previous < interval) {
                return false
            }
            if (lastAcceptedAt.compareAndSet(previous, nowElapsedMillis)) {
                return true
            }
        }
    }

    fun reset() {
        lastAcceptedAt.set(Long.MIN_VALUE)
    }
}
