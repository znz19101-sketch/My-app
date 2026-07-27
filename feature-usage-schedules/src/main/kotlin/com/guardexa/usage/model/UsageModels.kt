
package com.guardexa.usage.model

enum class BonusTargetType {
    DEVICE,
    APPLICATION
}

enum class SessionEndReason {
    APP_CLOSED,
    DAILY_LIMIT_REACHED,
    SCHEDULE_ENDED,
    GLASSES_REMOVED,
    PARENT_ACTION,
    DEVICE_LOCKED,
    SYSTEM_RESTART,
    UNKNOWN
}

data class ScheduleWindow(
    val id: String,
    val profileId: String,
    val name: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val daysOfWeekMask: Int,
    val enabled: Boolean,
    val priority: Int
) {
    init {
        require(startMinuteOfDay in 0..1439)
        require(endMinuteOfDay in 0..1439)
        require(daysOfWeekMask in 0..127)
    }

    fun includesDay(dayOfWeekIndex: Int): Boolean {
        require(dayOfWeekIndex in 0..6)
        return daysOfWeekMask and (1 shl dayOfWeekIndex) != 0
    }

    fun containsMinute(minuteOfDay: Int): Boolean {
        require(minuteOfDay in 0..1439)
        return if (startMinuteOfDay <= endMinuteOfDay) {
            minuteOfDay in startMinuteOfDay until endMinuteOfDay
        } else {
            minuteOfDay >= startMinuteOfDay || minuteOfDay < endMinuteOfDay
        }
    }
}

data class DailyUsage(
    val usageDateKey: String,
    val packageName: String?,
    val profileId: String,
    val usedDurationMillis: Long,
    val lastSessionStartElapsedMillis: Long?,
    val lastCalculatedAtEpochMillis: Long
)

data class UsageSession(
    val id: String,
    val packageName: String,
    val startedAtEpochMillis: Long,
    val startedAtElapsedMillis: Long,
    val endedAtEpochMillis: Long?,
    val endedAtElapsedMillis: Long?,
    val durationMillis: Long,
    val profileId: String,
    val glassesRequired: Boolean,
    val glassesConfirmed: Boolean,
    val endReason: SessionEndReason?
)

data class BonusTimeGrant(
    val id: String,
    val targetType: BonusTargetType,
    val targetPackageName: String?,
    val durationMillis: Long,
    val grantedAtEpochMillis: Long,
    val expiresAtEpochMillis: Long?,
    val reason: String?,
    val active: Boolean
) {
    init {
        require(durationMillis > 0)
        require(
            targetType != BonusTargetType.APPLICATION ||
                !targetPackageName.isNullOrBlank()
        )
    }

    fun appliesTo(packageName: String): Boolean =
        active && (
            targetType == BonusTargetType.DEVICE ||
                targetPackageName == packageName
            )

    fun isExpired(nowEpochMillis: Long): Boolean =
        expiresAtEpochMillis?.let { nowEpochMillis >= it } == true
}

data class UsageLimitSnapshot(
    val baseLimitMillis: Long?,
    val usedMillis: Long,
    val bonusMillis: Long,
    val remainingMillis: Long?,
    val exhausted: Boolean
)
