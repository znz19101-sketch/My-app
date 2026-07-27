
package com.guardexa.ai.liveness

import android.graphics.Bitmap
import com.guardexa.ai.core.model.LivenessAnalysisResult
import com.guardexa.ai.core.model.LivenessStatus
import com.guardexa.ai.face.FaceLandmarkResult
import com.guardexa.ai.integration.LivenessAnalyzer
import com.guardexa.ai.liveness.spoof.SpoofDetectionEngine
import com.guardexa.ai.liveness.spoof.SpoofSignals

class DefaultLivenessAnalyzer(
    private val passiveEngine: PassiveLivenessEngine,
    private val spoofEngine: SpoofDetectionEngine
) : LivenessAnalyzer {

    override suspend fun analyze(
        bitmap: Bitmap,
        faceLandmarks: FaceLandmarkResult
    ): LivenessAnalysisResult {
        val passive = passiveEngine.snapshot()

        val spoof = spoofEngine.assess(
            SpoofSignals(
                screenMoireScore = estimateMoire(bitmap),
                flatPerspectiveScore = estimateFlatPerspective(faceLandmarks),
                replayLoopScore = 0f,
                staticReflectionScore = 0f,
                unnaturalMotionScore = 0f,
                passiveLivenessScore = passive.score
            )
        )

        val status = when {
            spoof.suspected -> LivenessStatus.SPOOF_SUSPECTED
            passive.score >= 0.65f -> LivenessStatus.LIVE
            passive.signalCount >= 3 -> LivenessStatus.UNCERTAIN
            else -> LivenessStatus.NOT_EVALUATED
        }

        return LivenessAnalysisResult(
            status = status,
            score = passive.score,
            blinkScore = passive.blinkScore,
            motionConsistencyScore = passive.motionConsistencyScore,
            screenSpoofScore = spoof.spoofScore
        )
    }

    private fun estimateMoire(bitmap: Bitmap): Float {
        if (bitmap.width < 8 || bitmap.height < 8) return 0f

        val stepX = (bitmap.width / 48).coerceAtLeast(1)
        val stepY = (bitmap.height / 48).coerceAtLeast(1)

        var alternating = 0
        var samples = 0
        var previous: Int? = null

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val value = grayscale(bitmap.getPixel(x, y))
                previous?.let {
                    if (kotlin.math.abs(value - it) > 55) alternating++
                }
                previous = value
                samples++
                x += stepX
            }
            y += stepY
        }

        return if (samples == 0) 0f
        else (alternating.toFloat() / samples.toFloat() * 2f)
            .coerceIn(0f, 1f)
    }

    private fun estimateFlatPerspective(
        landmarks: FaceLandmarkResult
    ): Float {
        val face = landmarks.faces.singleOrNull() ?: return 1f
        if (face.isEmpty()) return 1f

        val depthValues = face.map { it.z }
        val min = depthValues.minOrNull() ?: 0f
        val max = depthValues.maxOrNull() ?: 0f
        val range = kotlin.math.abs(max - min)

        return (1f - range * 8f).coerceIn(0f, 1f)
    }

    private fun grayscale(pixel: Int): Int =
        (
            android.graphics.Color.red(pixel) +
                android.graphics.Color.green(pixel) +
                android.graphics.Color.blue(pixel)
            ) / 3
}
