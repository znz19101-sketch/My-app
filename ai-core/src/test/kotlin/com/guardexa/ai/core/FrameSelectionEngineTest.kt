
package com.guardexa.ai.core

import com.guardexa.ai.core.model.FrameMetadata
import com.guardexa.ai.core.runtime.*
import kotlin.test.Test
import kotlin.test.assertIs

class FrameSelectionEngineTest {

    private val engine = FrameSelectionEngine()

    @Test
    fun clearFrameIsAccepted() {
        assertIs<FrameSelectionResult.Accepted>(
            engine.evaluate(
                FrameMetadata(
                    frameId = 1,
                    timestampElapsedMillis = 1,
                    width = 640,
                    height = 480,
                    rotationDegrees = 0,
                    brightnessScore = 0.7f,
                    sharpnessScore = 0.8f,
                    motionScore = 0.2f
                )
            )
        )
    }

    @Test
    fun darkFrameIsRejected() {
        assertIs<FrameSelectionResult.Rejected>(
            engine.evaluate(
                FrameMetadata(
                    frameId = 1,
                    timestampElapsedMillis = 1,
                    width = 640,
                    height = 480,
                    rotationDegrees = 0,
                    brightnessScore = 0.05f,
                    sharpnessScore = 0.8f,
                    motionScore = 0.2f
                )
            )
        )
    }
}
