
package com.guardexa.ai.integration

import com.guardexa.ai.camera.CameraFrame
import com.guardexa.ai.camera.CameraFrameConsumer
import com.guardexa.ai.core.model.AiFrameEvidence
import com.guardexa.ai.core.model.GlassesAnalysisResult
import com.guardexa.ai.core.model.GlassesClass
import com.guardexa.ai.core.model.LivenessAnalysisResult
import com.guardexa.ai.core.model.LivenessStatus
import com.guardexa.ai.face.FaceLandmarker
import com.guardexa.ai.face.FaceQualityEvaluator

interface GlassesAnalyzer {
    suspend fun analyze(
        bitmap: android.graphics.Bitmap,
        faceLandmarks: com.guardexa.ai.face.FaceLandmarkResult
    ): GlassesAnalysisResult
}

interface LivenessAnalyzer {
    suspend fun analyze(
        bitmap: android.graphics.Bitmap,
        faceLandmarks: com.guardexa.ai.face.FaceLandmarkResult
    ): LivenessAnalysisResult
}

interface AiEvidenceConsumer {
    suspend fun consume(evidence: AiFrameEvidence)
}

class CameraFacePipeline(
    private val faceLandmarker: FaceLandmarker,
    private val faceQualityEvaluator: FaceQualityEvaluator,
    private val glassesAnalyzer: GlassesAnalyzer,
    private val livenessAnalyzer: LivenessAnalyzer,
    private val evidenceConsumer: AiEvidenceConsumer
) : CameraFrameConsumer {

    override suspend fun consume(frame: CameraFrame) {
        try {
            val landmarks = faceLandmarker.analyze(frame.bitmap)

            val face = faceQualityEvaluator.evaluate(
                landmarks = landmarks,
                imageWidth = frame.bitmap.width,
                imageHeight = frame.bitmap.height,
                brightnessScore = frame.metadata.brightnessScore,
                sharpnessScore = frame.metadata.sharpnessScore
            )

            val glasses = if (face.faceDetected && face.faceCount == 1) {
                glassesAnalyzer.analyze(frame.bitmap, landmarks)
            } else {
                GlassesAnalysisResult(
                    predictedClass = GlassesClass.INVALID_REGION,
                    prescriptionGlassesScore = 0f,
                    noGlassesScore = 0f,
                    geometryScore = 0f,
                    bridgeScore = 0f,
                    templeArmScore = 0f,
                    reflectionScore = 0f
                )
            }

            val liveness = if (face.faceDetected && face.faceCount == 1) {
                livenessAnalyzer.analyze(frame.bitmap, landmarks)
            } else {
                LivenessAnalysisResult(
                    status = LivenessStatus.NOT_EVALUATED,
                    score = 0f,
                    blinkScore = 0f,
                    motionConsistencyScore = 0f,
                    screenSpoofScore = 0f
                )
            }

            evidenceConsumer.consume(
                AiFrameEvidence(
                    metadata = frame.metadata,
                    face = face,
                    glasses = glasses,
                    liveness = liveness
                )
            )
        } finally {
            frame.bitmap.recycle()
        }
    }
}
