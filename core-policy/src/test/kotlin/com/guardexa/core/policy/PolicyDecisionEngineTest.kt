
package com.guardexa.core.policy

import com.guardexa.core.policy.engine.PolicyDecisionEngine
import com.guardexa.core.policy.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PolicyDecisionEngineTest {

    private val engine = PolicyDecisionEngine()

    @Test
    fun emergencyAppIsAlwaysAllowed() {
        val result = engine.evaluate(
            baseInput(
                appPolicy = basePolicy(
                    emergency = true,
                    activityPolicy = CurrentActivityPolicy.EMERGENCY
                )
            )
        )

        assertIs<PolicyDecision.Allow>(result)
        assertEquals(DecisionReason.EMERGENCY_ALLOWED, result.reason)
    }

    @Test
    fun safeModeBlocksNonAllowedPackage() {
        val result = engine.evaluate(
            baseInput(
                safeModeEnabled = true,
                allowedPackagesInSafeMode = setOf("com.phone")
            )
        )

        assertIs<PolicyDecision.Block>(result)
        assertEquals(DecisionReason.SAFE_MODE_ACTIVE, result.reason)
    }

    @Test
    fun verifiedGlassesAllowsProtectedApplication() {
        val result = engine.evaluate(
            baseInput(
                verificationStatus = VerificationStatus.VERIFIED,
                facePresenceStatus = FacePresenceStatus.PRESENT
            )
        )

        assertIs<PolicyDecision.Allow>(result)
        assertEquals(DecisionReason.GLASSES_VERIFIED, result.reason)
    }

    @Test
    fun failedGlassesStartsGracePeriod() {
        val result = engine.evaluate(
            baseInput(
                verificationStatus = VerificationStatus.FAILED,
                facePresenceStatus = FacePresenceStatus.PRESENT
            )
        )

        assertIs<PolicyDecision.StartGracePeriod>(result)
        assertEquals(30, result.durationSeconds)
    }

    @Test
    fun shortFaceAbsenceDoesNotBlock() {
        val result = engine.evaluate(
            baseInput(
                verificationStatus = VerificationStatus.UNCERTAIN,
                facePresenceStatus = FacePresenceStatus.TEMPORARILY_MISSING
            )
        )

        assertIs<PolicyDecision.Allow>(result)
        assertEquals(DecisionReason.FACE_TEMPORARILY_MISSING, result.reason)
    }

    @Test
    fun exhaustedLimitMayAllowCurrentActivityToFinish() {
        val result = engine.evaluate(
            baseInput(
                appLimitStatus = UsageLimitStatus.EXHAUSTED,
                currentActivityRunning = true,
                appPolicy = basePolicy(
                    activityPolicy = CurrentActivityPolicy.ALLOW_CURRENT_ACTIVITY_TO_FINISH
                )
            )
        )

        assertIs<PolicyDecision.AllowCurrentActivityOnly>(result)
    }

    private fun basePolicy(
        emergency: Boolean = false,
        activityPolicy: CurrentActivityPolicy = CurrentActivityPolicy.CLOSE_IMMEDIATELY
    ) = ApplicationPolicySnapshot(
        packageName = "com.example.game",
        permanentlyBlocked = false,
        requiresGlasses = true,
        alwaysAllowed = false,
        emergency = emergency,
        activityPolicy = activityPolicy,
        gracePeriodSeconds = 30
    )

    private fun baseInput(
        appPolicy: ApplicationPolicySnapshot = basePolicy(),
        safeModeEnabled: Boolean = false,
        allowedPackagesInSafeMode: Set<String> = emptySet(),
        verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
        facePresenceStatus: FacePresenceStatus = FacePresenceStatus.PRESENT,
        appLimitStatus: UsageLimitStatus = UsageLimitStatus.AVAILABLE,
        currentActivityRunning: Boolean = false
    ) = PolicyDecisionInput(
        nowEpochMillis = 1_000L,
        packageName = appPolicy.packageName,
        appPolicy = appPolicy,
        safeModeEnabled = safeModeEnabled,
        allowedPackagesInSafeMode = allowedPackagesInSafeMode,
        integrityStatus = IntegrityStatus.HEALTHY,
        permissionStatus = PermissionStatus.HEALTHY,
        administratorOverride = null,
        scheduleStatus = ScheduleStatus.ALLOWED,
        deviceLimitStatus = UsageLimitStatus.AVAILABLE,
        appLimitStatus = appLimitStatus,
        verificationStatus = verificationStatus,
        facePresenceStatus = facePresenceStatus,
        currentActivityRunning = currentActivityRunning,
        globalPolicyAllows = true
    )
}
