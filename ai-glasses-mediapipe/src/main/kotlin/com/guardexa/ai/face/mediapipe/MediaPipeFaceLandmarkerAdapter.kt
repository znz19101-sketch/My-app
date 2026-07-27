
package com.guardexa.ai.face.mediapipe

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.guardexa.ai.face.FaceLandmarkResult
import com.guardexa.ai.face.FaceLandmarker
import com.guardexa.ai.face.LandmarkPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

class MediaPipeFaceLandmarkerAdapter(
    context: Context,
    modelAssetPath: String = "face_landmarker.task",
    minimumFaceDetectionConfidence: Float = 0.60f,
    minimumFacePresenceConfidence: Float = 0.60f,
    minimumTrackingConfidence: Float = 0.60f
) : FaceLandmarker {

    private val landmarker: FaceLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(modelAssetPath)
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumFaces(2)
            .setMinFaceDetectionConfidence(minimumFaceDetectionConfidence)
            .setMinFacePresenceConfidence(minimumFacePresenceConfidence)
            .setMinTrackingConfidence(minimumTrackingConfidence)
            .setOutputFaceBlendshapes(true)
            .setOutputFacialTransformationMatrixes(true)
            .build()

        landmarker = FaceLandmarker.createFromOptions(context, options)
    }

    override suspend fun analyze(bitmap: Bitmap): FaceLandmarkResult =
        withContext(Dispatchers.Default) {
            lateinit var result: FaceLandmarkerResult
            val elapsed = measureTimeMillis {
                val mpImage = BitmapImageBuilder(bitmap).build()
                result = landmarker.detect(mpImage)
            }

            FaceLandmarkResult(
                faces = result.faceLandmarks().map { face ->
                    face.map { point ->
                        LandmarkPoint(
                            x = point.x(),
                            y = point.y(),
                            z = point.z()
                        )
                    }
                },
                inferenceMillis = elapsed
            )
        }

    override suspend fun close() {
        withContext(Dispatchers.Default) {
            landmarker.close()
        }
    }
}
