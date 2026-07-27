
package com.guardexa.core.policy.engine

import com.guardexa.core.policy.model.*

class PolicyDecisionEngine {

    fun evaluate(input: PolicyDecisionInput): PolicyDecision {
        val app = input.appPolicy

        if (app.emergency || app.activityPolicy == CurrentActivityPolicy.EMERGENCY) {
            return PolicyDecision.Allow(DecisionReason.EMERGENCY_ALLOWED)
        }

        if (input.safeModeEnabled || input.integrityStatus == IntegrityStatus.SAFE_MODE_REQUIRED) {
            return if (input.packageName in input.allowedPackagesInSafeMode) {
                PolicyDecision.Allow(DecisionReason.SAFE_MODE_ACTIVE)
            } else {
                PolicyDecision.Block(
                    reason = DecisionReason.SAFE_MODE_ACTIVE,
                    action = FinalProtectionAction.SAFE_MODE_GATE
                )
            }
        }

        if (input.permissionStatus == PermissionStatus.CRITICAL_PERMISSION_MISSING) {
            return PolicyDecision.Block(
                reason = DecisionReason.CRITICAL_PERMISSION_MISSING,
                action = FinalProtectionAction.SAFE_MODE_GATE
            )
        }

        if (input.administratorOverride?.appliesTo(
                packageName = input.packageName,
                nowEpochMillis = input.nowEpochMillis
            ) == true
        ) {
            return PolicyDecision.Allow(DecisionReason.ADMINISTRATOR_OVERRIDE)
        }

        if (app.alwaysAllowed || app.activityPolicy == CurrentActivityPolicy.ALWAYS_ALLOWED) {
            return PolicyDecision.Allow(DecisionReason.ALWAYS_ALLOWED)
        }

        if (app.permanentlyBlocked) {
            return PolicyDecision.Block(
                reason = DecisionReason.PERMANENTLY_BLOCKED,
                action = FinalProtectionAction.SHOW_PROTECTION_SCREEN
            )
        }

        if (input.scheduleStatus == ScheduleStatus.OUTSIDE_ALLOWED_WINDOW) {
            return decideOnRestriction(
                input = input,
                reason = DecisionReason.OUTSIDE_SCHEDULE
            )
        }

        if (input.deviceLimitStatus == UsageLimitStatus.EXHAUSTED) {
            return decideOnRestriction(
                input = input,
                reason = DecisionReason.DEVICE_LIMIT_EXHAUSTED
            )
        }

        if (input.appLimitStatus == UsageLimitStatus.EXHAUSTED) {
            return decideOnRestriction(
                input = input,
                reason = DecisionReason.APP_LIMIT_EXHAUSTED
            )
        }

        if (!app.requiresGlasses || input.verificationStatus == VerificationStatus.NOT_REQUIRED) {
            return if (input.globalPolicyAllows) {
                PolicyDecision.Allow(DecisionReason.GLOBAL_POLICY_ALLOWED)
            } else {
                PolicyDecision.Block(
                    reason = DecisionReason.DEFAULT_DENY,
                    action = FinalProtectionAction.SHOW_PROTECTION_SCREEN
                )
            }
        }

        return evaluateGlassesRequirement(input)
    }

    private fun evaluateGlassesRequirement(
        input: PolicyDecisionInput
    ): PolicyDecision {
        return when (input.facePresenceStatus) {
            FacePresenceStatus.TEMPORARILY_MISSING ->
                PolicyDecision.Allow(
                    reason = DecisionReason.FACE_TEMPORARILY_MISSING
                )

            FacePresenceStatus.MISSING_LONG_ENOUGH_FOR_GRACE ->
                PolicyDecision.StartGracePeriod(
                    reason = DecisionReason.FACE_MISSING_TOO_LONG,
                    durationSeconds = input.appPolicy.gracePeriodSeconds,
                    finalAction = FinalProtectionAction.SHOW_PROTECTION_SCREEN
                )

            FacePresenceStatus.MISSING_AND_GRACE_EXPIRED ->
                PolicyDecision.Block(
                    reason = DecisionReason.FACE_MISSING_TOO_LONG,
                    action = FinalProtectionAction.SHOW_PROTECTION_SCREEN
                )

            FacePresenceStatus.PRESENT ->
                when (input.verificationStatus) {
                    VerificationStatus.VERIFIED ->
                        PolicyDecision.Allow(DecisionReason.GLASSES_VERIFIED)

                    VerificationStatus.UNCERTAIN ->
                        PolicyDecision.ShowGuidance(
                            reason = DecisionReason.GLASSES_UNCERTAIN,
                            messageCode = "guidance_hold_still",
                            recheckAfterMillis = 800L
                        )

                    VerificationStatus.FAILED ->
                        PolicyDecision.StartGracePeriod(
                            reason = DecisionReason.GLASSES_NOT_DETECTED,
                            durationSeconds = input.appPolicy.gracePeriodSeconds,
                            finalAction = FinalProtectionAction.SHOW_PROTECTION_SCREEN
                        )

                    VerificationStatus.NOT_REQUIRED ->
                        PolicyDecision.Allow(DecisionReason.GLOBAL_POLICY_ALLOWED)
                }
        }
    }

    private fun decideOnRestriction(
        input: PolicyDecisionInput,
        reason: DecisionReason
    ): PolicyDecision {
        val policy = input.appPolicy.activityPolicy

        if (
            input.currentActivityRunning &&
            policy == CurrentActivityPolicy.ALLOW_CURRENT_ACTIVITY_TO_FINISH
        ) {
            return PolicyDecision.AllowCurrentActivityOnly(
                reason = DecisionReason.CURRENT_ACTIVITY_MAY_FINISH,
                maximumDurationSeconds = 15 * 60
            )
        }

        return when (policy) {
            CurrentActivityPolicy.CLOSE_IMMEDIATELY ->
                PolicyDecision.Block(
                    reason = reason,
                    action = FinalProtectionAction.SHOW_PROTECTION_SCREEN
                )

            CurrentActivityPolicy.ALLOW_CURRENT_ACTIVITY_TO_FINISH ->
                PolicyDecision.StartGracePeriod(
                    reason = reason,
                    durationSeconds = input.appPolicy.gracePeriodSeconds,
                    finalAction = FinalProtectionAction.SHOW_PROTECTION_SCREEN
                )

            CurrentActivityPolicy.ALWAYS_ALLOWED ->
                PolicyDecision.Allow(DecisionReason.ALWAYS_ALLOWED)

            CurrentActivityPolicy.EMERGENCY ->
                PolicyDecision.Allow(DecisionReason.EMERGENCY_ALLOWED)
        }
    }
}
