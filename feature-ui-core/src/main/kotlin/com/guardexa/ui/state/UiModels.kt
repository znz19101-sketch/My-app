
package com.guardexa.ui.state

enum class SetupStep {
    WELCOME,
    PRIVACY,
    CREATE_PIN,
    CONFIRM_PIN,
    BIOMETRIC,
    PROTECTION_MODE,
    PERMISSIONS,
    CHILD_PROFILE,
    GLASSES_POLICY,
    APPLICATIONS,
    FIRST_PROFILE,
    DAILY_LIMITS,
    SCHEDULES,
    CALIBRATION,
    REVIEW,
    ACTIVATION_COUNTDOWN,
    COMPLETED
}

enum class PermissionCardState {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    DENIED_PERMANENTLY,
    RESTRICTED_BY_SYSTEM,
    LOST_AFTER_ACTIVATION
}

enum class ProtectionMode {
    STANDARD,
    ADVANCED
}

data class PermissionCardModel(
    val id: String,
    val title: String,
    val description: String,
    val required: Boolean,
    val state: PermissionCardState
)

data class SetupUiState(
    val currentStep: SetupStep = SetupStep.WELCOME,
    val pin: String = "",
    val pinConfirmation: String = "",
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val protectionMode: ProtectionMode = ProtectionMode.STANDARD,
    val permissions: List<PermissionCardModel> = emptyList(),
    val childName: String = "",
    val glassesRequired: Boolean = true,
    val selectedPackages: Set<String> = emptySet(),
    val firstProfileName: String = "Default Protection",
    val dailyLimitMinutes: Int = 120,
    val gracePeriodSeconds: Int = 30,
    val calibrationComplete: Boolean = false,
    val activationSecondsRemaining: Int = 30,
    val loading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SetupAction {
    data object Continue : SetupAction
    data object Back : SetupAction
    data class EnterPin(val value: String) : SetupAction
    data class ConfirmPin(val value: String) : SetupAction
    data class SetBiometric(val enabled: Boolean) : SetupAction
    data class SetProtectionMode(val mode: ProtectionMode) : SetupAction
    data class PermissionResult(
        val id: String,
        val state: PermissionCardState
    ) : SetupAction
    data class SetChildName(val value: String) : SetupAction
    data class SetGlassesRequired(val required: Boolean) : SetupAction
    data class TogglePackage(val packageName: String) : SetupAction
    data class SetProfileName(val value: String) : SetupAction
    data class SetDailyLimit(val minutes: Int) : SetupAction
    data class SetGracePeriod(val seconds: Int) : SetupAction
    data class SetCalibrationComplete(val complete: Boolean) : SetupAction
    data object CancelActivation : SetupAction
    data object TickActivation : SetupAction
}
