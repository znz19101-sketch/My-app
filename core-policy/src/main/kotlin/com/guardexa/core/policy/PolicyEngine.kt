
package com.guardexa.core.policy

enum class VerificationState { VERIFIED, UNCERTAIN, FAILED }
enum class ProtectionAction { ALLOW, START_GRACE, BLOCK, REQUEST_RECHECK }

data class PolicyContext(
    val glassesDetected:Boolean,
    val faceVisible:Boolean,
    val graceExpired:Boolean,
    val trustedSession:Boolean,
    val appRequiresGlasses:Boolean,
    val verificationState: VerificationState
)

data class PolicyDecision(
    val action: ProtectionAction,
    val reason:String
)

class PolicyEngine {

    fun evaluate(ctx: PolicyContext): PolicyDecision {

        if(!ctx.appRequiresGlasses){
            return PolicyDecision(
                ProtectionAction.ALLOW,
                "Application exempt from glasses policy"
            )
        }

        if(ctx.verificationState==VerificationState.VERIFIED &&
            ctx.glassesDetected &&
            ctx.faceVisible){
            return PolicyDecision(
                ProtectionAction.ALLOW,
                "Verification successful"
            )
        }

        if(ctx.verificationState==VerificationState.UNCERTAIN &&
            !ctx.graceExpired){
            return PolicyDecision(
                ProtectionAction.START_GRACE,
                "Waiting for stable verification"
            )
        }

        if(ctx.verificationState==VerificationState.FAILED &&
            ctx.graceExpired){
            return PolicyDecision(
                ProtectionAction.BLOCK,
                "Verification failed after grace period"
            )
        }

        if(ctx.trustedSession){
            return PolicyDecision(
                ProtectionAction.REQUEST_RECHECK,
                "Trusted session requires recheck"
            )
        }

        return PolicyDecision(
            ProtectionAction.START_GRACE,
            "Default policy"
        )
    }
}
