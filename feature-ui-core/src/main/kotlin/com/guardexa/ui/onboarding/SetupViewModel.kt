
package com.guardexa.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardexa.ui.state.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface SetupCompletionHandler {
    suspend fun persist(state: SetupUiState)
    suspend fun activateProtection()
}

class SetupViewModel(
    private val completionHandler: SetupCompletionHandler
) : ViewModel() {

    private val _state = MutableStateFlow(
        SetupUiState(
            permissions = defaultPermissions()
        )
    )
    val state: StateFlow<SetupUiState> = _state

    fun onAction(action: SetupAction) {
        when (action) {
            SetupAction.Continue -> continueToNextStep()
            SetupAction.Back -> goBack()
            is SetupAction.EnterPin ->
                _state.update { it.copy(pin = action.value.filter(Char::isDigit).take(12)) }
            is SetupAction.ConfirmPin ->
                _state.update { it.copy(pinConfirmation = action.value.filter(Char::isDigit).take(12)) }
            is SetupAction.SetBiometric ->
                _state.update { it.copy(biometricEnabled = action.enabled) }
            is SetupAction.SetProtectionMode ->
                _state.update { it.copy(protectionMode = action.mode) }
            is SetupAction.PermissionResult ->
                _state.update { current ->
                    current.copy(
                        permissions = current.permissions.map {
                            if (it.id == action.id) it.copy(state = action.state) else it
                        }
                    )
                }
            is SetupAction.SetChildName ->
                _state.update { it.copy(childName = action.value.take(50)) }
            is SetupAction.SetGlassesRequired ->
                _state.update { it.copy(glassesRequired = action.required) }
            is SetupAction.TogglePackage ->
                _state.update { current ->
                    val next = current.selectedPackages.toMutableSet().apply {
                        if (!add(action.packageName)) remove(action.packageName)
                    }
                    current.copy(selectedPackages = next)
                }
            is SetupAction.SetProfileName ->
                _state.update { it.copy(firstProfileName = action.value.take(60)) }
            is SetupAction.SetDailyLimit ->
                _state.update { it.copy(dailyLimitMinutes = action.minutes.coerceIn(0, 1440)) }
            is SetupAction.SetGracePeriod ->
                _state.update { it.copy(gracePeriodSeconds = action.seconds.coerceIn(0, 3600)) }
            is SetupAction.SetCalibrationComplete ->
                _state.update { it.copy(calibrationComplete = action.complete) }
            SetupAction.CancelActivation ->
                _state.update { it.copy(currentStep = SetupStep.REVIEW) }
            SetupAction.TickActivation -> tickActivation()
        }
    }

    private fun continueToNextStep() {
        val current = _state.value
        val validation = validateCurrentStep(current)
        if (validation != null) {
            _state.update { it.copy(errorMessage = validation) }
            return
        }

        _state.update {
            it.copy(
                currentStep = nextStep(it.currentStep),
                errorMessage = null
            )
        }

        if (_state.value.currentStep == SetupStep.ACTIVATION_COUNTDOWN) {
            viewModelScope.launch {
                runCatching { completionHandler.persist(_state.value) }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                currentStep = SetupStep.REVIEW,
                                errorMessage = error.message
                            )
                        }
                    }
            }
        }
    }

    private fun goBack() {
        _state.update {
            it.copy(
                currentStep = previousStep(it.currentStep),
                errorMessage = null
            )
        }
    }

    private fun tickActivation() {
        val current = _state.value
        if (current.currentStep != SetupStep.ACTIVATION_COUNTDOWN) return

        val remaining = (current.activationSecondsRemaining - 1).coerceAtLeast(0)
        _state.update { it.copy(activationSecondsRemaining = remaining) }

        if (remaining == 0) {
            viewModelScope.launch {
                _state.update { it.copy(loading = true) }
                runCatching { completionHandler.activateProtection() }
                    .onSuccess {
                        _state.update {
                            it.copy(
                                loading = false,
                                currentStep = SetupStep.COMPLETED
                            )
                        }
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                loading = false,
                                currentStep = SetupStep.REVIEW,
                                errorMessage = error.message
                            )
                        }
                    }
            }
        }
    }

    private fun validateCurrentStep(state: SetupUiState): String? =
        when (state.currentStep) {
            SetupStep.CREATE_PIN ->
                if (state.pin.length < 6) "pin_too_short" else null

            SetupStep.CONFIRM_PIN ->
                if (state.pin != state.pinConfirmation) "pin_mismatch" else null

            SetupStep.PERMISSIONS ->
                if (state.permissions.any { it.required && it.state != PermissionCardState.GRANTED }) {
                    "required_permissions_missing"
                } else null

            SetupStep.FIRST_PROFILE ->
                if (state.firstProfileName.isBlank()) "profile_name_required" else null

            SetupStep.CALIBRATION ->
                if (state.glassesRequired && !state.calibrationComplete) {
                    "calibration_required"
                } else null

            else -> null
        }

    private fun nextStep(step: SetupStep): SetupStep {
        val entries = SetupStep.entries
        return entries.getOrElse(step.ordinal + 1) { SetupStep.COMPLETED }
    }

    private fun previousStep(step: SetupStep): SetupStep {
        val entries = SetupStep.entries
        return entries.getOrElse((step.ordinal - 1).coerceAtLeast(0)) {
            SetupStep.WELCOME
        }
    }

    companion object {
        fun defaultPermissions() = listOf(
            PermissionCardModel(
                id = "camera",
                title = "Camera",
                description = "Required to verify glasses locally on this device.",
                required = true,
                state = PermissionCardState.NOT_REQUESTED
            ),
            PermissionCardModel(
                id = "usage_access",
                title = "Usage access",
                description = "Required to detect the currently used application.",
                required = true,
                state = PermissionCardState.NOT_REQUESTED
            ),
            PermissionCardModel(
                id = "notifications",
                title = "Notifications",
                description = "Used for important protection status messages.",
                required = false,
                state = PermissionCardState.NOT_REQUESTED
            )
        )
    }
}
