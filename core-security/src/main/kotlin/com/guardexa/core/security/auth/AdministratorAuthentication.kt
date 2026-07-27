
package com.guardexa.core.security.auth

enum class AdministratorAuthMethod {
    PIN,
    BIOMETRIC
}

data class AdministratorSession(
    val sessionId: String,
    val method: AdministratorAuthMethod,
    val createdAtElapsedMillis: Long,
    val expiresAtElapsedMillis: Long
) {
    fun isValid(nowElapsedMillis: Long): Boolean =
        nowElapsedMillis < expiresAtElapsedMillis
}

class AdministratorSessionManager(
    private val elapsedRealtime: () -> Long,
    private val sessionDurationMillis: Long = 120_000L
) {
    private var activeSession: AdministratorSession? = null

    fun createSession(
        sessionId: String,
        method: AdministratorAuthMethod
    ): AdministratorSession {
        val now = elapsedRealtime()
        return AdministratorSession(
            sessionId = sessionId,
            method = method,
            createdAtElapsedMillis = now,
            expiresAtElapsedMillis = now + sessionDurationMillis
        ).also {
            activeSession = it
        }
    }

    fun currentSession(): AdministratorSession? {
        val session = activeSession ?: return null
        return if (session.isValid(elapsedRealtime())) {
            session
        } else {
            activeSession = null
            null
        }
    }

    fun invalidate() {
        activeSession = null
    }
}

interface BiometricAuthenticator {
    suspend fun authenticate(
        title: String,
        subtitle: String?
    ): BiometricAuthenticationResult
}

sealed interface BiometricAuthenticationResult {
    data object Success : BiometricAuthenticationResult
    data object Cancelled : BiometricAuthenticationResult
    data class Failed(val reason: String) : BiometricAuthenticationResult
    data object NotAvailable : BiometricAuthenticationResult
}
