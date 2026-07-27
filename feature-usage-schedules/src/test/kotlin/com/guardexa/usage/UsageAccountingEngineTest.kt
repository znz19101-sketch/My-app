
package com.guardexa.usage

import com.guardexa.usage.engine.UsageAccountingEngine
import com.guardexa.usage.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UsageAccountingEngineTest {

    @Test
    fun deviceBonusExtendsLimit() {
        val engine = UsageAccountingEngine(
            epochTime = { 1_000L },
            elapsedRealtime = { 1_000L }
        )

        val snapshot = engine.calculateLimit(
            baseLimitMillis = 60 * 60_000L,
            usedMillis = 60 * 60_000L,
            bonusGrants = listOf(
                BonusTimeGrant(
                    id = "bonus",
                    targetType = BonusTargetType.DEVICE,
                    targetPackageName = null,
                    durationMillis = 15 * 60_000L,
                    grantedAtEpochMillis = 0L,
                    expiresAtEpochMillis = null,
                    reason = null,
                    active = true
                )
            ),
            packageName = "com.example",
            nowEpochMillis = 1_000L
        )

        assertFalse(snapshot.exhausted)
        assertEquals(15 * 60_000L, snapshot.remainingMillis)
    }

    @Test
    fun exhaustedWithoutBonusIsBlocked() {
        val engine = UsageAccountingEngine(
            epochTime = { 1_000L },
            elapsedRealtime = { 1_000L }
        )

        val snapshot = engine.calculateLimit(
            baseLimitMillis = 30 * 60_000L,
            usedMillis = 30 * 60_000L,
            bonusGrants = emptyList(),
            packageName = "com.example",
            nowEpochMillis = 1_000L
        )

        assertTrue(snapshot.exhausted)
    }
}
