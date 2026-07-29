package com.guardexa.app.di

import android.content.Context
import com.guardexa.ai.camera.CameraFrameConsumer
import com.guardexa.ai.core.confidence.ConfidenceEngine
import com.guardexa.ai.core.confidence.ConfidencePolicy
import com.guardexa.ai.core.fusion.DecisionFusionEngine
import com.guardexa.ai.face.DefaultFaceQualityEvaluator
import com.guardexa.ai.face.FaceLandmarker
import com.guardexa.ai.face.FaceQualityEvaluator
import com.guardexa.ai.face.mediapipe.MediaPipeFaceLandmarkerAdapter
import com.guardexa.ai.glasses.EyeRegionExtractor
import com.guardexa.ai.glasses.geometry.GlassesGeometryAnalyzer
import com.guardexa.ai.glasses.tflite.TfliteGlassesClassifier
import com.guardexa.ai.integration.AiEvidenceConsumer
import com.guardexa.ai.integration.CameraFacePipeline
import com.guardexa.ai.integration.DefaultGlassesAnalyzer
import com.guardexa.ai.integration.GlassesAnalyzer
import com.guardexa.ai.integration.LivenessAnalyzer
import com.guardexa.ai.liveness.DefaultLivenessAnalyzer
import com.guardexa.ai.liveness.PassiveLivenessEngine
import com.guardexa.ai.liveness.spoof.SpoofDetectionEngine
import com.guardexa.app.runtime.DefaultAiEvidenceConsumer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideConfidencePolicy(): ConfidencePolicy =
        ConfidencePolicy()

    @Provides
    @Singleton
    fun provideConfidenceEngine(
        policy: ConfidencePolicy
    ): ConfidenceEngine =
        ConfidenceEngine(policy)

    @Provides
    @Singleton
    fun provideDecisionFusionEngine(
        policy: ConfidencePolicy
    ): DecisionFusionEngine =
        DecisionFusionEngine(policy)

    @Provides
    @Singleton
    fun provideFaceLandmarker(
        @ApplicationContext context: Context
    ): FaceLandmarker =
        MediaPipeFaceLandmarkerAdapter(context)

    @Provides
    @Singleton
    fun provideFaceQualityEvaluator():
        FaceQualityEvaluator =
        DefaultFaceQualityEvaluator()

    @Provides
    @Singleton
    fun provideEyeRegionExtractor():
        EyeRegionExtractor =
        EyeRegionExtractor()

    @Provides
    @Singleton
    fun provideGlassesGeometryAnalyzer():
        GlassesGeometryAnalyzer =
        GlassesGeometryAnalyzer()

    @Provides
    @Singleton
    fun provideTfliteGlassesClassifier(
        @ApplicationContext context: Context
    ): TfliteGlassesClassifier =
        TfliteGlassesClassifier(context)

    @Provides
    @Singleton
    fun provideGlassesAnalyzer(
        eyeRegionExtractor: EyeRegionExtractor,
        geometryAnalyzer: GlassesGeometryAnalyzer,
        classifier: TfliteGlassesClassifier
    ): GlassesAnalyzer =
        DefaultGlassesAnalyzer(
            eyeRegionExtractor = eyeRegionExtractor,
            geometryAnalyzer = geometryAnalyzer,
            classifier = classifier
        )

    @Provides
    @Singleton
    fun providePassiveLivenessEngine():
        PassiveLivenessEngine =
        PassiveLivenessEngine()

    @Provides
    @Singleton
    fun provideSpoofDetectionEngine():
        SpoofDetectionEngine =
        SpoofDetectionEngine()

    @Provides
    @Singleton
    fun provideLivenessAnalyzer(
        passiveEngine: PassiveLivenessEngine,
        spoofEngine: SpoofDetectionEngine
    ): LivenessAnalyzer =
        DefaultLivenessAnalyzer(
            passiveEngine = passiveEngine,
            spoofEngine = spoofEngine
        )

    @Provides
    @Singleton
    fun provideAiEvidenceConsumer(
        consumer: DefaultAiEvidenceConsumer
    ): AiEvidenceConsumer = consumer

    @Provides
    @Singleton
    fun provideCameraFacePipeline(
        faceLandmarker: FaceLandmarker,
        faceQualityEvaluator: FaceQualityEvaluator,
        glassesAnalyzer: GlassesAnalyzer,
        livenessAnalyzer: LivenessAnalyzer,
        evidenceConsumer: AiEvidenceConsumer
    ): CameraFacePipeline =
        CameraFacePipeline(
            faceLandmarker = faceLandmarker,
            faceQualityEvaluator = faceQualityEvaluator,
            glassesAnalyzer = glassesAnalyzer,
            livenessAnalyzer = livenessAnalyzer,
            evidenceConsumer = evidenceConsumer
        )

    @Provides
    @Singleton
    fun provideCameraFrameConsumer(
        pipeline: CameraFacePipeline
    ): CameraFrameConsumer = pipeline
}
