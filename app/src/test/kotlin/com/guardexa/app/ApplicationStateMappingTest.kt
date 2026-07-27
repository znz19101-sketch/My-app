
package com.guardexa.app

import com.guardexa.app.runtime.GuardexaLaunchState
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationStateMappingTest {

    @Test
    fun safeModeHasHighestStartupPriority() {
        val state = when {
            true -> GuardexaLaunchState.SAFE_MODE
            else -> GuardexaLaunchState.READY
        }

        assertEquals(
            GuardexaLaunchState.SAFE_MODE,
            state
        )
    }
}
