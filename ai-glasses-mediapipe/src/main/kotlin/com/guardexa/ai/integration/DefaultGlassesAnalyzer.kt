
package com.guardexa.ai.integration

import android.graphics.Bitmap
import com.guardexa.ai.core.model.GlassesAnalysisResult
import com.guardexa.ai.core.model.GlassesClass
import com.guardexa.ai.face.FaceLandmarkResult
import com.guardexa.ai.glasses.EyeRegionExtractor
import com.guardexa.ai.glasses.geometry.GlassesGeometryAnalyzer
import com.guardexa.ai.glasses.tflite.TfliteGlassesClassifier

class DefaultGlassesAnalyzer(
    private val eyeRegionExtractor: EyeRegionExtractor,
    private val geometryAnalyzer: GlassesGeometryAnalyzer,
    private val classifier: TfliteGlassesClassifier
) : GlassesAnalyzer {

    override suspend fun analyze(
        bitmap: Bitmap,
        faceLandmarks: FaceLandmarkResult
    ): GlassesAnalysisResult {
        val region = eyeRegionExtractor.extract(bitmap, faceLandmarks)
            ?: return GlassesAnalysisResult(
                predictedClass = GlassesClass.INVALID_REGION,
                prescriptionGlassesScore = 0f,
                noGlassesScore = 0f,
                geometryScore = 0f,
                bridgeScore = 0f,
                templeArmScore = 0f,
                reflectionScore = 0f
            )

        return try {
            val geometry = geometryAnalyzer.analyze(region)

            classifier.classify(
                eyeRegion = region,
                bridgeScore = geometry.bridgeScore,
                templeArmScore = geometry.templeArmScore,
                reflectionScore = geometry.reflectionScore
            )
        } finally {
            region.bitmap.recycle()
        }
    }
}
