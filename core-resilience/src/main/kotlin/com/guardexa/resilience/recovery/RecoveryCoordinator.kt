
package com.guardexa.resilience.recovery

import com.guardexa.resilience.model.*
import com.guardexa.resilience.safemode.SafeModeController
import com.guardexa.resilience.selftest.SelfTestEngine

interface RecoverableComponent {
    val component: SelfTestComponent
    suspend fun recover(): Boolean
}

data class RecoverySummary(
    val attemptedComponents: List<SelfTestComponent>,
    val recoveredComponents: List<SelfTestComponent>,
    val failedComponents: List<SelfTestComponent>,
    val safeModeDisabled: Boolean
)

class RecoveryCoordinator(
    private val selfTestEngine: SelfTestEngine,
    private val safeModeController: SafeModeController,
    recoverableComponents: Set<RecoverableComponent>
) {
    private val components = recoverableComponents.associateBy { it.component }

    suspend fun recover(): RecoverySummary {
        safeModeController.registerRecoveryAttempt()

        val before = selfTestEngine.runFull()
        val attempted = mutableListOf<SelfTestComponent>()
        val recovered = mutableListOf<SelfTestComponent>()
        val failed = mutableListOf<SelfTestComponent>()

        before.results
            .filter { it.recoverable && it.severity != SelfTestSeverity.PASS }
            .forEach { result ->
                val component = components[result.component] ?: return@forEach
                attempted += result.component

                if (runCatching { component.recover() }.getOrDefault(false)) {
                    recovered += result.component
                } else {
                    failed += result.component
                }
            }

        val after = selfTestEngine.runFull()
        val canDisableSafeMode =
            !after.safeModeRequired &&
                !after.criticalFailure

        if (canDisableSafeMode) {
            safeModeController.disableAfterVerifiedRecovery()
        }

        return RecoverySummary(
            attemptedComponents = attempted,
            recoveredComponents = recovered,
            failedComponents = failed,
            safeModeDisabled = canDisableSafeMode
        )
    }
}
