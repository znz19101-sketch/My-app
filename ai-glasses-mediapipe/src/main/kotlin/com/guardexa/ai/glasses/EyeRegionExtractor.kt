
package com.guardexa.ai.glasses

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import com.guardexa.ai.face.FaceLandmarkResult
import com.guardexa.ai.face.LandmarkPoint
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

data class EyeRegion(
    val bitmap: Bitmap,
    val sourceRect: Rect,
    val rotationDegrees: Float,
    val geometryConfidence: Float
)

class EyeRegionExtractor(
    private val outputWidth: Int = 224,
    private val outputHeight: Int = 96
) {
    fun extract(
        source: Bitmap,
        landmarks: FaceLandmarkResult
    ): EyeRegion? {
        val face = landmarks.faces.singleOrNull() ?: return null
        if (face.size < MIN_REQUIRED_LANDMARKS) return null

        val leftEye = averagePoints(face, LEFT_EYE_INDICES) ?: return null
        val rightEye = averagePoints(face, RIGHT_EYE_INDICES) ?: return null
        val noseBridge = averagePoints(face, NOSE_BRIDGE_INDICES) ?: return null

        val leftPx = leftEye.toPixel(source.width, source.height)
        val rightPx = rightEye.toPixel(source.width, source.height)
        val bridgePx = noseBridge.toPixel(source.width, source.height)

        val eyeDistance = distance(leftPx.first, leftPx.second, rightPx.first, rightPx.second)
        if (eyeDistance < source.width * 0.08f) return null

        val rotation = Math.toDegrees(
            atan2(
                (rightPx.second - leftPx.second).toDouble(),
                (rightPx.first - leftPx.first).toDouble()
            )
        ).toFloat()

        val centerX = (leftPx.first + rightPx.first + bridgePx.first) / 3f
        val centerY = (leftPx.second + rightPx.second + bridgePx.second) / 3f

        val regionWidth = eyeDistance * 2.10f
        val regionHeight = eyeDistance * 0.95f

        val left = (centerX - regionWidth / 2f).toInt().coerceAtLeast(0)
        val top = (centerY - regionHeight / 2f).toInt().coerceAtLeast(0)
        val right = (centerX + regionWidth / 2f).toInt().coerceAtMost(source.width)
        val bottom = (centerY + regionHeight / 2f).toInt().coerceAtMost(source.height)

        if (right <= left || bottom <= top) return null

        val rect = Rect(left, top, right, bottom)
        val cropped = Bitmap.createBitmap(
            source,
            rect.left,
            rect.top,
            rect.width(),
            rect.height()
        )

        val matrix = Matrix().apply {
            postRotate(-rotation)
            postScale(
                outputWidth.toFloat() / cropped.width.toFloat(),
                outputHeight.toFloat() / cropped.height.toFloat()
            )
        }

        val normalized = Bitmap.createBitmap(
            cropped,
            0,
            0,
            cropped.width,
            cropped.height,
            matrix,
            true
        )

        if (normalized !== cropped) cropped.recycle()

        val geometryConfidence = (
            1f - (kotlin.math.abs(rotation) / 45f)
        ).coerceIn(0f, 1f)

        return EyeRegion(
            bitmap = normalized,
            sourceRect = rect,
            rotationDegrees = rotation,
            geometryConfidence = geometryConfidence
        )
    }

    private fun averagePoints(
        points: List<LandmarkPoint>,
        indices: IntArray
    ): LandmarkPoint? {
        if (indices.any { it !in points.indices }) return null
        val selected = indices.map(points::get)
        return LandmarkPoint(
            x = selected.map { it.x }.average().toFloat(),
            y = selected.map { it.y }.average().toFloat(),
            z = selected.map { it.z }.average().toFloat()
        )
    }

    private fun LandmarkPoint.toPixel(
        width: Int,
        height: Int
    ): Pair<Float, Float> =
        x * width to y * height

    private fun distance(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    companion object {
        private const val MIN_REQUIRED_LANDMARKS = 468

        private val LEFT_EYE_INDICES = intArrayOf(
            33, 133, 159, 145, 153, 154, 155
        )

        private val RIGHT_EYE_INDICES = intArrayOf(
            362, 263, 386, 374, 380, 381, 382
        )

        private val NOSE_BRIDGE_INDICES = intArrayOf(
            6, 168, 197, 195
        )
    }
}
