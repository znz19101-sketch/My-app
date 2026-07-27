
package com.guardexa.core.policy.time

import com.guardexa.core.policy.model.FacePresenceStatus

data class PresencePolicy(
    val shortAbsenceToleranceMillis: Long = 2_500L,
    val startGraceAfterMillis: Long = 8_000L,
    val blockAfterMillis: Long = 38_000L
) {
    init {
        require(shortAbsenceToleranceMillis >= 0)
        require(startGraceAfterMillis >= shortAbsenceToleranceMillis)
        require(blockAfterMillis >= startGraceAfterMillis)
    }
}

data class PresenceSnapshot(
    val faceVisible: Boolean,
    val lastFaceSeenElapsedMillis: Long?,
    val nowElapsedMillis: Long
)

class PresenceContinuityEngine(
    private val policy: PresencePolicy = PresencePolicy()
) {
    fun evaluate(snapshot: PresenceSnapshot): FacePresenceStatus {
        if (snapshot.faceVisible) return FacePresenceStatus.PRESENT

        val lastSeen = snapshot.lastFaceSeenElapsedMillis
            ?: return FacePresenceStatus.MISSING_LONG_ENOUGH_FOR_GRACE

        val missingFor = (snapshot.nowElapsedMillis - lastSeen).coerceAtLeast(0L)

        return when {
            missingFor <= policy.shortAbsenceToleranceMillis ->
                FacePresenceStatus.TEMPORARILY_MISSING

            missingFor < policy.startGraceAfterMillis ->
                FacePresenceStatus.TEMPORARILY_MISSING

            missingFor < policy.blockAfterMillis ->
                FacePresenceStatus.MISSING_LONG_ENOUGH_FOR_GRACE

            else ->
                FacePresenceStatus.MISSING_AND_GRACE_EXPIRED
        }
    }
}
