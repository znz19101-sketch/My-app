
package com.guardexa.core.security

import com.guardexa.core.security.pin.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PinSecurityTest {

    @Test
    fun validPinCanBeVerified() {
        val policy = PinPolicy()
        val hasher = PinHasher()
        val credential = hasher.createCredential(
            pin = "123456".toCharArray(),
            policy = policy,
            nowEpochMillis = 1L
        )

        assertTrue(
            hasher.verify(
                pin = "123456".toCharArray(),
                credential = credential
            )
        )

        assertFalse(
            hasher.verify(
                pin = "654321".toCharArray(),
                credential = credential
            )
        )
    }

    @Test
    fun fiveFailedAttemptsTriggerThirtySecondLockout() {
        var now = 0L
        val manager = PinAttemptManager(
            policy = PinPolicy(),
            elapsedRealtime = { now }
        )

        repeat(4) {
            assertIs<PinAttemptResult.Rejected>(
                manager.onFailedAuthentication()
            )
        }

        val locked = manager.onFailedAuthentication()
        assertIs<PinAttemptResult.Locked>(locked)
        assertEquals(30_000L, locked.remainingMillis)

        now = 30_001L
        assertFalse(manager.currentState().isLocked(now))
    }
}
