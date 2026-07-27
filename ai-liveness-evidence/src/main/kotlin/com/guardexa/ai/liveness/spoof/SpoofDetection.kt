
package com.guardexa.ai.liveness.spoof

import kotlin.math.abs

data class SpoofSignals(
    val screenMoireScore: Float,
    val flatPerspectiveScore: Float,
    val replayLoopScore: Float,
    val staticReflectionScore: Float,
    val unnaturalMotionScore: Float,
    val passiveLivenessScore: Float
)

data class SpoofAssessment(
    val spoofScore: Float,
    val suspected: Boolean,
    val reasonCode: String?
)

class SpoofDetectionEngine(
    private val suspicionThreshold: Float = 0.70f
) {
    fun assess(signals: SpoofSignals): SpoofAssessment {
        val score = (
            signals.screenMoireScore.coerceIn(0f, 1f) * 0.25f +
                signals.flatPerspectiveScore.coerceIn(0f, 1f) * 0.20f +
                signals.replayLoopScore.coerceIn(0f, 1f) * 0.20f +
                signals.staticReflectionScore.coerceIn(0f, 1f) * 0.10f +
                signals.unnaturalMotionScore.coerceIn(0f, 1f) * 0.15f +
                (1f - signals.passiveLivenessScore.coerceIn(0f, 1f)) * 0.10f
            ).coerceIn(0f, 1f)

        val reason = when {
            signals.screenMoireScore >= 0.80f -> "screen_pattern_detected"
            signals.replayLoopScore >= 0.80f -> "replay_suspected"
            signals.flatPerspectiveScore >= 0.80f -> "flat_image_suspected"
            signals.unnaturalMotionScore >= 0.80f -> "unnatural_motion"
            score >= suspicionThreshold -> "combined_spoof_suspicion"
            else -> null
        }

        return SpoofAssessment(
            spoofScore = score,
            suspected = score >= suspicionThreshold,
            reasonCode = reason
        )
    }
}
