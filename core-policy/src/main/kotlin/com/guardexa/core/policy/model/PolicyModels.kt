
package com.guardexa.core.policy.model

enum class VerificationStatus {
    VERIFIED,
    UNCERTAIN,
    FAILED,
    NOT_REQUIRED
}

enum class FacePresenceStatus {
    PRESENT,
    TEMPORARILY_MISSING,
    MISSING_LONG_ENOUGH_FOR_GRACE,
    MISSING_AND_GRACE_EXPIRED
}

enum class ScheduleStatus {
    ALLOWED,
    OUTSIDE_ALLOWED_WINDOW,
    NOT_CONFIGURED
}

enum class UsageLimitStatus {
    AVAILABLE,
    EXHAUSTED,
    NOT_CONFIGURED
}

enum class IntegrityStatus {
    HEALTHY,
    RECOVERING,
    SAFE_MODE_REQUIRED
}

enum class PermissionStatus {
    HEALTHY,
    DEGRADED,
    CRITICAL_PERMISSION_MISSING
}

enum class CurrentActivityPolicy {
    CLOSE_IMMEDIATELY,
    ALLOW_CURRENT_ACTIVITY_TO_FINISH,
    ALWAYS_ALLOWED,
    EMERGENCY
}

enum class DecisionReason {
    EMERGENCY_ALLOWED,
    ALWAYS_ALLOWED,
    SAFE_MODE_ACTIVE,
    CRITICAL_PERMISSION_MISSING,
    ADMINISTRATOR_OVERRIDE,
    PERMANENTLY_BLOCKED,
    OUTSIDE_SCHEDULE,
    DEVICE_LIMIT_EXHAUSTED,
    APP_LIMIT_EXHAUSTED,
    GLASSES_VERIFIED,
    GLASSES_UNCERTAIN,
    GLASSES_NOT_DETECTED,
    FACE_TEMPORARILY_MISSING,
    FACE_MISSING_TOO_LONG,
    CURRENT_ACTIVITY_MAY_FINISH,
    GLOBAL_POLICY_ALLOWED,
    DEFAULT_DENY
}

enum class FinalProtectionAction {
    NONE,
    SHOW_GUIDANCE,
    START_GRACE_PERIOD,
    BLOCK_APPLICATION,
    SHOW_PROTECTION_SCREEN,
    REQUIRE_ADMINISTRATOR_AUTH,
    SAFE_MODE_GATE
}

data class AdministratorOverride(
    val active: Boolean,
    val expiresAtEpochMillis: Long? = null,
    val targetPackageName: String? = null
) {
    fun appliesTo(packageName: String, nowEpochMillis: Long): Boolean {
        if (!active) return false
        if (expiresAtEpochMillis != null && nowEpochMillis >= expiresAtEpochMillis) return false
        return targetPackageName == null || targetPackageName == packageName
    }
}

data class ApplicationPolicySnapshot(
    val packageName: String,
    val permanentlyBlocked: Boolean,
    val requiresGlasses: Boolean,
    val alwaysAllowed: Boolean,
    val emergency: Boolean,
    val activityPolicy: CurrentActivityPolicy,
    val gracePeriodSeconds: Int
)

data class PolicyDecisionInput(
    val nowEpochMillis: Long,
    val packageName: String,
    val appPolicy: ApplicationPolicySnapshot,
    val safeModeEnabled: Boolean,
    val allowedPackagesInSafeMode: Set<String>,
    val integrityStatus: IntegrityStatus,
    val permissionStatus: PermissionStatus,
    val administratorOverride: AdministratorOverride?,
    val scheduleStatus: ScheduleStatus,
    val deviceLimitStatus: UsageLimitStatus,
    val appLimitStatus: UsageLimitStatus,
    val verificationStatus: VerificationStatus,
    val facePresenceStatus: FacePresenceStatus,
    val currentActivityRunning: Boolean,
    val globalPolicyAllows: Boolean
)

sealed interface PolicyDecision {
    val reason: DecisionReason
    val shouldLog: Boolean

    data class Allow(
        override val reason: DecisionReason,
        override val shouldLog: Boolean = false
    ) : PolicyDecision

    data class ShowGuidance(
        override val reason: DecisionReason,
        val messageCode: String,
        val recheckAfterMillis: Long,
        override val shouldLog: Boolean = false
    ) : PolicyDecision

    data class StartGracePeriod(
        override val reason: DecisionReason,
        val durationSeconds: Int,
        val finalAction: FinalProtectionAction,
        override val shouldLog: Boolean = true
    ) : PolicyDecision

    data class AllowCurrentActivityOnly(
        override val reason: DecisionReason,
        val maximumDurationSeconds: Int?,
        override val shouldLog: Boolean = true
    ) : PolicyDecision

    data class Block(
        override val reason: DecisionReason,
        val action: FinalProtectionAction,
        override val shouldLog: Boolean = true
    ) : PolicyDecision

    data class RequireAdministrator(
        override val reason: DecisionReason,
        override val shouldLog: Boolean = true
    ) : PolicyDecision
}
