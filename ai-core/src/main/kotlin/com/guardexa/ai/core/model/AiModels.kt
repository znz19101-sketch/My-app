
package com.guardexa.ai.core.model

enum class GlassesClass {
    PRESCRIPTION_GLASSES,
    NO_GLASSES,
    SUNGLASSES,
    OCCLUDED,
    INVALID_REGION,
    UNKNOWN
}

enum class FaceQualityIssue {
    NONE,
    NO_FACE,
    MULTIPLE_FACES,
    FACE_TOO_SMALL,
    FACE_TOO_LARGE,
    HEAD_ANGLE_TOO_HIGH,
    EYES_NOT_VISIBLE,
    LOW_LIGHT,
    OVEREXPOSED,
    MOTION_BLUR,
    CAMERA_OBSTRUCTED
}

enum class LivenessStatus {
    LIVE,
    UNCERTAIN,
    SPOOF_SUSPECTED,
    NOT_EVALUATED
}

enum class AiVerificationState {
    VERIFIED,
    UNCERTAIN,
    FAILED,
    INVALID_INPUT
}

enum class AiPowerState {
    SLEEP,
    PASSIVE,
    ACTIVE,
    INTENSIVE,
    EMERGENCY
}

data class FrameMetadata(
    val frameId: Long,
    val timestampElapsedMillis: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val brightnessScore: Float,
    val sharpnessScore: Float,
    val motionScore: Float
)

data class FaceAnalysisResult(
    val faceDetected: Boolean,
    val faceCount: Int,
    val faceConfidence: Float,
    val eyeVisibilityScore: Float,
    val headPoseScore: Float,
    val faceSizeScore: Float,
    val qualityIssue: FaceQualityIssue
)

data class GlassesAnalysisResult(
    val predictedClass: GlassesClass,
    val prescriptionGlassesScore: Float,
    val noGlassesScore: Float,
    val geometryScore: Float,
    val bridgeScore: Float,
    val templeArmScore: Float,
    val reflectionScore: Float
)

data class LivenessAnalysisResult(
    val status: LivenessStatus,
    val score: Float,
    val blinkScore: Float,
    val motionConsistencyScore: Float,
    val screenSpoofScore: Float
)

data class AiFrameEvidence(
    val metadata: FrameMetadata,
    val face: FaceAnalysisResult,
    val glasses: GlassesAnalysisResult,
    val liveness: LivenessAnalysisResult
)

data class VerificationResult(
    val state: AiVerificationState,
    val confidence: Float,
    val explanationCode: String,
    val usedFrameCount: Int,
    val powerState: AiPowerState
)
