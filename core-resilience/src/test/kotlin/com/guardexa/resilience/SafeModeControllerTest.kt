
package com.guardexa.resilience

import com.guardexa.resilience.model.*
import com.guardexa.resilience.safemode.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafeModeControllerTest {

    @Test
    fun safeModeAllowsOnlyConfiguredPackages() = kotlinx.coroutines.test.runTest {
        val persistence = object : SafeModePersistence {
            var stored = SafeModeState(
                enabled = false,
                reason = null,
                enabledAtEpochMillis = null,
                allowedPackages = emptySet(),
                recoveryAttempts = 0,
                lastRecoveryAtEpochMillis = null
            )

            override suspend fun load() = stored
            override suspend fun save(state: SafeModeState) {
                stored = state
            }
        }

        val controller = SafeModeController(
            persistence = persistence,
            epochTime = { 1_000L },
            emergencyPackages = setOf("com.phone"),
            systemPackages = setOf("com.settings")
        )

        controller.enable(SafeModeReason.CRITICAL_PERMISSION_MISSING)

        assertTrue(controller.isPackageAllowed("com.phone"))
        assertFalse(controller.isPackageAllowed("com.game"))
    }
}
