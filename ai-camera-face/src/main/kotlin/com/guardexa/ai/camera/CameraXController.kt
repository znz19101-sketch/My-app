
package com.guardexa.ai.camera

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.guardexa.ai.core.model.AiPowerState
import com.guardexa.ai.core.model.FrameMetadata
import com.guardexa.ai.core.runtime.FrameSelectionEngine
import com.guardexa.ai.core.runtime.FrameSelectionResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraXController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val consumer: CameraFrameConsumer,
    private val powerStateProvider: () -> AiPowerState,
    private val elapsedRealtime: () -> Long,
    private val bitmapConverter: ImageProxyBitmapConverter = ImageProxyBitmapConverter(),
    private val frameScheduler: FrameScheduler = FrameScheduler(),
    private val frameSelectionEngine: FrameSelectionEngine = FrameSelectionEngine()
) : CameraController {

    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _health = MutableStateFlow(
        CameraHealth(
            state = CameraState.IDLE,
            lastFrameElapsedMillis = null,
            averageAnalysisMillis = 0L,
            droppedFrames = 0L
        )
    )
    override val health: StateFlow<CameraHealth> = _health

    private var provider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var totalAnalysisMillis = 0L
    private var analysisCount = 0L
    private var droppedFrames = 0L

    override suspend fun start() {
        if (_health.value.state == CameraState.RUNNING) return

        _health.value = _health.value.copy(state = CameraState.STARTING)

        try {
            val cameraProvider = awaitCameraProvider()
            provider = cameraProvider

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                analyze(imageProxy)
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                analysis
            )

            imageAnalysis = analysis
            _health.value = _health.value.copy(
                state = CameraState.RUNNING,
                failureReason = null
            )
        } catch (t: Throwable) {
            _health.value = _health.value.copy(
                state = CameraState.ERROR,
                failureReason = CameraFailureReason.BINDING_FAILED
            )
            throw t
        }
    }

    override suspend fun pause() {
        if (_health.value.state != CameraState.RUNNING) return
        imageAnalysis?.clearAnalyzer()
        _health.value = _health.value.copy(state = CameraState.PAUSED)
    }

    override suspend fun resume() {
        if (_health.value.state != CameraState.PAUSED) return
        imageAnalysis?.setAnalyzer(analysisExecutor) { imageProxy ->
            analyze(imageProxy)
        }
        _health.value = _health.value.copy(state = CameraState.RUNNING)
    }

    override suspend fun stop() {
        provider?.unbindAll()
        imageAnalysis = null
        frameScheduler.reset()
        _health.value = _health.value.copy(state = CameraState.IDLE)
    }

    fun release() {
        provider?.unbindAll()
        imageAnalysis?.clearAnalyzer()
        imageAnalysis = null
        scope.cancel()
        analysisExecutor.shutdownNow()
        _health.value = _health.value.copy(state = CameraState.RELEASED)
    }

    private fun analyze(imageProxy: ImageProxy) {
        val now = elapsedRealtime()

        if (!frameScheduler.shouldAnalyze(now, powerStateProvider())) {
            droppedFrames++
            _health.value = _health.value.copy(droppedFrames = droppedFrames)
            imageProxy.close()
            return
        }

        scope.launch {
            val started = elapsedRealtime()
            try {
                val bitmap = bitmapConverter.convert(imageProxy)
                val metadata = FrameMetadata(
                    frameId = now,
                    timestampElapsedMillis = now,
                    width = bitmap.width,
                    height = bitmap.height,
                    rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                    brightnessScore = BitmapQualityMetrics.brightness(bitmap),
                    sharpnessScore = BitmapQualityMetrics.sharpness(bitmap),
                    motionScore = 0f
                )

                when (frameSelectionEngine.evaluate(metadata)) {
                    FrameSelectionResult.Accepted -> {
                        consumer.consume(
                            CameraFrame(
                                metadata = metadata,
                                bitmap = bitmap
                            )
                        )
                    }
                    is FrameSelectionResult.Rejected -> {
                        droppedFrames++
                        bitmap.recycle()
                    }
                }
            } catch (_: Throwable) {
                droppedFrames++
                _health.value = _health.value.copy(
                    state = CameraState.ERROR,
                    failureReason = CameraFailureReason.ANALYSIS_FAILED
                )
            } finally {
                imageProxy.close()
                val elapsed = (elapsedRealtime() - started).coerceAtLeast(0L)
                analysisCount++
                totalAnalysisMillis += elapsed
                _health.value = _health.value.copy(
                    lastFrameElapsedMillis = now,
                    averageAnalysisMillis = if (analysisCount == 0L) 0L
                    else totalAnalysisMillis / analysisCount,
                    droppedFrames = droppedFrames
                )
            }
        }
    }

    private suspend fun awaitCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    runCatching { future.get() }
                        .onSuccess { continuation.resume(it) }
                        .onFailure { continuation.resumeWithException(it) }
                },
                ContextCompat.getMainExecutor(context)
            )
        }
}

object BitmapQualityMetrics {
    fun brightness(bitmap: android.graphics.Bitmap): Float {
        if (bitmap.width == 0 || bitmap.height == 0) return 0f
        val stepX = (bitmap.width / 16).coerceAtLeast(1)
        val stepY = (bitmap.height / 16).coerceAtLeast(1)
        var sum = 0L
        var count = 0

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                sum += ((r + g + b) / 3)
                count++
                x += stepX
            }
            y += stepY
        }

        return if (count == 0) 0f else (sum.toFloat() / count / 255f)
    }

    fun sharpness(bitmap: android.graphics.Bitmap): Float {
        if (bitmap.width < 3 || bitmap.height < 3) return 0f

        val stepX = (bitmap.width / 32).coerceAtLeast(1)
        val stepY = (bitmap.height / 32).coerceAtLeast(1)

        var sum = 0.0
        var count = 0

        var y = 1
        while (y < bitmap.height - 1) {
            var x = 1
            while (x < bitmap.width - 1) {
                val center = grayscale(bitmap.getPixel(x, y))
                val left = grayscale(bitmap.getPixel(x - 1, y))
                val right = grayscale(bitmap.getPixel(x + 1, y))
                val up = grayscale(bitmap.getPixel(x, y - 1))
                val down = grayscale(bitmap.getPixel(x, y + 1))

                val laplacian = kotlin.math.abs(
                    4 * center - left - right - up - down
                )
                sum += laplacian
                count++
                x += stepX
            }
            y += stepY
        }

        return if (count == 0) 0f
        else (sum / count / 255.0).toFloat().coerceIn(0f, 1f)
    }

    private fun grayscale(pixel: Int): Int =
        (
            android.graphics.Color.red(pixel) +
                android.graphics.Color.green(pixel) +
                android.graphics.Color.blue(pixel)
            ) / 3
}
