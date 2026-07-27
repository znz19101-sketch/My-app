
package com.guardexa.ai.camera

import com.guardexa.ai.core.model.AiPowerState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FrameSchedulerTest {

    @Test
    fun passiveModeUsesLongerInterval() {
        val scheduler = FrameScheduler()

        assertTrue(scheduler.shouldAnalyze(1_000L, AiPowerState.PASSIVE))
        assertFalse(scheduler.shouldAnalyze(1_500L, AiPowerState.PASSIVE))
        assertTrue(scheduler.shouldAnalyze(2_100L, AiPowerState.PASSIVE))
    }

    @Test
    fun emergencyModeAcceptsFramesMoreFrequently() {
        val scheduler = FrameScheduler()

        assertTrue(scheduler.shouldAnalyze(1_000L, AiPowerState.EMERGENCY))
        assertFalse(scheduler.shouldAnalyze(1_050L, AiPowerState.EMERGENCY))
        assertTrue(scheduler.shouldAnalyze(1_130L, AiPowerState.EMERGENCY))
    }

    @Test
    fun sleepModeRejectsAllFrames() {
        val scheduler = FrameScheduler()
        assertFalse(scheduler.shouldAnalyze(1_000L, AiPowerState.SLEEP))
    }
}
