
package com.guardexa.device.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.guardexa.device.service.ProtectionForegroundService

interface BootProtectionState {
    suspend fun shouldStartProtection(): Boolean
}

class BootReceiver : BroadcastReceiver() {

    lateinit var bootProtectionState: BootProtectionState

    override fun onReceive(context: Context, intent: Intent) {
        val supported = intent.action in setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (!supported) return

        val pendingResult = goAsync()

        kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() +
                kotlinx.coroutines.Dispatchers.Default
        ).launch {
            try {
                if (bootProtectionState.shouldStartProtection()) {
                    ProtectionForegroundService.start(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
