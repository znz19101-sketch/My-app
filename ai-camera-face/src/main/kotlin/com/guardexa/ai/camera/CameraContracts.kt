
package com.guardexa.ai.camera

import android.graphics.Bitmap
import com.guardexa.ai.core.model.FrameMetadata

enum class CameraState {
    IDLE,
    STARTING,
    RUNNING,
    PAUSED,
    ERROR,
    RELEASED
}

enum class CameraFailureReason {
    PERMISSION_MISSING,
    CAMERA_UNAVAILABLE,
    BINDING_FAILED,
    ANALYSIS_FAILED,
    UNKNOWN
}

data class CameraHealth(
    val state: CameraState,
    val lastFrameElapsedMillis: Long?,
    val averageAnalysisMillis: Long,
    val droppedFrames: Long,
    val failureReason: CameraFailureReason? = null
)

data class CameraFrame(
    val metadata: FrameMetadata,
    val bitmap: Bitmap
)

interface CameraFrameConsumer {
    suspend fun consume(frame: CameraFrame)
}

interface CameraController {
    val health: kotlinx.coroutines.flow.StateFlow<CameraHealth>
    suspend fun start()
    suspend fun pause()
    suspend fun resume()
    suspend fun stop()
}
