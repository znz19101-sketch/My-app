
package com.guardexa.ai.core.runtime

import com.guardexa.ai.core.model.AiPowerState

data class RuntimeSignals(
    val protectedAppInForeground: Boolean,
    val verificationStable: Boolean,
    val confidenceFalling: Boolean,
    val spoofSuspected: Boolean,
    val thermalLevel: Int,
    val batterySaverEnabled: Boolean,
    val screenInteractive: Boolean
)

data class RuntimePlan(
    val powerState: AiPowerState,
    val targetAnalysisIntervalMillis: Long,
    val enableLiveness: Boolean,
    val enableSpoofDetection: Boolean,
    val keepModelsWarm: Boolean
)

class AiRuntimeGovernor {
    fun plan(signals: RuntimeSignals): RuntimePlan {
        if (!signals.screenInteractive || !signals.protectedAppInForeground) {
            return RuntimePlan(
                powerState = AiPowerState.SLEEP,
                targetAnalysisIntervalMillis = Long.MAX_VALUE,
                enableLiveness = false,
                enableSpoofDetection = false,
                keepModelsWarm = false
            )
        }

        if (signals.spoofSuspected) {
            return RuntimePlan(
                powerState = AiPowerState.EMERGENCY,
                targetAnalysisIntervalMillis = 120L,
                enableLiveness = true,
                enableSpoofDetection = true,
                keepModelsWarm = true
            )
        }

        if (signals.thermalLevel >= 4) {
            return RuntimePlan(
                powerState = AiPowerState.PASSIVE,
                targetAnalysisIntervalMillis = 1_500L,
                enableLiveness = false,
                enableSpoofDetection = false,
                keepModelsWarm = true
            )
        }

        if (signals.confidenceFalling) {
            return RuntimePlan(
                powerState = AiPowerState.INTENSIVE,
                targetAnalysisIntervalMillis = 200L,
                enableLiveness = true,
                enableSpoofDetection = true,
                keepModelsWarm = true
            )
        }

        if (signals.verificationStable) {
            return RuntimePlan(
                powerState = AiPowerState.PASSIVE,
                targetAnalysisIntervalMillis = if (
                    signals.batterySaverEnabled
                ) 2_000L else 1_000L,
                enableLiveness = false,
                enableSpoofDetection = false,
                keepModelsWarm = true
            )
        }

        return RuntimePlan(
            powerState = AiPowerState.ACTIVE,
            targetAnalysisIntervalMillis = if (
                signals.batterySaverEnabled
            ) 700L else 400L,
            enableLiveness = true,
            enableSpoofDetection = false,
            keepModelsWarm = true
        )
    }
}
