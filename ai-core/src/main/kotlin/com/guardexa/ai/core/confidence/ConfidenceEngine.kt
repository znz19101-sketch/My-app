
package com.guardexa.ai.core.confidence

import com.guardexa.ai.core.model.AiFrameEvidence
import kotlin.math.exp

data class ConfidencePolicy(
    val maximumSamples: Int = 20,
    val openThreshold: Float = 0.82f,
    val keepOpenThreshold: Float = 0.68f,
    val uncertainThreshold: Float = 0.55f,
    val minimumFaceQuality: Float = 0.55f,
    val minimumLiveness: Float = 0.55f,
    val sampleHalfLifeMillis: Long = 4_000L
) {
    init {
        require(maximumSamples in 3..100)
        require(openThreshold in 0f..1f)
        require(keepOpenThreshold in 0f..openThreshold)
        require(uncertainThreshold in 0f..keepOpenThreshold)
        require(sampleHalfLifeMillis > 0)
    }
}

data class ConfidenceSample(
    val timestampElapsedMillis: Long,
    val score: Float,
    val faceQuality: Float,
    val liveness: Float,
    val valid: Boolean
)

data class ConfidenceSnapshot(
    val weightedScore: Float,
    val validSampleCount: Int,
    val trend: Float,
    val volatility: Float
)

class ConfidenceEngine(
    private val policy: ConfidencePolicy
) {
    private val samples = ArrayDeque<ConfidenceSample>()

    fun addEvidence(evidence: AiFrameEvidence) {
        val faceQuality = average(
            evidence.face.faceConfidence,
            evidence.face.eyeVisibilityScore,
            evidence.face.headPoseScore,
            evidence.face.faceSizeScore
        )

        val glassesScore = weightedAverage(
            evidence.glasses.prescriptionGlassesScore to 0.45f,
            evidence.glasses.geometryScore to 0.20f,
            evidence.glasses.bridgeScore to 0.12f,
            evidence.glasses.templeArmScore to 0.10f,
            evidence.glasses.reflectionScore to 0.05f,
            evidence.liveness.score to 0.08f
        )

        val valid = evidence.face.faceDetected &&
            evidence.face.faceCount == 1 &&
            faceQuality >= policy.minimumFaceQuality &&
            evidence.liveness.score >= policy.minimumLiveness

        samples.addLast(
            ConfidenceSample(
                timestampElapsedMillis = evidence.metadata.timestampElapsedMillis,
                score = glassesScore.coerceIn(0f, 1f),
                faceQuality = faceQuality.coerceIn(0f, 1f),
                liveness = evidence.liveness.score.coerceIn(0f, 1f),
                valid = valid
            )
        )

        while (samples.size > policy.maximumSamples) {
            samples.removeFirst()
        }
    }

    fun snapshot(nowElapsedMillis: Long): ConfidenceSnapshot {
        val validSamples = samples.filter { it.valid }
        if (validSamples.isEmpty()) {
            return ConfidenceSnapshot(
                weightedScore = 0f,
                validSampleCount = 0,
                trend = 0f,
                volatility = 0f
            )
        }

        var weightSum = 0.0
        var weightedScore = 0.0

        for (sample in validSamples) {
            val age = (nowElapsedMillis - sample.timestampElapsedMillis)
                .coerceAtLeast(0L)
            val weight = exp(
                -age.toDouble() /
                    policy.sampleHalfLifeMillis.toDouble()
            )

            weightSum += weight
            weightedScore += sample.score * weight
        }

        val score = if (weightSum == 0.0) 0f
        else (weightedScore / weightSum).toFloat()

        val trend = calculateTrend(validSamples)
        val volatility = calculateVolatility(validSamples)

        return ConfidenceSnapshot(
            weightedScore = score.coerceIn(0f, 1f),
            validSampleCount = validSamples.size,
            trend = trend,
            volatility = volatility
        )
    }

    fun clear() {
        samples.clear()
    }

    fun hasEnoughEvidence(minimumSamples: Int = 3): Boolean =
        samples.count { it.valid } >= minimumSamples

    private fun calculateTrend(samples: List<ConfidenceSample>): Float {
        if (samples.size < 2) return 0f
        return (
            samples.last().score - samples.first().score
        ) / (samples.size - 1).coerceAtLeast(1)
    }

    private fun calculateVolatility(samples: List<ConfidenceSample>): Float {
        if (samples.size < 2) return 0f
        val mean = samples.map { it.score }.average()
        val variance = samples
            .map { (it.score - mean) * (it.score - mean) }
            .average()
        return variance.toFloat()
    }

    private fun average(vararg values: Float): Float =
        if (values.isEmpty()) 0f else values.average().toFloat()

    private fun weightedAverage(
        vararg values: Pair<Float, Float>
    ): Float {
        val totalWeight = values.sumOf { it.second.toDouble() }.toFloat()
        if (totalWeight <= 0f) return 0f
        return values.sumOf { (it.first * it.second).toDouble() }
            .toFloat() / totalWeight
    }
}
