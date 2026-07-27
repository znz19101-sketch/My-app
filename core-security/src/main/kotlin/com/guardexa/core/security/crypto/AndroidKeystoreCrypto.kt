
package com.guardexa.core.security.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class EncryptedPayload(
    val cipherText: ByteArray,
    val initializationVector: ByteArray,
    val keyAlias: String,
    val version: Int = 1
)

class AndroidKeystoreCrypto(
    private val keyStoreProvider: String = "AndroidKeyStore"
) {
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(keyStoreProvider).apply { load(null) }
    }

    fun encrypt(
        plainText: ByteArray,
        keyAlias: String
    ): EncryptedPayload {
        val key = getOrCreateKey(keyAlias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val encrypted = cipher.doFinal(plainText)

        return EncryptedPayload(
            cipherText = encrypted,
            initializationVector = cipher.iv,
            keyAlias = keyAlias
        )
    }

    fun decrypt(payload: EncryptedPayload): ByteArray {
        val key = getExistingKey(payload.keyAlias)
            ?: error("Required decryption key is unavailable")

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.initializationVector)
        )

        return cipher.doFinal(payload.cipherText)
    }

    fun containsKey(alias: String): Boolean =
        keyStore.containsAlias(alias)

    fun deleteKey(alias: String) {
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    private fun getExistingKey(alias: String): SecretKey? =
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)
            ?.secretKey

    private fun getOrCreateKey(alias: String): SecretKey =
        getExistingKey(alias) ?: createKey(alias)

    private fun createKey(alias: String): SecretKey {
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            keyStoreProvider
        )

        val specification = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        generator.init(specification)
        return generator.generateKey()
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
