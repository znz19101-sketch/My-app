
package com.guardexa.ai.liveness

enum class PassiveSignalType {
    BLINK,
    HEAD_MOTION,
    GAZE_SHIFT,
    PERSPECTIVE_CHANGE,
    LIGHTING_VARIATION,
    GLASSES_FACE_MOTION_CONSISTENCY
}

data class PassiveSignal(
    val type: PassiveSignalType,
    val score: Float,
    val timestampElapsedMillis: Long
)

data class PassiveLivenessSnapshot(
    val score: Float,
    val blinkScore: Float,
    val headMotionScore: Float,
    val gazeScore: Float,
    val motionConsistencyScore: Float,
    val signalCount: Int
)

class PassiveLivenessEngine(
    private val maximumSignals: Int = 40
) {
    private val signals = ArrayDeque<PassiveSignal>()

    fun add(signal: PassiveSignal) {
        signals.addLast(
            signal.copy(score = signal.score.coerceIn(0f, 1f))
        )
        while (signals.size > maximumSignals) {
            signals.removeFirst()
        }
    }

    fun snapshot(): PassiveLivenessSnapshot {
        if (signals.isEmpty()) {
            return PassiveLivenessSnapshot(
                score = 0f,
                blinkScore = 0f,
                headMotionScore = 0f,
                gazeScore = 0f,
                motionConsistencyScore = 0f,
                signalCount = 0
            )
        }

        fun average(type: PassiveSignalType): Float {
            val values = signals.filter { it.type == type }.map { it.score }
            return if (values.isEmpty()) 0f else values.average().toFloat()
        }

        val blink = average(PassiveSignalType.BLINK)
        val head = average(PassiveSignalType.HEAD_MOTION)
        val gaze = average(PassiveSignalType.GAZE_SHIFT)
        val perspective = average(PassiveSignalType.PERSPECTIVE_CHANGE)
        val lighting = average(PassiveSignalType.LIGHTING_VARIATION)
        val consistency = average(
            PassiveSignalType.GLASSES_FACE_MOTION_CONSISTENCY
        )

        val finalScore = (
            blink * 0.20f +
                head * 0.20f +
                gaze * 0.10f +
                perspective * 0.20f +
                lighting * 0.10f +
                consistency * 0.20f
            ).coerceIn(0f, 1f)

        return PassiveLivenessSnapshot(
            score = finalScore,
            blinkScore = blink,
            headMotionScore = head,
            gazeScore = gaze,
            motionConsistencyScore = consistency,
            signalCount = signals.size
        )
    }

    fun clear() {
        signals.clear()
    }
}
