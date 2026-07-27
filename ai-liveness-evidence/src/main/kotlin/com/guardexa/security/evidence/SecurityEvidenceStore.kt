
package com.guardexa.security.evidence

import android.graphics.Bitmap
import com.guardexa.core.security.crypto.AndroidKeystoreCrypto
import com.guardexa.core.security.crypto.EncryptedPayload
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

data class SecurityEvidenceMetadata(
    val id: String,
    val encryptedFileName: String,
    val capturedAtEpochMillis: Long,
    val eventType: String,
    val width: Int,
    val height: Int,
    val encryptedSizeBytes: Long,
    val encryptionVersion: Int
)

interface SecurityEvidenceIndex {
    suspend fun insert(metadata: SecurityEvidenceMetadata)
    suspend fun listOldestFirst(): List<SecurityEvidenceMetadata>
    suspend fun delete(id: String)
}

class SecurityEvidenceStore(
    private val directory: File,
    private val index: SecurityEvidenceIndex,
    private val crypto: AndroidKeystoreCrypto,
    private val keyAlias: String = "guardexa_security_evidence_v1",
    private val maximumImages: Int = 30,
    private val targetWidth: Int = 320,
    private val jpegQuality: Int = 55
) {
    suspend fun save(
        source: Bitmap,
        eventType: String,
        nowEpochMillis: Long
    ): SecurityEvidenceMetadata {
        require(maximumImages > 0)

        directory.mkdirs()

        val resized = resizePreservingAspectRatio(source, targetWidth)
        val plain = ByteArrayOutputStream().use { output ->
            check(resized.compress(Bitmap.CompressFormat.JPEG, jpegQuality, output)) {
                "Image compression failed"
            }
            output.toByteArray()
        }

        val encrypted = crypto.encrypt(
            plainText = plain,
            keyAlias = keyAlias
        )
        plain.fill(0)

        val id = UUID.randomUUID().toString()
        val fileName = "$id.gxe"
        val destination = File(directory, fileName)
        val temporary = File(directory, "$fileName.tmp")

        writePayload(temporary, encrypted)

        if (destination.exists() && !destination.delete()) {
            temporary.delete()
            resized.recycleIfDifferentFrom(source)
            error("Unable to replace security evidence")
        }

        check(temporary.renameTo(destination)) {
            temporary.delete()
            resized.recycleIfDifferentFrom(source)
            "Unable to commit security evidence"
        }

        val metadata = SecurityEvidenceMetadata(
            id = id,
            encryptedFileName = fileName,
            capturedAtEpochMillis = nowEpochMillis,
            eventType = eventType,
            width = resized.width,
            height = resized.height,
            encryptedSizeBytes = destination.length(),
            encryptionVersion = encrypted.version
        )

        resized.recycleIfDifferentFrom(source)

        try {
            index.insert(metadata)
            enforceLimit()
        } catch (t: Throwable) {
            destination.delete()
            throw t
        }

        return metadata
    }

    suspend fun delete(id: String) {
        val metadata = index.listOldestFirst()
            .firstOrNull { it.id == id }
            ?: return

        File(directory, metadata.encryptedFileName).delete()
        index.delete(id)
    }

    private suspend fun enforceLimit() {
        val all = index.listOldestFirst()
        val excess = all.size - maximumImages
        if (excess <= 0) return

        all.take(excess).forEach { old ->
            File(directory, old.encryptedFileName).delete()
            index.delete(old.id)
        }
    }

    private fun writePayload(
        file: File,
        payload: EncryptedPayload
    ) {
        file.outputStream().use { raw ->
            java.io.DataOutputStream(raw).use { output ->
                output.writeInt(payload.version)
                output.writeUTF(payload.keyAlias)
                output.writeInt(payload.initializationVector.size)
                output.write(payload.initializationVector)
                output.writeInt(payload.cipherText.size)
                output.write(payload.cipherText)
                output.flush()
                (raw as? java.io.FileOutputStream)?.fd?.sync()
            }
        }
    }

    private fun resizePreservingAspectRatio(
        source: Bitmap,
        targetWidth: Int
    ): Bitmap {
        if (source.width <= targetWidth) return source
        val ratio = targetWidth.toFloat() / source.width.toFloat()
        val targetHeight = (source.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(
            source,
            targetWidth,
            targetHeight,
            true
        )
    }

    private fun Bitmap.recycleIfDifferentFrom(source: Bitmap) {
        if (this !== source && !isRecycled) recycle()
    }
}
