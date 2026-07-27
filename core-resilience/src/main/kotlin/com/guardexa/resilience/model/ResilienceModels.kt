
package com.guardexa.resilience.model

enum class ComponentHealth {
    HEALTHY,
    DEGRADED,
    FAILED,
    MISSING,
    RECOVERING
}

enum class SelfTestComponent {
    DATABASE,
    SECURITY_KEYS,
    AI_MODELS,
    CAMERA_PERMISSION,
    USAGE_ACCESS,
    NOTIFICATION_PERMISSION,
    PROTECTION_SERVICE,
    ACTIVE_PROFILE,
    ENCRYPTED_STORAGE,
    DEVICE_OWNER_CAPABILITY
}

enum class SelfTestSeverity {
    PASS,
    WARNING,
    RECOVERABLE_FAILURE,
    SAFE_MODE_REQUIRED,
    CRITICAL
}

data class ComponentSelfTestResult(
    val component: SelfTestComponent,
    val health: ComponentHealth,
    val severity: SelfTestSeverity,
    val reasonCode: String?,
    val recoverable: Boolean,
    val checkedAtEpochMillis: Long
)

data class FullSelfTestReport(
    val id: String,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val results: List<ComponentSelfTestResult>,
    val safeModeRequired: Boolean,
    val criticalFailure: Boolean
)

enum class SafeModeReason {
    CRITICAL_PERMISSION_MISSING,
    DATABASE_UNAVAILABLE,
    AI_MODEL_CORRUPTED,
    ENCRYPTION_KEY_MISSING,
    PROTECTION_SERVICE_UNSTABLE,
    INTEGRITY_FAILURE,
    MANUAL_ADMINISTRATOR_ACTION,
    UNKNOWN
}

data class SafeModeState(
    val enabled: Boolean,
    val reason: SafeModeReason?,
    val enabledAtEpochMillis: Long?,
    val allowedPackages: Set<String>,
    val recoveryAttempts: Int,
    val lastRecoveryAtEpochMillis: Long?
)

data class WatchdogTargetState(
    val id: String,
    val healthy: Boolean,
    val lastHeartbeatElapsedMillis: Long?,
    val restartAttempts: Int,
    val lastRestartElapsedMillis: Long?
)
