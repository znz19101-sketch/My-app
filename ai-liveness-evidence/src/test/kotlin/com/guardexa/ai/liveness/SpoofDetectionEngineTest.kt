
package com.guardexa.ai.liveness

import com.guardexa.ai.liveness.spoof.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpoofDetectionEngineTest {

    private val engine = SpoofDetectionEngine()

    @Test
    fun strongScreenPatternTriggersSuspicion() {
        val result = engine.assess(
            SpoofSignals(
                screenMoireScore = 0.95f,
                flatPerspectiveScore = 0.60f,
                replayLoopScore = 0.30f,
                staticReflectionScore = 0.50f,
                unnaturalMotionScore = 0.20f,
                passiveLivenessScore = 0.20f
            )
        )

        assertTrue(result.suspected)
    }

    @Test
    fun naturalSignalsRemainBelowThreshold() {
        val result = engine.assess(
            SpoofSignals(
                screenMoireScore = 0.10f,
                flatPerspectiveScore = 0.10f,
                replayLoopScore = 0.05f,
                staticReflectionScore = 0.10f,
                unnaturalMotionScore = 0.05f,
                passiveLivenessScore = 0.90f
            )
        )

        assertFalse(result.suspected)
    }
}
