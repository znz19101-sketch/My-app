
package com.guardexa.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

class AndroidNotificationPublisher(
    private val context: Context
) {
    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannels() {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_INFO,
                "Guardexa information",
                NotificationManager.IMPORTANCE_LOW
            ),
            NotificationChannel(
                CHANNEL_ALERT,
                "Guardexa alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            NotificationChannel(
                CHANNEL_URGENT,
                "Guardexa urgent",
                NotificationManager.IMPORTANCE_HIGH
            )
        )

        manager.createNotificationChannels(channels)
    }

    fun publish(
        notificationId: Int,
        notification: GuardexaNotification
    ) {
        val channel = when (notification.level) {
            NotificationLevel.INFO -> CHANNEL_INFO
            NotificationLevel.ALERT,
            NotificationLevel.ATTENTION -> CHANNEL_ALERT
            NotificationLevel.URGENT -> CHANNEL_URGENT
        }

        val built = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_secure)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        manager.notify(notificationId, built)
    }

    companion object {
        const val CHANNEL_INFO = "guardexa_info"
        const val CHANNEL_ALERT = "guardexa_alert"
        const val CHANNEL_URGENT = "guardexa_urgent"
    }
}
