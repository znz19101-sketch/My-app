
package com.guardexa.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.guardexa.ui.components.*
import com.guardexa.ui.state.*

@Composable
fun SetupScreen(
    state: SetupUiState,
    onAction: (SetupAction) -> Unit,
    onRequestPermission: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (
                state.currentStep !in setOf(
                    SetupStep.ACTIVATION_COUNTDOWN,
                    SetupStep.COMPLETED
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.currentStep != SetupStep.WELCOME) {
                        OutlinedButton(
                            onClick = { onAction(SetupAction.Back) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                    }

                    Button(
                        onClick = { onAction(SetupAction.Continue) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            when (state.currentStep) {
                SetupStep.WELCOME -> WelcomeStep()
                SetupStep.PRIVACY -> PrivacyStep()
                SetupStep.CREATE_PIN -> PinStep(
                    title = "Create security PIN",
                    value = state.pin,
                    onValueChange = { onAction(SetupAction.EnterPin(it)) }
                )
                SetupStep.CONFIRM_PIN -> PinStep(
                    title = "Confirm security PIN",
                    value = state.pinConfirmation,
                    onValueChange = { onAction(SetupAction.ConfirmPin(it)) }
                )
                SetupStep.BIOMETRIC -> BiometricStep(
                    available = state.biometricAvailable,
                    enabled = state.biometricEnabled,
                    onEnabled = { onAction(SetupAction.SetBiometric(it)) }
                )
                SetupStep.PROTECTION_MODE -> ProtectionModeStep(
                    selected = state.protectionMode,
                    onSelected = { onAction(SetupAction.SetProtectionMode(it)) }
                )
                SetupStep.PERMISSIONS -> PermissionsStep(
                    permissions = state.permissions,
                    onGrant = onRequestPermission
                )
                SetupStep.CHILD_PROFILE -> ChildProfileStep(
                    name = state.childName,
                    onNameChange = { onAction(SetupAction.SetChildName(it)) }
                )
                SetupStep.GLASSES_POLICY -> GlassesPolicyStep(
                    required = state.glassesRequired,
                    onRequiredChange = {
                        onAction(SetupAction.SetGlassesRequired(it))
                    }
                )
                SetupStep.APPLICATIONS -> PlaceholderStep(
                    title = "Choose applications",
                    body = "Select applications that may be used under the first protection profile."
                )
                SetupStep.FIRST_PROFILE -> ProfileStep(
                    name = state.firstProfileName,
                    onNameChange = { onAction(SetupAction.SetProfileName(it)) }
                )
                SetupStep.DAILY_LIMITS -> DailyLimitStep(
                    minutes = state.dailyLimitMinutes,
                    onChange = { onAction(SetupAction.SetDailyLimit(it)) }
                )
                SetupStep.SCHEDULES -> PlaceholderStep(
                    title = "Schedules",
                    body = "Add allowed time windows. Overnight schedules are supported."
                )
                SetupStep.CALIBRATION -> CalibrationStep(
                    complete = state.calibrationComplete,
                    onComplete = {
                        onAction(SetupAction.SetCalibrationComplete(true))
                    }
                )
                SetupStep.REVIEW -> ReviewStep(state)
                SetupStep.ACTIVATION_COUNTDOWN -> ActivationCountdownStep(
                    seconds = state.activationSecondsRemaining,
                    loading = state.loading,
                    onCancel = { onAction(SetupAction.CancelActivation) }
                )
                SetupStep.COMPLETED -> ProtectionStartedStep()
            }

            state.errorMessage?.let {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    Text(it)
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Guardexa", style = MaterialTheme.typography.displaySmall)
        Text(
            "Protecting What Matters Most",
            style = MaterialTheme.typography.titleLarge
        )
        Text("We will now configure local parental protection on this device.")
    }
}

@Composable
private fun PrivacyStep() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Your privacy", style = MaterialTheme.typography.headlineMedium)
        Text("• Photos do not leave this device.")
        Text("• Face data is not uploaded.")
        Text("• Activity reports remain local.")
        Text("• No hidden background communication is used.")
        Text("• Diagnostic sharing is always manual and visible.")
    }
}

@Composable
private fun PinStep(
    title: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            label = { Text("6–12 digits") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BiometricStep(
    available: Boolean,
    enabled: Boolean,
    onEnabled: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Biometric unlock", style = MaterialTheme.typography.headlineMedium)
        if (!available) {
            Text("Biometric authentication is not available on this device.")
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Use fingerprint for administrator access")
                Switch(checked = enabled, onCheckedChange = onEnabled)
            }
        }
    }
}

@Composable
private fun ProtectionModeStep(
    selected: ProtectionMode,
    onSelected: (ProtectionMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Protection mode", style = MaterialTheme.typography.headlineMedium)

        ProtectionMode.entries.forEach { mode ->
            Card(
                onClick = { onSelected(mode) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected == mode,
                        onClick = { onSelected(mode) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(mode.name)
                        Text(
                            if (mode == ProtectionMode.STANDARD) {
                                "Recommended for normal installation."
                            } else {
                                "Adds Device Owner and kiosk capabilities when configured."
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsStep(
    permissions: List<PermissionCardModel>,
    onGrant: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Required permissions",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        items(permissions, key = { it.id }) { permission ->
            GuardexaPermissionCard(
                title = permission.title,
                description = permission.description,
                granted = permission.state == PermissionCardState.GRANTED,
                required = permission.required,
                onGrant = { onGrant(permission.id) }
            )
        }
    }
}

@Composable
private fun ChildProfileStep(
    name: String,
    onNameChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Child profile", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Optional display name") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GlassesPolicyStep(
    required: Boolean,
    onRequiredChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Glasses requirement", style = MaterialTheme.typography.headlineMedium)
        Text("Guardexa can also operate as parental control with glasses checks disabled.")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Require glasses")
            Switch(checked = required, onCheckedChange = onRequiredChange)
        }
    }
}

@Composable
private fun ProfileStep(
    name: String,
    onNameChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("First protection profile", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Profile name") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DailyLimitStep(
    minutes: Int,
    onChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Daily limit", style = MaterialTheme.typography.headlineMedium)
        Text("$minutes minutes")
        Slider(
            value = minutes.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..480f,
            steps = 31
        )
    }
}

@Composable
private fun CalibrationStep(
    complete: Boolean,
    onComplete: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Smart calibration", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Look forward, turn left and right, blink naturally, then move slightly closer and farther."
        )
        Text(
            if (complete) "Calibration completed."
            else "Calibration has not been completed."
        )
        GuardexaPrimaryButton(
            text = if (complete) "Repeat calibration" else "Start calibration",
            onClick = onComplete
        )
    }
}

@Composable
private fun ReviewStep(state: SetupUiState) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Review", style = MaterialTheme.typography.headlineMedium)
        }
        item { GuardexaStatusCard("Protection mode", state.protectionMode.name) }
        item { GuardexaStatusCard("Profile", state.firstProfileName) }
        item { GuardexaStatusCard("Daily limit", "${state.dailyLimitMinutes} minutes") }
        item {
            GuardexaStatusCard(
                "Glasses verification",
                if (state.glassesRequired) "Enabled" else "Disabled"
            )
        }
        item {
            GuardexaStatusCard(
                "Permissions",
                "${state.permissions.count { it.state == PermissionCardState.GRANTED }}/${state.permissions.size}"
            )
        }
    }
}

@Composable
private fun ActivationCountdownStep(
    seconds: Int,
    loading: Boolean,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Protection starts in", style = MaterialTheme.typography.titleLarge)
        Text("$seconds", style = MaterialTheme.typography.displayLarge)
        if (loading) {
            CircularProgressIndicator()
        } else {
            Spacer(Modifier.height(20.dp))
            GuardexaSecondaryButton(
                text = "Cancel",
                onClick = onCancel
            )
        }
    }
}

@Composable
private fun ProtectionStartedStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🛡", style = MaterialTheme.typography.displayLarge)
        Text(
            "Your protection has started",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun PlaceholderStep(
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(body)
    }
}
