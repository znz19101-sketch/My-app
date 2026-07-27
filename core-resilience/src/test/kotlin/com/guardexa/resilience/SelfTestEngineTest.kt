
package com.guardexa.resilience

import com.guardexa.resilience.model.*
import com.guardexa.resilience.selftest.*
import kotlin.test.Test
import kotlin.test.assertTrue

class SelfTestEngineTest {

    @Test
    fun safeModeIsRequiredWhenAnyCheckRequiresIt() =
        kotlinx.coroutines.test.runTest {
            val check = object : SelfTestCheck {
                override val component = SelfTestComponent.DATABASE

                override suspend fun run() =
                    ComponentSelfTestResult(
                        component = component,
                        health = ComponentHealth.FAILED,
                        severity = SelfTestSeverity.SAFE_MODE_REQUIRED,
                        reasonCode = "database_unavailable",
                        recoverable = true,
                        checkedAtEpochMillis = 1L
                    )
            }

            val engine = SelfTestEngine(
                checks = setOf(check),
                epochTime = { 1L }
            )

            assertTrue(engine.runFull().safeModeRequired)
        }
}
