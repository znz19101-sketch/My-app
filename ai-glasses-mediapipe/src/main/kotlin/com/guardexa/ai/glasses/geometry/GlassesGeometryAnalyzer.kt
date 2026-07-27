
package com.guardexa.ai.glasses.geometry

import android.graphics.Bitmap
import com.guardexa.ai.glasses.EyeRegion
import kotlin.math.abs

data class GeometryEvidence(
    val bridgeScore: Float,
    val templeArmScore: Float,
    val reflectionScore: Float
)

class GlassesGeometryAnalyzer {

    fun analyze(region: EyeRegion): GeometryEvidence {
        val bitmap = region.bitmap

        val bridge = analyzeBridge(bitmap)
        val temple = analyzeTempleArms(bitmap)
        val reflection = analyzeReflections(bitmap)

        return GeometryEvidence(
            bridgeScore = bridge,
            templeArmScore = temple,
            reflectionScore = reflection
        )
    }

    private fun analyzeBridge(bitmap: Bitmap): Float {
        val centerX = bitmap.width / 2
        val startX = (centerX - bitmap.width * 0.08f).toInt().coerceAtLeast(0)
        val endX = (centerX + bitmap.width * 0.08f).toInt().coerceAtMost(bitmap.width - 1)
        val startY = (bitmap.height * 0.30f).toInt()
        val endY = (bitmap.height * 0.70f).toInt()

        var edgeSum = 0.0
        var count = 0

        for (y in startY until endY.coerceAtMost(bitmap.height - 1)) {
            for (x in startX until endX) {
                val current = gray(bitmap.getPixel(x, y))
                val next = gray(bitmap.getPixel((x + 1).coerceAtMost(bitmap.width - 1), y))
                edgeSum += abs(current - next)
                count++
            }
        }

        return if (count == 0) 0f
        else (edgeSum / count / 64.0).toFloat().coerceIn(0f, 1f)
    }

    private fun analyzeTempleArms(bitmap: Bitmap): Float {
        val leftBandEnd = (bitmap.width * 0.18f).toInt()
        val rightBandStart = (bitmap.width * 0.82f).toInt()
        val yStart = (bitmap.height * 0.25f).toInt()
        val yEnd = (bitmap.height * 0.75f).toInt()

        val leftScore = bandEdgeScore(bitmap, 0, leftBandEnd, yStart, yEnd)
        val rightScore = bandEdgeScore(
            bitmap,
            rightBandStart,
            bitmap.width,
            yStart,
            yEnd
        )

        return ((leftScore + rightScore) / 2f).coerceIn(0f, 1f)
    }

    private fun analyzeReflections(bitmap: Bitmap): Float {
        val leftStart = (bitmap.width * 0.15f).toInt()
        val leftEnd = (bitmap.width * 0.45f).toInt()
        val rightStart = (bitmap.width * 0.55f).toInt()
        val rightEnd = (bitmap.width * 0.85f).toInt()
        val yStart = (bitmap.height * 0.20f).toInt()
        val yEnd = (bitmap.height * 0.80f).toInt()

        val left = brightPixelRatio(bitmap, leftStart, leftEnd, yStart, yEnd)
        val right = brightPixelRatio(bitmap, rightStart, rightEnd, yStart, yEnd)

        val symmetry = (1f - abs(left - right)).coerceIn(0f, 1f)
        val presence = ((left + right) / 2f * 4f).coerceIn(0f, 1f)

        return (symmetry * 0.4f + presence * 0.6f).coerceIn(0f, 1f)
    }

    private fun bandEdgeScore(
        bitmap: Bitmap,
        xStart: Int,
        xEnd: Int,
        yStart: Int,
        yEnd: Int
    ): Float {
        var edgeSum = 0.0
        var count = 0

        val safeXEnd = xEnd.coerceAtMost(bitmap.width - 1)
        val safeYEnd = yEnd.coerceAtMost(bitmap.height - 1)

        for (y in yStart.coerceAtLeast(0) until safeYEnd) {
            for (x in xStart.coerceAtLeast(0) until safeXEnd) {
                val current = gray(bitmap.getPixel(x, y))
                val next = gray(bitmap.getPixel((x + 1).coerceAtMost(bitmap.width - 1), y))
                edgeSum += abs(current - next)
                count++
            }
        }

        return if (count == 0) 0f
        else (edgeSum / count / 72.0).toFloat().coerceIn(0f, 1f)
    }

    private fun brightPixelRatio(
        bitmap: Bitmap,
        xStart: Int,
        xEnd: Int,
        yStart: Int,
        yEnd: Int
    ): Float {
        var bright = 0
        var count = 0

        for (y in yStart.coerceAtLeast(0) until yEnd.coerceAtMost(bitmap.height)) {
            for (x in xStart.coerceAtLeast(0) until xEnd.coerceAtMost(bitmap.width)) {
                if (gray(bitmap.getPixel(x, y)) >= 210) bright++
                count++
            }
        }

        return if (count == 0) 0f else bright.toFloat() / count.toFloat()
    }

    private fun gray(pixel: Int): Int =
        (
            android.graphics.Color.red(pixel) +
                android.graphics.Color.green(pixel) +
                android.graphics.Color.blue(pixel)
            ) / 3
}
