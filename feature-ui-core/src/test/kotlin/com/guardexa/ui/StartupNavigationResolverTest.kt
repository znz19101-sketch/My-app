
package com.guardexa.ui

import com.guardexa.ui.navigation.*
import kotlin.test.Test
import kotlin.test.assertEquals

class StartupNavigationResolverTest {

    private val resolver = StartupNavigationResolver()

    @Test
    fun safeModeAlwaysOpensSafeModeScreen() {
        val result = resolver.resolve(ApplicationLaunchState.SAFE_MODE)
        assertEquals(GuardexaDestination.SafeMode, result.destination)
    }

    @Test
    fun unconfiguredAppOpensSetup() {
        val result = resolver.resolve(ApplicationLaunchState.NOT_CONFIGURED)
        assertEquals(GuardexaDestination.Setup, result.destination)
    }
}
