
package com.guardexa.core.policy

import com.guardexa.core.policy.model.FacePresenceStatus
import com.guardexa.core.policy.time.*
import kotlin.test.Test
import kotlin.test.assertEquals

class PresenceContinuityEngineTest {

    private val engine = PresenceContinuityEngine(
        PresencePolicy(
            shortAbsenceToleranceMillis = 2_000L,
            startGraceAfterMillis = 8_000L,
            blockAfterMillis = 38_000L
        )
    )

    @Test
    fun visibleFaceIsPresent() {
        assertEquals(
            FacePresenceStatus.PRESENT,
            engine.evaluate(
                PresenceSnapshot(
                    faceVisible = true,
                    lastFaceSeenElapsedMillis = 0L,
                    nowElapsedMillis = 100L
                )
            )
        )
    }

    @Test
    fun shortAbsenceIsTolerated() {
        assertEquals(
            FacePresenceStatus.TEMPORARILY_MISSING,
            engine.evaluate(
                PresenceSnapshot(
                    faceVisible = false,
                    lastFaceSeenElapsedMillis = 1_000L,
                    nowElapsedMillis = 2_500L
                )
            )
        )
    }

    @Test
    fun longAbsenceStartsGrace() {
        assertEquals(
            FacePresenceStatus.MISSING_LONG_ENOUGH_FOR_GRACE,
            engine.evaluate(
                PresenceSnapshot(
                    faceVisible = false,
                    lastFaceSeenElapsedMillis = 1_000L,
                    nowElapsedMillis = 15_000L
                )
            )
        )
    }

    @Test
    fun veryLongAbsenceExpiresGrace() {
        assertEquals(
            FacePresenceStatus.MISSING_AND_GRACE_EXPIRED,
            engine.evaluate(
                PresenceSnapshot(
                    faceVisible = false,
                    lastFaceSeenElapsedMillis = 1_000L,
                    nowElapsedMillis = 50_000L
                )
            )
        )
    }
}
