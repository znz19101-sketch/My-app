package com.guardexa.app.runtime

import com.guardexa.ai.core.confidence.ConfidenceEngine
import com.guardexa.ai.core.fusion.DecisionFusionEngine
import com.guardexa.ai.core.model.AiFrameEvidence
import com.guardexa.ai.core.model.AiPowerState
import com.guardexa.ai.core.model.AiVerificationState
import com.guardexa.ai.integration.AiEvidenceConsumer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAiEvidenceConsumer @Inject constructor(
    private val confidenceEngine: ConfidenceEngine,
    private val fusionEngine: DecisionFusionEngine,
    private val repository: AiVerificationRepository
) : AiEvidenceConsumer {

    private val mutex = Mutex()

    override suspend fun consume(evidence: AiFrameEvidence) {
        mutex.withLock {
            confidenceEngine.addEvidence(evidence)

            val confidence = confidenceEngine.snapshot(
                evidence.metadata.timestampElapsedMillis
            )

            val previouslyVerified =
                repository.current().result?.state ==
                    AiVerificationState.VERIFIED

            val result = fusionEngine.decide(
                evidence = evidence,
                confidence = confidence,
                previouslyVerified = previouslyVerified,
                powerState = AiPowerState.ACTIVE
            )

            repository.update(
                AiVerificationSnapshot(
                    result = result,
                    faceDetected = evidence.face.faceDetected,
                    faceQualityIssue = evidence.face.qualityIssue,
                    updatedAtElapsedMillis =
                        evidence.metadata.timestampElapsedMillis
                )
            )
        }
    }
}
