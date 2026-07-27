
package com.guardexa.ai.core

import com.guardexa.ai.core.confidence.*
import com.guardexa.ai.core.fusion.*
import com.guardexa.ai.core.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class DecisionFusionEngineTest {

    private val confidencePolicy = ConfidencePolicy()
    private val engine = DecisionFusionEngine(confidencePolicy)

    @Test
    fun highStableConfidenceVerifiesGlasses() {
        val result = engine.decide(
            evidence = validEvidence(),
            confidence = ConfidenceSnapshot(
                weightedScore = 0.90f,
                validSampleCount = 5,
                trend = 0f,
                volatility = 0.01f
            ),
            previouslyVerified = false,
            powerState = AiPowerState.ACTIVE
        )

        assertEquals(AiVerificationState.VERIFIED, result.state)
    }

    @Test
    fun spoofSuspicionFailsImmediately() {
        val evidence = validEvidence().copy(
            liveness = validEvidence().liveness.copy(
                status = LivenessStatus.SPOOF_SUSPECTED,
                screenSpoofScore = 0.95f
            )
        )

        val result = engine.decide(
            evidence = evidence,
            confidence = ConfidenceSnapshot(
                weightedScore = 0.90f,
                validSampleCount = 5,
                trend = 0f,
                volatility = 0.01f
            ),
            previouslyVerified = true,
            powerState = AiPowerState.ACTIVE
        )

        assertEquals(AiVerificationState.FAILED, result.state)
        assertEquals(AiPowerState.EMERGENCY, result.powerState)
    }

    @Test
    fun uncertainInputRequestsMoreEvidence() {
        val result = engine.decide(
            evidence = validEvidence(),
            confidence = ConfidenceSnapshot(
                weightedScore = 0.70f,
                validSampleCount = 1,
                trend = 0f,
                volatility = 0.01f
            ),
            previouslyVerified = false,
            powerState = AiPowerState.ACTIVE
        )

        assertEquals(AiVerificationState.UNCERTAIN, result.state)
    }

    private fun validEvidence() = AiFrameEvidence(
        metadata = FrameMetadata(
            frameId = 1L,
            timestampElapsedMillis = 1_000L,
            width = 640,
            height = 480,
            rotationDegrees = 0,
            brightnessScore = 0.75f,
            sharpnessScore = 0.80f,
            motionScore = 0.10f
        ),
        face = FaceAnalysisResult(
            faceDetected = true,
            faceCount = 1,
            faceConfidence = 0.95f,
            eyeVisibilityScore = 0.90f,
            headPoseScore = 0.85f,
            faceSizeScore = 0.80f,
            qualityIssue = FaceQualityIssue.NONE
        ),
        glasses = GlassesAnalysisResult(
            predictedClass = GlassesClass.PRESCRIPTION_GLASSES,
            prescriptionGlassesScore = 0.92f,
            noGlassesScore = 0.05f,
            geometryScore = 0.90f,
            bridgeScore = 0.88f,
            templeArmScore = 0.82f,
            reflectionScore = 0.70f
        ),
        liveness = LivenessAnalysisResult(
            status = LivenessStatus.LIVE,
            score = 0.90f,
            blinkScore = 0.85f,
            motionConsistencyScore = 0.90f,
            screenSpoofScore = 0.05f
        )
    )
}
