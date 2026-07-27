
package com.guardexa.notifications

enum class NotificationLevel {
    INFO,
    ALERT,
    ATTENTION,
    URGENT
}

data class GuardexaNotification(
    val id: String,
    val level: NotificationLevel,
    val category: String,
    val title: String,
    val message: String,
    val createdAtEpochMillis: Long,
    val groupKey: String?,
    val sensitive: Boolean
)

data class QuietHours(
    val enabled: Boolean,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int
) {
    init {
        require(startMinuteOfDay in 0..1439)
        require(endMinuteOfDay in 0..1439)
    }

    fun activeAt(minuteOfDay: Int): Boolean {
        if (!enabled) return false

        return if (startMinuteOfDay <= endMinuteOfDay) {
            minuteOfDay in startMinuteOfDay until endMinuteOfDay
        } else {
            minuteOfDay >= startMinuteOfDay ||
                minuteOfDay < endMinuteOfDay
        }
    }
}

data class NotificationDigest(
    val groupKey: String,
    val count: Int,
    val highestLevel: NotificationLevel,
    val title: String,
    val message: String
)
