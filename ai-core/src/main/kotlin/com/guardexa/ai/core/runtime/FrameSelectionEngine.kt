
package com.guardexa.ai.core.runtime

import com.guardexa.ai.core.model.FrameMetadata

data class FrameSelectionPolicy(
    val minimumBrightness: Float = 0.15f,
    val minimumSharpness: Float = 0.35f,
    val maximumMotion: Float = 0.75f
)

sealed interface FrameSelectionResult {
    data object Accepted : FrameSelectionResult
    data class Rejected(val reasonCode: String) : FrameSelectionResult
}

class FrameSelectionEngine(
    private val policy: FrameSelectionPolicy = FrameSelectionPolicy()
) {
    fun evaluate(metadata: FrameMetadata): FrameSelectionResult =
        when {
            metadata.brightnessScore < policy.minimumBrightness ->
                FrameSelectionResult.Rejected("frame_too_dark")

            metadata.sharpnessScore < policy.minimumSharpness ->
                FrameSelectionResult.Rejected("frame_too_blurry")

            metadata.motionScore > policy.maximumMotion ->
                FrameSelectionResult.Rejected("frame_motion_too_high")

            metadata.width <= 0 || metadata.height <= 0 ->
                FrameSelectionResult.Rejected("invalid_frame_dimensions")

            else ->
                FrameSelectionResult.Accepted
        }
}
