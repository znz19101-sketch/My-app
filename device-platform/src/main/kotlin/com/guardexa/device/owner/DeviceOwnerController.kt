
package com.guardexa.device.owner

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

enum class DeviceOwnerCapability {
    NOT_DEVICE_OWNER,
    DEVICE_OWNER_ACTIVE,
    ADMIN_ACTIVE_ONLY
}

class DeviceOwnerController(
    private val context: Context,
    private val adminReceiver: ComponentName
) {
    private val manager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun capability(): DeviceOwnerCapability =
        when {
            manager.isDeviceOwnerApp(context.packageName) ->
                DeviceOwnerCapability.DEVICE_OWNER_ACTIVE

            manager.isAdminActive(adminReceiver) ->
                DeviceOwnerCapability.ADMIN_ACTIVE_ONLY

            else ->
                DeviceOwnerCapability.NOT_DEVICE_OWNER
        }

    fun setLockTaskPackages(packages: Set<String>) {
        check(manager.isDeviceOwnerApp(context.packageName)) {
            "Device Owner mode is required"
        }

        val safePackages = packages
            .filter { it.isNotBlank() }
            .plus(context.packageName)
            .distinct()
            .toTypedArray()

        manager.setLockTaskPackages(
            adminReceiver,
            safePackages
        )
    }

    fun clearLockTaskPackages() {
        if (!manager.isDeviceOwnerApp(context.packageName)) return
        manager.setLockTaskPackages(adminReceiver, emptyArray())
    }

    fun isLockTaskPermitted(packageName: String): Boolean =
        manager.isLockTaskPermitted(packageName)
}
