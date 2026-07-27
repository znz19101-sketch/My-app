
package com.guardexa.ai.core.calibration

import com.guardexa.ai.core.model.AiFrameEvidence

enum class CalibrationStep {
    CENTER,
    TURN_RIGHT,
    TURN_LEFT,
    LOOK_UP,
    LOOK_DOWN,
    BLINK,
    MOVE_CLOSER,
    MOVE_FARTHER,
    COMPLETE
}

data class CalibrationSample(
    val step: CalibrationStep,
    val glassesScore: Float,
    val geometryScore: Float,
    val faceSizeScore: Float,
    val headPoseScore: Float,
    val lightingScore: Float,
    val timestampElapsedMillis: Long
)

data class CalibrationProfile(
    val version: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val qualityScore: Float,
    val sampleCount: Int,
    val averageGlassesScore: Float,
    val averageGeometryScore: Float,
    val acceptableFaceSizeMin: Float,
    val acceptableFaceSizeMax: Float,
    val acceptableLightingMin: Float,
    val recommendedOpenThreshold: Float,
    val recommendedKeepOpenThreshold: Float
)

class CalibrationEngine(
    private val minimumSamplesPerStep: Int = 2
) {
    private val samples = mutableListOf<CalibrationSample>()

    fun add(
        step: CalibrationStep,
        evidence: AiFrameEvidence
    ): Boolean {
        if (step == CalibrationStep.COMPLETE) return false
        if (!evidence.face.faceDetected) return false
        if (evidence.face.faceCount != 1) return false
        if (evidence.face.eyeVisibilityScore < 0.55f) return false
        if (evidence.glasses.prescriptionGlassesScore < 0.55f) return false

        samples += CalibrationSample(
            step = step,
            glassesScore = evidence.glasses.prescriptionGlassesScore,
            geometryScore = evidence.glasses.geometryScore,
            faceSizeScore = evidence.face.faceSizeScore,
            headPoseScore = evidence.face.headPoseScore,
            lightingScore = evidence.metadata.brightnessScore,
            timestampElapsedMillis = evidence.metadata.timestampElapsedMillis
        )
        return true
    }

    fun nextRequiredStep(): CalibrationStep {
        val required = CalibrationStep.entries
            .filterNot { it == CalibrationStep.COMPLETE }

        return required.firstOrNull { step ->
            samples.count { it.step == step } < minimumSamplesPerStep
        } ?: CalibrationStep.COMPLETE
    }

    fun buildProfile(
        nowEpochMillis: Long
    ): CalibrationProfile {
        require(nextRequiredStep() == CalibrationStep.COMPLETE) {
            "Calibration is incomplete"
        }

        val glasses = samples.map { it.glassesScore }
        val geometry = samples.map { it.geometryScore }
        val faceSizes = samples.map { it.faceSizeScore }
        val lighting = samples.map { it.lightingScore }

        val quality = listOf(
            glasses.average(),
            geometry.average(),
            samples.map { it.headPoseScore }.average(),
            lighting.average()
        ).average().toFloat().coerceIn(0f, 1f)

        val averageGlasses = glasses.average().toFloat()
        val recommendedOpen = (
            averageGlasses * 0.90f
        ).coerceIn(0.72f, 0.90f)

        return CalibrationProfile(
            version = 1,
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis,
            qualityScore = quality,
            sampleCount = samples.size,
            averageGlassesScore = averageGlasses,
            averageGeometryScore = geometry.average().toFloat(),
            acceptableFaceSizeMin = (
                faceSizes.minOrNull() ?: 0.4f
            ).coerceAtLeast(0.2f),
            acceptableFaceSizeMax = (
                faceSizes.maxOrNull() ?: 0.8f
            ).coerceAtMost(1f),
            acceptableLightingMin = (
                lighting.minOrNull() ?: 0.4f
            ).coerceAtLeast(0.15f),
            recommendedOpenThreshold = recommendedOpen,
            recommendedKeepOpenThreshold = (
                recommendedOpen - 0.12f
            ).coerceAtLeast(0.60f)
        )
    }

    fun clear() {
        samples.clear()
    }
}
