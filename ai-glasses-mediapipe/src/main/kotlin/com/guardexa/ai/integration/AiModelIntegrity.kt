
package com.guardexa.ai.integration

import android.content.Context
import com.guardexa.core.security.integrity.IntegrityResult
import com.guardexa.core.security.integrity.IntegrityVerifier
import java.io.File

data class AiModelDescriptor(
    val id: String,
    val assetPath: String,
    val runtimeFileName: String,
    val expectedSha256: String,
    val version: Int
)

class AiModelInstaller(
    private val context: Context,
    private val integrityVerifier: IntegrityVerifier
) {
    fun installOrVerify(
        descriptor: AiModelDescriptor,
        destinationDirectory: File
    ): File {
        destinationDirectory.mkdirs()
        val destination = File(destinationDirectory, descriptor.runtimeFileName)

        if (destination.exists()) {
            val check = integrityVerifier.verifyFile(
                id = descriptor.id,
                file = destination,
                expectedHashHex = descriptor.expectedSha256
            )
            if (check.result == IntegrityResult.VALID) {
                return destination
            }
            destination.delete()
        }

        val temporary = File(
            destinationDirectory,
            "${descriptor.runtimeFileName}.installing"
        )

        context.assets.open(descriptor.assetPath).use { input ->
            temporary.outputStream().use { output ->
                input.copyTo(output)
                (output as? java.io.FileOutputStream)?.fd?.sync()
            }
        }

        val verification = integrityVerifier.verifyFile(
            id = descriptor.id,
            file = temporary,
            expectedHashHex = descriptor.expectedSha256
        )

        require(verification.result == IntegrityResult.VALID) {
            "AI model integrity validation failed"
        }

        if (destination.exists()) destination.delete()
        require(temporary.renameTo(destination)) {
            "Unable to atomically install AI model"
        }

        return destination
    }
}
