
package com.guardexa.usage.engine

import com.guardexa.usage.model.*
import java.util.UUID

class UsageAccountingEngine(
    private val epochTime: () -> Long,
    private val elapsedRealtime: () -> Long
) {
    fun startSession(
        packageName: String,
        profileId: String,
        glassesRequired: Boolean,
        glassesConfirmed: Boolean
    ): UsageSession =
        UsageSession(
            id = UUID.randomUUID().toString(),
            packageName = packageName,
            startedAtEpochMillis = epochTime(),
            startedAtElapsedMillis = elapsedRealtime(),
            endedAtEpochMillis = null,
            endedAtElapsedMillis = null,
            durationMillis = 0L,
            profileId = profileId,
            glassesRequired = glassesRequired,
            glassesConfirmed = glassesConfirmed,
            endReason = null
        )

    fun updateDuration(session: UsageSession): UsageSession {
        if (session.endedAtElapsedMillis != null) return session

        val nowElapsed = elapsedRealtime()
        val duration = (
            nowElapsed - session.startedAtElapsedMillis
        ).coerceAtLeast(0L)

        return session.copy(durationMillis = duration)
    }

    fun endSession(
        session: UsageSession,
        reason: SessionEndReason
    ): UsageSession {
        val nowEpoch = epochTime()
        val nowElapsed = elapsedRealtime()
        val duration = (
            nowElapsed - session.startedAtElapsedMillis
        ).coerceAtLeast(0L)

        return session.copy(
            endedAtEpochMillis = nowEpoch,
            endedAtElapsedMillis = nowElapsed,
            durationMillis = duration,
            endReason = reason
        )
    }

    fun calculateLimit(
        baseLimitMillis: Long?,
        usedMillis: Long,
        bonusGrants: List<BonusTimeGrant>,
        packageName: String,
        nowEpochMillis: Long
    ): UsageLimitSnapshot {
        val activeBonus = bonusGrants
            .filter { it.appliesTo(packageName) }
            .filterNot { it.isExpired(nowEpochMillis) }
            .sumOf { it.durationMillis }

        if (baseLimitMillis == null) {
            return UsageLimitSnapshot(
                baseLimitMillis = null,
                usedMillis = usedMillis,
                bonusMillis = activeBonus,
                remainingMillis = null,
                exhausted = false
            )
        }

        val totalAllowed = baseLimitMillis + activeBonus
        val remaining = (totalAllowed - usedMillis).coerceAtLeast(0L)

        return UsageLimitSnapshot(
            baseLimitMillis = baseLimitMillis,
            usedMillis = usedMillis,
            bonusMillis = activeBonus,
            remainingMillis = remaining,
            exhausted = remaining == 0L
        )
    }

    fun recoverInterruptedSession(
        session: UsageSession,
        lastPersistedDurationMillis: Long
    ): UsageSession {
        if (session.endedAtEpochMillis != null) return session

        return session.copy(
            endedAtEpochMillis = epochTime(),
            endedAtElapsedMillis = elapsedRealtime(),
            durationMillis = maxOf(
                session.durationMillis,
                lastPersistedDurationMillis
            ),
            endReason = SessionEndReason.SYSTEM_RESTART
        )
    }
}
