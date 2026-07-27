
package com.guardexa.ai.liveness.challenge

enum class ChallengeType {
    BLINK_TWICE,
    TURN_HEAD_LEFT,
    TURN_HEAD_RIGHT,
    LOOK_UP,
    LOOK_DOWN
}

data class ActiveChallenge(
    val id: String,
    val type: ChallengeType,
    val createdAtElapsedMillis: Long,
    val expiresAtElapsedMillis: Long
)

sealed interface ChallengeResult {
    data object Passed : ChallengeResult
    data object Failed : ChallengeResult
    data object Expired : ChallengeResult
    data object Pending : ChallengeResult
}

data class ChallengeEvidence(
    val blinkCount: Int = 0,
    val headYaw: Float = 0f,
    val headPitch: Float = 0f
)

class ActiveChallengeEngine(
    private val elapsedRealtime: () -> Long,
    private val challengeDurationMillis: Long = 8_000L,
    private val randomInt: (Int) -> Int
) {
    private var current: ActiveChallenge? = null

    fun create(id: String): ActiveChallenge {
        val now = elapsedRealtime()
        val type = ChallengeType.entries[
            randomInt(ChallengeType.entries.size)
                .coerceIn(0, ChallengeType.entries.lastIndex)
        ]

        return ActiveChallenge(
            id = id,
            type = type,
            createdAtElapsedMillis = now,
            expiresAtElapsedMillis = now + challengeDurationMillis
        ).also { current = it }
    }

    fun current(): ActiveChallenge? {
        val challenge = current ?: return null
        if (elapsedRealtime() >= challenge.expiresAtElapsedMillis) {
            current = null
            return null
        }
        return challenge
    }

    fun evaluate(evidence: ChallengeEvidence): ChallengeResult {
        val challenge = current ?: return ChallengeResult.Pending

        if (elapsedRealtime() >= challenge.expiresAtElapsedMillis) {
            current = null
            return ChallengeResult.Expired
        }

        val passed = when (challenge.type) {
            ChallengeType.BLINK_TWICE -> evidence.blinkCount >= 2
            ChallengeType.TURN_HEAD_LEFT -> evidence.headYaw <= -18f
            ChallengeType.TURN_HEAD_RIGHT -> evidence.headYaw >= 18f
            ChallengeType.LOOK_UP -> evidence.headPitch <= -12f
            ChallengeType.LOOK_DOWN -> evidence.headPitch >= 12f
        }

        return if (passed) {
            current = null
            ChallengeResult.Passed
        } else {
            ChallengeResult.Pending
        }
    }

    fun cancel() {
        current = null
    }
}
