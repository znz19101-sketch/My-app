
package com.guardexa.core.security

import com.guardexa.core.security.auth.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AdministratorSessionManagerTest {

    @Test
    fun sessionExpiresAutomatically() {
        var now = 1_000L
        val manager = AdministratorSessionManager(
            elapsedRealtime = { now },
            sessionDurationMillis = 5_000L
        )

        manager.createSession(
            sessionId = "test",
            method = AdministratorAuthMethod.PIN
        )

        assertNotNull(manager.currentSession())

        now = 6_001L
        assertNull(manager.currentSession())
    }
}
