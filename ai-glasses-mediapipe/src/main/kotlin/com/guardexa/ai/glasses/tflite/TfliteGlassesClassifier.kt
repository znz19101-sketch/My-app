
package com.guardexa.ai.glasses.tflite

import android.content.Context
import android.graphics.Bitmap
import com.guardexa.ai.core.model.GlassesAnalysisResult
import com.guardexa.ai.core.model.GlassesClass
import com.guardexa.ai.glasses.EyeRegion
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max

data class GlassesClassifierConfig(
    val modelAssetPath: String = "glasses_classifier.tflite",
    val inputWidth: Int = 224,
    val inputHeight: Int = 96,
    val inputChannels: Int = 3,
    val numThreads: Int = 4,
    val prescriptionClassIndex: Int = 0,
    val noGlassesClassIndex: Int = 1,
    val sunglassesClassIndex: Int = 2,
    val occludedClassIndex: Int = 3
)

class TfliteGlassesClassifier(
    context: Context,
    private val config: GlassesClassifierConfig = GlassesClassifierConfig()
) : AutoCloseable {

    private val interpreter: Interpreter

    init {
        val model = loadMappedModel(context, config.modelAssetPath)
        val options = Interpreter.Options()
            .setNumThreads(config.numThreads)
            .setUseXNNPACK(true)

        interpreter = Interpreter(model, options)
    }

    fun classify(
        eyeRegion: EyeRegion,
        bridgeScore: Float,
        templeArmScore: Float,
        reflectionScore: Float
    ): GlassesAnalysisResult {
        val input = bitmapToFloatBuffer(eyeRegion.bitmap)
        val output = Array(1) { FloatArray(4) }

        interpreter.run(input, output)

        val scores = softmax(output[0])

        val prescription = scores.getOrElse(config.prescriptionClassIndex) { 0f }
        val noGlasses = scores.getOrElse(config.noGlassesClassIndex) { 0f }
        val sunglasses = scores.getOrElse(config.sunglassesClassIndex) { 0f }
        val occluded = scores.getOrElse(config.occludedClassIndex) { 0f }

        val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: -1

        val predicted = when (maxIndex) {
            config.prescriptionClassIndex -> GlassesClass.PRESCRIPTION_GLASSES
            config.noGlassesClassIndex -> GlassesClass.NO_GLASSES
            config.sunglassesClassIndex -> GlassesClass.SUNGLASSES
            config.occludedClassIndex -> GlassesClass.OCCLUDED
            else -> GlassesClass.UNKNOWN
        }

        return GlassesAnalysisResult(
            predictedClass = predicted,
            prescriptionGlassesScore = prescription,
            noGlassesScore = noGlasses,
            geometryScore = eyeRegion.geometryConfidence,
            bridgeScore = bridgeScore.coerceIn(0f, 1f),
            templeArmScore = templeArmScore.coerceIn(0f, 1f),
            reflectionScore = reflectionScore.coerceIn(0f, 1f)
        )
    }

    override fun close() {
        interpreter.close()
    }


    private fun loadMappedModel(
        context: Context,
        assetPath: String
    ): MappedByteBuffer {
        val descriptor = context.assets.openFd(assetPath)

        FileInputStream(descriptor.fileDescriptor).use { input ->
            val channel = input.channel
            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                descriptor.startOffset,
                descriptor.declaredLength
            )
        }
    }

    private fun bitmapToFloatBuffer(bitmap: Bitmap): ByteBuffer {
        val resized = if (
            bitmap.width == config.inputWidth &&
            bitmap.height == config.inputHeight
        ) bitmap else Bitmap.createScaledBitmap(
            bitmap,
            config.inputWidth,
            config.inputHeight,
            true
        )

        val buffer = ByteBuffer.allocateDirect(
            4 *
                config.inputWidth *
                config.inputHeight *
                config.inputChannels
        ).order(ByteOrder.nativeOrder())

        val pixels = IntArray(config.inputWidth * config.inputHeight)
        resized.getPixels(
            pixels,
            0,
            config.inputWidth,
            0,
            0,
            config.inputWidth,
            config.inputHeight
        )

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f

            buffer.putFloat((r - 0.5f) / 0.5f)
            buffer.putFloat((g - 0.5f) / 0.5f)
            buffer.putFloat((b - 0.5f) / 0.5f)
        }

        buffer.rewind()

        if (resized !== bitmap) resized.recycle()

        return buffer
    }

    private fun softmax(values: FloatArray): FloatArray {
        val maxValue = values.maxOrNull() ?: 0f
        val exps = values.map { kotlin.math.exp((it - maxValue).toDouble()) }
        val sum = exps.sum().takeIf { it > 0.0 } ?: 1.0
        return FloatArray(values.size) { index ->
            (exps[index] / sum).toFloat()
        }
    }
}
