
package com.guardexa.device.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.guardexa.device.usage.ForegroundAppMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

interface ProtectionRuntime {
    suspend fun onForegroundPackageChanged(packageName: String?)
    suspend fun onServiceStarted()
    suspend fun onServiceStopped()
}

class ProtectionForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var appMonitor: ForegroundAppMonitor
    lateinit var protectionRuntime: ProtectionRuntime

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            buildNotification()
        )

        scope.launch {
            protectionRuntime.onServiceStarted()
        }

        appMonitor.start()

        scope.launch {
            appMonitor.state.collectLatest { state ->
                protectionRuntime.onForegroundPackageChanged(
                    state.packageName
                )
            }
        }
    }

    override fun onDestroy() {
        appMonitor.stop()
        scope.launch {
            protectionRuntime.onServiceStopped()
        }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Guardexa protection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows that parental protection is active."
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_secure)
            .setContentTitle("Guardexa is protecting this device")
            .setContentText("Parental protection is active.")
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    companion object {
        private const val CHANNEL_ID = "guardexa_protection"
        private const val NOTIFICATION_ID = 2101

        fun start(context: Context) {
            val intent = Intent(
                context,
                ProtectionForegroundService::class.java
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(
                Intent(
                    context,
                    ProtectionForegroundService::class.java
                )
            )
        }
    }
}
