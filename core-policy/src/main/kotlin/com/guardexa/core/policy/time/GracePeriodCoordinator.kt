
package com.guardexa.core.policy.time

enum class GraceState {
    IDLE,
    GUIDANCE,
    COUNTING_DOWN,
    RESOLVED,
    EXPIRED,
    CANCELLED_BY_ADMINISTRATOR
}

data class GraceSession(
    val key: String,
    val state: GraceState,
    val startedAtElapsedMillis: Long?,
    val durationMillis: Long,
    val resolvedAtElapsedMillis: Long? = null
) {
    fun remainingMillis(nowElapsedMillis: Long): Long {
        val start = startedAtElapsedMillis ?: return durationMillis
        return (durationMillis - (nowElapsedMillis - start)).coerceAtLeast(0L)
    }

    fun isExpired(nowElapsedMillis: Long): Boolean =
        state == GraceState.COUNTING_DOWN && remainingMillis(nowElapsedMillis) == 0L
}

class GracePeriodCoordinator(
    private val elapsedRealtime: () -> Long
) {
    private val sessions = linkedMapOf<String, GraceSession>()

    fun start(
        key: String,
        durationSeconds: Int
    ): GraceSession {
        require(durationSeconds in 0..3600) { "Grace period out of range" }

        val existing = sessions[key]
        if (existing?.state == GraceState.COUNTING_DOWN) return existing

        val session = GraceSession(
            key = key,
            state = GraceState.COUNTING_DOWN,
            startedAtElapsedMillis = elapsedRealtime(),
            durationMillis = durationSeconds * 1000L
        )
        sessions[key] = session
        return session
    }

    fun resolve(key: String): GraceSession? {
        val current = sessions[key] ?: return null
        val updated = current.copy(
            state = GraceState.RESOLVED,
            resolvedAtElapsedMillis = elapsedRealtime()
        )
        sessions[key] = updated
        return updated
    }

    fun cancelByAdministrator(key: String): GraceSession? {
        val current = sessions[key] ?: return null
        val updated = current.copy(
            state = GraceState.CANCELLED_BY_ADMINISTRATOR,
            resolvedAtElapsedMillis = elapsedRealtime()
        )
        sessions[key] = updated
        return updated
    }

    fun get(key: String): GraceSession? {
        val current = sessions[key] ?: return null
        if (current.isExpired(elapsedRealtime())) {
            val expired = current.copy(state = GraceState.EXPIRED)
            sessions[key] = expired
            return expired
        }
        return current
    }

    fun remove(key: String) {
        sessions.remove(key)
    }

    fun clearResolved() {
        sessions.entries.removeAll {
            it.value.state in setOf(
                GraceState.RESOLVED,
                GraceState.EXPIRED,
                GraceState.CANCELLED_BY_ADMINISTRATOR
            )
        }
    }
}
