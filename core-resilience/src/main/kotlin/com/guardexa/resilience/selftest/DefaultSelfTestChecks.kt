
package com.guardexa.resilience.selftest

import com.guardexa.resilience.model.*

interface DatabaseHealthProbe {
    suspend fun canReadAndWrite(): Boolean
}

interface SecurityKeyProbe {
    suspend fun requiredKeysAvailable(): Boolean
}

interface AiModelProbe {
    suspend fun allRequiredModelsValid(): Boolean
}

interface PermissionHealthProbe {
    suspend fun cameraGranted(): Boolean
    suspend fun usageAccessGranted(): Boolean
    suspend fun notificationsHealthy(): Boolean
}

interface ProtectionServiceProbe {
    suspend fun isRunning(): Boolean
}

interface ActiveProfileProbe {
    suspend fun hasActiveProfile(): Boolean
}

interface EncryptedStorageProbe {
    suspend fun canEncryptAndDecrypt(): Boolean
}

class DatabaseSelfTest(
    private val probe: DatabaseHealthProbe,
    private val epochTime: () -> Long
) : SelfTestCheck {
    override val component = SelfTestComponent.DATABASE

    override suspend fun run(): ComponentSelfTestResult =
        if (probe.canReadAndWrite()) {
            pass()
        } else {
            fail(
                severity = SelfTestSeverity.SAFE_MODE_REQUIRED,
                reason = "database_unavailable"
            )
        }

    private fun pass() = ComponentSelfTestResult(
        component, ComponentHealth.HEALTHY,
        SelfTestSeverity.PASS, null, false, epochTime()
    )

    private fun fail(
        severity: SelfTestSeverity,
        reason: String
    ) = ComponentSelfTestResult(
        component, ComponentHealth.FAILED,
        severity, reason, true, epochTime()
    )
}

class SecurityKeysSelfTest(
    private val probe: SecurityKeyProbe,
    private val epochTime: () -> Long
) : SelfTestCheck {
    override val component = SelfTestComponent.SECURITY_KEYS

    override suspend fun run(): ComponentSelfTestResult =
        if (probe.requiredKeysAvailable()) {
            result(ComponentHealth.HEALTHY, SelfTestSeverity.PASS, null, false)
        } else {
            result(
                ComponentHealth.MISSING,
                SelfTestSeverity.CRITICAL,
                "security_key_missing",
                false
            )
        }

    private fun result(
        health: ComponentHealth,
        severity: SelfTestSeverity,
        reason: String?,
        recoverable: Boolean
    ) = ComponentSelfTestResult(
        component, health, severity, reason, recoverable, epochTime()
    )
}

class AiModelsSelfTest(
    private val probe: AiModelProbe,
    private val epochTime: () -> Long
) : SelfTestCheck {
    override val component = SelfTestComponent.AI_MODELS

    override suspend fun run(): ComponentSelfTestResult =
        if (probe.allRequiredModelsValid()) {
            ComponentSelfTestResult(
                component, ComponentHealth.HEALTHY,
                SelfTestSeverity.PASS, null, false, epochTime()
            )
        } else {
            ComponentSelfTestResult(
                component, ComponentHealth.FAILED,
                SelfTestSeverity.SAFE_MODE_REQUIRED,
                "ai_model_integrity_failure",
                true,
                epochTime()
            )
        }
}

class CameraPermissionSelfTest(
    private val probe: PermissionHealthProbe,
    private val epochTime: () -> Long
) : SelfTestCheck {
    override val component = SelfTestComponent.CAMERA_PERMISSION

    override suspend fun run(): ComponentSelfTestResult =
        if (probe.cameraGranted()) {
            ComponentSelfTestResult(
                component, ComponentHealth.HEALTHY,
                SelfTestSeverity.PASS, null, false, epochTime()
            )
        } else {
            ComponentSelfTestResult(
                component, ComponentHealth.MISSING,
                SelfTestSeverity.SAFE_MODE_REQUIRED,
                "camera_permission_missing",
                true,
                epochTime()
            )
        }
}

class UsageAccessSelfTest(
    private val probe: PermissionHealthProbe,
    private val epochTime: () -> Long
) : SelfTestCheck {
    override val component = SelfTestComponent.USAGE_ACCESS

    override suspend fun run(): ComponentSelfTestResult =
        if (probe.usageAccessGranted()) {
            ComponentSelfTestResult(
                component, ComponentHealth.HEALTHY,
                SelfTestSeverity.PASS, null, false, epochTime()
            )
        } else {
            ComponentSelfTestResult(
                component, ComponentHealth.MISSING,
                SelfTestSeverity.SAFE_MODE_REQUIRED,
                "usage_access_missing",
                true,
                epochTime()
            )
        }
}
