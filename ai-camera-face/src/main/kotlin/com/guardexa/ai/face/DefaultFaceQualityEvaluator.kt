
package com.guardexa.ai.face

import com.guardexa.ai.core.model.FaceAnalysisResult
import com.guardexa.ai.core.model.FaceQualityIssue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DefaultFaceQualityEvaluator : FaceQualityEvaluator {

    override fun evaluate(
        landmarks: FaceLandmarkResult,
        imageWidth: Int,
        imageHeight: Int,
        brightnessScore: Float,
        sharpnessScore: Float
    ): FaceAnalysisResult {
        val count = landmarks.faces.size

        if (count == 0) {
            return failure(
                issue = FaceQualityIssue.NO_FACE,
                count = 0
            )
        }

        if (count > 1) {
            return failure(
                issue = FaceQualityIssue.MULTIPLE_FACES,
                count = count
            )
        }

        if (brightnessScore < 0.15f) {
            return failure(
                issue = FaceQualityIssue.LOW_LIGHT,
                count = 1
            )
        }

        if (brightnessScore > 0.95f) {
            return failure(
                issue = FaceQualityIssue.OVEREXPOSED,
                count = 1
            )
        }

        if (sharpnessScore < 0.25f) {
            return failure(
                issue = FaceQualityIssue.MOTION_BLUR,
                count = 1
            )
        }

        val points = landmarks.faces.first()
        if (points.isEmpty()) {
            return failure(
                issue = FaceQualityIssue.NO_FACE,
                count = 1
            )
        }

        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }

        val normalizedWidth = (maxX - minX).coerceIn(0f, 1f)
        val normalizedHeight = (maxY - minY).coerceIn(0f, 1f)
        val sizeScore = ((normalizedWidth + normalizedHeight) / 2f)
            .coerceIn(0f, 1f)

        val sizeIssue = when {
            sizeScore < 0.18f -> FaceQualityIssue.FACE_TOO_SMALL
            sizeScore > 0.92f -> FaceQualityIssue.FACE_TOO_LARGE
            else -> FaceQualityIssue.NONE
        }

        if (sizeIssue != FaceQualityIssue.NONE) {
            return failure(
                issue = sizeIssue,
                count = 1,
                sizeScore = sizeScore
            )
        }

        val faceConfidence = 0.90f
        val eyeVisibility = estimateEyeVisibility(points)
        val headPose = estimateHeadPose(points)

        val issue = when {
            eyeVisibility < 0.55f -> FaceQualityIssue.EYES_NOT_VISIBLE
            headPose < 0.45f -> FaceQualityIssue.HEAD_ANGLE_TOO_HIGH
            else -> FaceQualityIssue.NONE
        }

        return FaceAnalysisResult(
            faceDetected = true,
            faceCount = 1,
            faceConfidence = faceConfidence,
            eyeVisibilityScore = eyeVisibility,
            headPoseScore = headPose,
            faceSizeScore = sizeScore,
            qualityIssue = issue
        )
    }

    private fun estimateEyeVisibility(points: List<LandmarkPoint>): Float {
        if (points.size < 10) return 0.5f
        val upperHalf = points.count { it.y < 0.55f }
        return (upperHalf.toFloat() / points.size.toFloat() * 2f)
            .coerceIn(0f, 1f)
    }

    private fun estimateHeadPose(points: List<LandmarkPoint>): Float {
        if (points.isEmpty()) return 0f
        val averageX = points.map { it.x }.average().toFloat()
        val deviation = abs(averageX - 0.5f)
        return (1f - deviation * 2f).coerceIn(0f, 1f)
    }

    private fun failure(
        issue: FaceQualityIssue,
        count: Int,
        sizeScore: Float = 0f
    ) = FaceAnalysisResult(
        faceDetected = count > 0,
        faceCount = count,
        faceConfidence = if (count > 0) 0.5f else 0f,
        eyeVisibilityScore = 0f,
        headPoseScore = 0f,
        faceSizeScore = sizeScore,
        qualityIssue = issue
    )
}
