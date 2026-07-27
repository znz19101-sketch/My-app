
package com.guardexa.core.security.pin

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PinPolicy(
    val minimumLength: Int = 6,
    val maximumLength: Int = 12,
    val maximumFailedAttempts: Int = 5,
    val lockoutDurationMillis: Long = 30_000L,
    val iterations: Int = 210_000,
    val keyLengthBits: Int = 256
) {
    init {
        require(minimumLength >= 6)
        require(maximumLength >= minimumLength)
        require(maximumFailedAttempts > 0)
        require(lockoutDurationMillis >= 1_000L)
        require(iterations >= 100_000)
        require(keyLengthBits >= 128)
    }
}

data class StoredPinCredential(
    val derivedKey: ByteArray,
    val salt: ByteArray,
    val iterations: Int,
    val keyLengthBits: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

sealed interface PinValidationResult {
    data object Valid : PinValidationResult
    data object Invalid : PinValidationResult
    data class TooShort(val minimumLength: Int) : PinValidationResult
    data class TooLong(val maximumLength: Int) : PinValidationResult
    data object NonNumeric : PinValidationResult
}

class PinHasher(
    private val secureRandom: SecureRandom = SecureRandom()
) {
    fun createCredential(
        pin: CharArray,
        policy: PinPolicy,
        nowEpochMillis: Long
    ): StoredPinCredential {
        require(validate(pin, policy) == PinValidationResult.Valid)

        val salt = ByteArray(32).also(secureRandom::nextBytes)
        val derived = derive(
            pin = pin,
            salt = salt,
            iterations = policy.iterations,
            keyLengthBits = policy.keyLengthBits
        )

        pin.fill('\u0000')

        return StoredPinCredential(
            derivedKey = derived,
            salt = salt,
            iterations = policy.iterations,
            keyLengthBits = policy.keyLengthBits,
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis
        )
    }

    fun verify(
        pin: CharArray,
        credential: StoredPinCredential
    ): Boolean {
        val candidate = derive(
            pin = pin,
            salt = credential.salt,
            iterations = credential.iterations,
            keyLengthBits = credential.keyLengthBits
        )

        pin.fill('\u0000')

        return MessageDigest.isEqual(
            credential.derivedKey,
            candidate
        ).also {
            candidate.fill(0)
        }
    }

    fun validate(
        pin: CharArray,
        policy: PinPolicy
    ): PinValidationResult =
        when {
            pin.size < policy.minimumLength ->
                PinValidationResult.TooShort(policy.minimumLength)
            pin.size > policy.maximumLength ->
                PinValidationResult.TooLong(policy.maximumLength)
            pin.any { !it.isDigit() } ->
                PinValidationResult.NonNumeric
            else ->
                PinValidationResult.Valid
        }

    private fun derive(
        pin: CharArray,
        salt: ByteArray,
        iterations: Int,
        keyLengthBits: Int
    ): ByteArray {
        val spec = PBEKeySpec(
            pin,
            salt,
            iterations,
            keyLengthBits
        )

        return try {
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }
}

data class PinAttemptState(
    val failedAttempts: Int = 0,
    val lockedUntilElapsedMillis: Long? = null
) {
    fun isLocked(nowElapsedMillis: Long): Boolean =
        lockedUntilElapsedMillis?.let { nowElapsedMillis < it } == true
}

sealed interface PinAttemptResult {
    data object Accepted : PinAttemptResult
    data class Rejected(val remainingAttempts: Int) : PinAttemptResult
    data class Locked(val remainingMillis: Long) : PinAttemptResult
}

class PinAttemptManager(
    private val policy: PinPolicy,
    private val elapsedRealtime: () -> Long
) {
    private var state = PinAttemptState()

    fun currentState(): PinAttemptState {
        refreshLockout()
        return state
    }

    fun onSuccessfulAuthentication(): PinAttemptResult {
        state = PinAttemptState()
        return PinAttemptResult.Accepted
    }

    fun onFailedAuthentication(): PinAttemptResult {
        val now = elapsedRealtime()
        refreshLockout()

        if (state.isLocked(now)) {
            return PinAttemptResult.Locked(
                remainingMillis = (state.lockedUntilElapsedMillis!! - now)
                    .coerceAtLeast(0L)
            )
        }

        val nextFailures = state.failedAttempts + 1
        return if (nextFailures >= policy.maximumFailedAttempts) {
            val lockedUntil = now + policy.lockoutDurationMillis
            state = PinAttemptState(
                failedAttempts = 0,
                lockedUntilElapsedMillis = lockedUntil
            )
            PinAttemptResult.Locked(policy.lockoutDurationMillis)
        } else {
            state = state.copy(failedAttempts = nextFailures)
            PinAttemptResult.Rejected(
                remainingAttempts = policy.maximumFailedAttempts - nextFailures
            )
        }
    }

    fun forceReset() {
        state = PinAttemptState()
    }

    private fun refreshLockout() {
        val now = elapsedRealtime()
        if (
            state.lockedUntilElapsedMillis != null &&
            now >= state.lockedUntilElapsedMillis
        ) {
            state = PinAttemptState()
        }
    }
}
