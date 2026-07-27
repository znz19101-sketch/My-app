
package com.guardexa.ai.core.fusion

import com.guardexa.ai.core.confidence.ConfidencePolicy
import com.guardexa.ai.core.confidence.ConfidenceSnapshot
import com.guardexa.ai.core.model.*

data class FusionPolicy(
    val minimumFramesForDecision: Int = 3,
    val maximumVolatilityForImmediateDecision: Float = 0.04f,
    val spoofFailureThreshold: Float = 0.70f
)

class DecisionFusionEngine(
    private val confidencePolicy: ConfidencePolicy,
    private val fusionPolicy: FusionPolicy = FusionPolicy()
) {
    fun decide(
        evidence: AiFrameEvidence,
        confidence: ConfidenceSnapshot,
        previouslyVerified: Boolean,
        powerState: AiPowerState
    ): VerificationResult {

        val invalidReason = validateInput(evidence)
        if (invalidReason != null) {
            return VerificationResult(
                state = AiVerificationState.INVALID_INPUT,
                confidence = confidence.weightedScore,
                explanationCode = invalidReason,
                usedFrameCount = confidence.validSampleCount,
                powerState = powerState
            )
        }

        if (
            evidence.liveness.status == LivenessStatus.SPOOF_SUSPECTED ||
            evidence.liveness.screenSpoofScore >= fusionPolicy.spoofFailureThreshold
        ) {
            return VerificationResult(
                state = AiVerificationState.FAILED,
                confidence = confidence.weightedScore,
                explanationCode = "spoof_suspected",
                usedFrameCount = confidence.validSampleCount,
                powerState = AiPowerState.EMERGENCY
            )
        }

        if (
            confidence.validSampleCount <
            fusionPolicy.minimumFramesForDecision
        ) {
            return VerificationResult(
                state = AiVerificationState.UNCERTAIN,
                confidence = confidence.weightedScore,
                explanationCode = "collecting_more_evidence",
                usedFrameCount = confidence.validSampleCount,
                powerState = AiPowerState.INTENSIVE
            )
        }

        if (
            confidence.volatility >
            fusionPolicy.maximumVolatilityForImmediateDecision
        ) {
            return VerificationResult(
                state = AiVerificationState.UNCERTAIN,
                confidence = confidence.weightedScore,
                explanationCode = "unstable_result",
                usedFrameCount = confidence.validSampleCount,
                powerState = AiPowerState.INTENSIVE
            )
        }

        val threshold = if (previouslyVerified) {
            confidencePolicy.keepOpenThreshold
        } else {
            confidencePolicy.openThreshold
        }

        return when {
            confidence.weightedScore >= threshold ->
                VerificationResult(
                    state = AiVerificationState.VERIFIED,
                    confidence = confidence.weightedScore,
                    explanationCode = "glasses_verified",
                    usedFrameCount = confidence.validSampleCount,
                    powerState = AiPowerState.PASSIVE
                )

            confidence.weightedScore >= confidencePolicy.uncertainThreshold ->
                VerificationResult(
                    state = AiVerificationState.UNCERTAIN,
                    confidence = confidence.weightedScore,
                    explanationCode = "verification_uncertain",
                    usedFrameCount = confidence.validSampleCount,
                    powerState = AiPowerState.INTENSIVE
                )

            else ->
                VerificationResult(
                    state = AiVerificationState.FAILED,
                    confidence = confidence.weightedScore,
                    explanationCode = "glasses_not_detected",
                    usedFrameCount = confidence.validSampleCount,
                    powerState = AiPowerState.ACTIVE
                )
        }
    }

    private fun validateInput(
        evidence: AiFrameEvidence
    ): String? {
        if (!evidence.face.faceDetected) return "face_not_found"
        if (evidence.face.faceCount > 1) return "multiple_faces"
        if (evidence.face.qualityIssue == FaceQualityIssue.LOW_LIGHT) {
            return "low_light"
        }
        if (evidence.face.qualityIssue == FaceQualityIssue.CAMERA_OBSTRUCTED) {
            return "camera_obstructed"
        }
        if (evidence.face.qualityIssue == FaceQualityIssue.MOTION_BLUR) {
            return "hold_still"
        }
        if (evidence.face.eyeVisibilityScore < 0.45f) {
            return "eyes_not_visible"
        }
        return null
    }
}
