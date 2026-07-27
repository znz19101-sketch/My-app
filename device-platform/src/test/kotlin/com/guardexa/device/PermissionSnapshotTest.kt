
package com.guardexa.device

import com.guardexa.device.permissions.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionSnapshotTest {

    @Test
    fun criticalPermissionsMustBothBeGranted() {
        val healthy = PermissionSnapshot(
            mapOf(
                GuardexaPermission.CAMERA to PermissionHealth.GRANTED,
                GuardexaPermission.USAGE_ACCESS to PermissionHealth.GRANTED
            )
        )
        assertTrue(healthy.criticalHealthy())

        val unhealthy = PermissionSnapshot(
            mapOf(
                GuardexaPermission.CAMERA to PermissionHealth.GRANTED,
                GuardexaPermission.USAGE_ACCESS to PermissionHealth.REQUIRES_SETTINGS
            )
        )
        assertFalse(unhealthy.criticalHealthy())
    }
}
