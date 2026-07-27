
package com.guardexa.core.policy.engine

import com.guardexa.core.policy.model.*
import com.guardexa.core.policy.time.GracePeriodCoordinator

data class CoordinatedDecision(
    val decision: PolicyDecision,
    val activeGraceRemainingMillis: Long? = null
)

class PolicyCoordinator(
    private val engine: PolicyDecisionEngine,
    private val graceCoordinator: GracePeriodCoordinator,
    private val elapsedRealtime: () -> Long
) {
    fun evaluate(input: PolicyDecisionInput): CoordinatedDecision {
        val decision = engine.evaluate(input)
        val key = graceKey(input.packageName)

        return when (decision) {
            is PolicyDecision.StartGracePeriod -> {
                val session = graceCoordinator.start(
                    key = key,
                    durationSeconds = decision.durationSeconds
                )

                CoordinatedDecision(
                    decision = decision,
                    activeGraceRemainingMillis = session.remainingMillis(elapsedRealtime())
                )
            }

            is PolicyDecision.Allow -> {
                graceCoordinator.resolve(key)
                CoordinatedDecision(decision = decision)
            }

            is PolicyDecision.Block -> {
                val session = graceCoordinator.get(key)
                if (session != null && session.state.name == "COUNTING_DOWN") {
                    CoordinatedDecision(
                        decision = PolicyDecision.StartGracePeriod(
                            reason = decision.reason,
                            durationSeconds = (session.durationMillis / 1000L).toInt(),
                            finalAction = decision.action
                        ),
                        activeGraceRemainingMillis = session.remainingMillis(elapsedRealtime())
                    )
                } else {
                    CoordinatedDecision(decision = decision)
                }
            }

            else -> CoordinatedDecision(decision = decision)
        }
    }

    fun cancelGraceBecauseGlassesReturned(packageName: String) {
        graceCoordinator.resolve(graceKey(packageName))
    }

    fun cancelGraceByAdministrator(packageName: String) {
        graceCoordinator.cancelByAdministrator(graceKey(packageName))
    }

    private fun graceKey(packageName: String): String =
        "app:$packageName"
}
