
package com.guardexa.core.security.integrity

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

enum class IntegrityResult {
    VALID,
    MISSING,
    HASH_MISMATCH,
    READ_FAILURE
}

data class IntegrityCheck(
    val id: String,
    val result: IntegrityResult,
    val expectedHash: String?,
    val actualHash: String?
)

class IntegrityVerifier(
    private val algorithm: String = "SHA-256"
) {
    fun verifyFile(
        id: String,
        file: File,
        expectedHashHex: String
    ): IntegrityCheck {
        if (!file.exists()) {
            return IntegrityCheck(
                id = id,
                result = IntegrityResult.MISSING,
                expectedHash = expectedHashHex,
                actualHash = null
            )
        }

        val actual = runCatching {
            file.inputStream().use(::hash)
        }.getOrElse {
            return IntegrityCheck(
                id = id,
                result = IntegrityResult.READ_FAILURE,
                expectedHash = expectedHashHex,
                actualHash = null
            )
        }

        return IntegrityCheck(
            id = id,
            result = if (actual.equals(expectedHashHex, ignoreCase = true)) {
                IntegrityResult.VALID
            } else {
                IntegrityResult.HASH_MISMATCH
            },
            expectedHash = expectedHashHex,
            actualHash = actual
        )
    }

    fun hash(input: InputStream): String {
        val digest = MessageDigest.getInstance(algorithm)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }

        return digest.digest().joinToString(separator = "") {
            "%02x".format(it)
        }
    }
}

sealed interface RecoveryResult {
    data object Restored : RecoveryResult
    data object NoEmbeddedCopy : RecoveryResult
    data class Failed(val reason: String) : RecoveryResult
}

interface EmbeddedAssetProvider {
    fun open(assetPath: String): InputStream?
}

class IntegrityRecoveryManager(
    private val verifier: IntegrityVerifier,
    private val assetProvider: EmbeddedAssetProvider
) {
    fun restoreFromEmbeddedAsset(
        assetPath: String,
        destination: File,
        expectedHashHex: String
    ): RecoveryResult {
        val source = assetProvider.open(assetPath)
            ?: return RecoveryResult.NoEmbeddedCopy

        return runCatching {
            destination.parentFile?.mkdirs()
            val temporary = File(
                destination.parentFile,
                "${destination.name}.recovering"
            )

            source.use { input ->
                temporary.outputStream().use { output ->
                    input.copyTo(output)
                    output.fdSyncIfPossible()
                }
            }

            val verification = verifier.verifyFile(
                id = assetPath,
                file = temporary,
                expectedHashHex = expectedHashHex
            )

            if (verification.result != IntegrityResult.VALID) {
                temporary.delete()
                return RecoveryResult.Failed("Recovered file failed verification")
            }

            if (destination.exists() && !destination.delete()) {
                temporary.delete()
                return RecoveryResult.Failed("Unable to replace damaged file")
            }

            if (!temporary.renameTo(destination)) {
                temporary.delete()
                return RecoveryResult.Failed("Atomic replacement failed")
            }

            RecoveryResult.Restored
        }.getOrElse {
            RecoveryResult.Failed(it.message ?: "Unknown recovery failure")
        }
    }

    private fun java.io.OutputStream.fdSyncIfPossible() {
        (this as? java.io.FileOutputStream)?.fd?.sync()
    }
}
