package com.guardexa.app.runtime

import com.guardexa.ai.core.model.FaceQualityIssue
import com.guardexa.ai.core.model.VerificationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AiVerificationSnapshot(
    val result: VerificationResult? = null,
    val faceDetected: Boolean = false,
    val faceQualityIssue: FaceQualityIssue = FaceQualityIssue.NO_FACE,
    val updatedAtElapsedMillis: Long? = null
)

@Singleton
class AiVerificationRepository @Inject constructor() {

    private val _state = MutableStateFlow(
        AiVerificationSnapshot()
    )

    val state: StateFlow<AiVerificationSnapshot> =
        _state.asStateFlow()

    fun current(): AiVerificationSnapshot = _state.value

    fun update(snapshot: AiVerificationSnapshot) {
        _state.value = snapshot
    }

    fun clear() {
        _state.value = AiVerificationSnapshot()
    }
}
