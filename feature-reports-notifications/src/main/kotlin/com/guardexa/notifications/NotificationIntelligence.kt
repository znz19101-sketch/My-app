
package com.guardexa.notifications

class NotificationIntelligence {

    fun shouldDeliver(
        notification: GuardexaNotification,
        quietHours: QuietHours,
        minuteOfDay: Int
    ): Boolean {
        if (!quietHours.activeAt(minuteOfDay)) return true

        return notification.level == NotificationLevel.URGENT
    }

    fun privacySafeCopy(
        notification: GuardexaNotification,
        privacyMode: Boolean
    ): GuardexaNotification {
        if (!privacyMode || !notification.sensitive) return notification

        return notification.copy(
            title = "Guardexa alert",
            message = "A protected event requires review."
        )
    }

    fun digest(
        notifications: List<GuardexaNotification>
    ): List<NotificationDigest> =
        notifications
            .groupBy { it.groupKey ?: it.category }
            .map { (groupKey, items) ->
                val highest = items.maxByOrNull { it.level.ordinal }
                    ?.level
                    ?: NotificationLevel.INFO

                NotificationDigest(
                    groupKey = groupKey,
                    count = items.size,
                    highestLevel = highest,
                    title = if (items.size == 1) {
                        items.first().title
                    } else {
                        "${items.size} Guardexa events"
                    },
                    message = if (items.size == 1) {
                        items.first().message
                    } else {
                        "Multiple related events are ready for review."
                    }
                )
            }
            .sortedByDescending { it.highestLevel.ordinal }
}
