
package com.guardexa.device.permissions

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GuardexaPermission {
    CAMERA,
    NOTIFICATIONS,
    USAGE_ACCESS,
    OVERLAY,
    DEVICE_ADMIN,
    BATTERY_OPTIMIZATION_EXEMPTION
}

enum class PermissionHealth {
    GRANTED,
    DENIED,
    REQUIRES_SETTINGS,
    NOT_SUPPORTED
}

data class PermissionSnapshot(
    val values: Map<GuardexaPermission, PermissionHealth>
) {
    fun criticalHealthy(): Boolean =
        values[GuardexaPermission.CAMERA] == PermissionHealth.GRANTED &&
            values[GuardexaPermission.USAGE_ACCESS] == PermissionHealth.GRANTED
}

class PermissionOrchestrator(
    private val context: Context
) {
    private val _snapshot = MutableStateFlow(readSnapshot())
    val snapshot: StateFlow<PermissionSnapshot> = _snapshot.asStateFlow()

    fun refresh(): PermissionSnapshot =
        readSnapshot().also { _snapshot.value = it }

    fun runtimePermissionsToRequest(): Array<String> =
        buildList {
            if (cameraHealth() != PermissionHealth.GRANTED) {
                add(Manifest.permission.CAMERA)
            }
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                notificationHealth() != PermissionHealth.GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

    fun settingsIntent(permission: GuardexaPermission): Intent? =
        when (permission) {
            GuardexaPermission.USAGE_ACCESS ->
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

            GuardexaPermission.OVERLAY ->
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                )

            GuardexaPermission.BATTERY_OPTIMIZATION_EXEMPTION ->
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

            GuardexaPermission.DEVICE_ADMIN ->
                null

            GuardexaPermission.CAMERA,
            GuardexaPermission.NOTIFICATIONS ->
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:${context.packageName}")
                )
        }

    private fun readSnapshot(): PermissionSnapshot =
        PermissionSnapshot(
            values = mapOf(
                GuardexaPermission.CAMERA to cameraHealth(),
                GuardexaPermission.NOTIFICATIONS to notificationHealth(),
                GuardexaPermission.USAGE_ACCESS to usageAccessHealth(),
                GuardexaPermission.OVERLAY to overlayHealth(),
                GuardexaPermission.DEVICE_ADMIN to PermissionHealth.REQUIRES_SETTINGS,
                GuardexaPermission.BATTERY_OPTIMIZATION_EXEMPTION to PermissionHealth.REQUIRES_SETTINGS
            )
        )

    private fun cameraHealth(): PermissionHealth =
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) PermissionHealth.GRANTED else PermissionHealth.DENIED

    private fun notificationHealth(): PermissionHealth {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                PermissionHealth.GRANTED
            } else {
                PermissionHealth.REQUIRES_SETTINGS
            }
        }

        return if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) PermissionHealth.GRANTED else PermissionHealth.DENIED
    }

    private fun usageAccessHealth(): PermissionHealth {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }

        return if (mode == AppOpsManager.MODE_ALLOWED) {
            PermissionHealth.GRANTED
        } else {
            PermissionHealth.REQUIRES_SETTINGS
        }
    }

    private fun overlayHealth(): PermissionHealth =
        if (Settings.canDrawOverlays(context)) {
            PermissionHealth.GRANTED
        } else {
            PermissionHealth.REQUIRES_SETTINGS
        }
}
