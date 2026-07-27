
package com.guardexa.ai.face

import android.graphics.Bitmap
import com.guardexa.ai.core.model.FaceAnalysisResult

data class LandmarkPoint(
    val x: Float,
    val y: Float,
    val z: Float
)

data class FaceLandmarkResult(
    val faces: List<List<LandmarkPoint>>,
    val inferenceMillis: Long
)

interface FaceLandmarker {
    suspend fun analyze(bitmap: Bitmap): FaceLandmarkResult
    suspend fun close()
}

interface FaceQualityEvaluator {
    fun evaluate(
        landmarks: FaceLandmarkResult,
        imageWidth: Int,
        imageHeight: Int,
        brightnessScore: Float,
        sharpnessScore: Float
    ): FaceAnalysisResult
}
