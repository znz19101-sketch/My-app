
package com.guardexa.resilience.selftest

import com.guardexa.resilience.model.*
import java.util.UUID

interface SelfTestCheck {
    val component: SelfTestComponent
    suspend fun run(): ComponentSelfTestResult
}

class SelfTestEngine(
    checks: Set<SelfTestCheck>,
    private val epochTime: () -> Long
) {
    private val orderedChecks = checks.sortedBy { it.component.ordinal }

    suspend fun runFull(): FullSelfTestReport {
        val started = epochTime()
        val results = mutableListOf<ComponentSelfTestResult>()

        for (check in orderedChecks) {
            val result = runCatching { check.run() }
                .getOrElse { error ->
                    ComponentSelfTestResult(
                        component = check.component,
                        health = ComponentHealth.FAILED,
                        severity = SelfTestSeverity.SAFE_MODE_REQUIRED,
                        reasonCode = error.message ?: "self_test_exception",
                        recoverable = true,
                        checkedAtEpochMillis = epochTime()
                    )
                }

            results += result
        }

        val safeMode = results.any {
            it.severity in setOf(
                SelfTestSeverity.SAFE_MODE_REQUIRED,
                SelfTestSeverity.CRITICAL
            )
        }

        val critical = results.any {
            it.severity == SelfTestSeverity.CRITICAL
        }

        return FullSelfTestReport(
            id = UUID.randomUUID().toString(),
            startedAtEpochMillis = started,
            completedAtEpochMillis = epochTime(),
            results = results,
            safeModeRequired = safeMode,
            criticalFailure = critical
        )
    }
}
