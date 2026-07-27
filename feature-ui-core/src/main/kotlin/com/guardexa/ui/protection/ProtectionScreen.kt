
package com.guardexa.ui.protection

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guardexa.ui.components.GuardexaPrimaryButton
import com.guardexa.ui.components.GuardexaSecondaryButton

enum class ProtectionScreenReason {
    GLASSES_REQUIRED,
    FACE_NOT_VISIBLE,
    LOW_LIGHT,
    CAMERA_OBSTRUCTED,
    DAILY_LIMIT_REACHED,
    OUTSIDE_SCHEDULE,
    APPLICATION_BLOCKED,
    SAFE_MODE
}

data class ProtectionScreenState(
    val reason: ProtectionScreenReason,
    val secondsRemaining: Int?,
    val rechecking: Boolean,
    val allowedAppsAvailable: Boolean,
    val administratorEntryAvailable: Boolean
)

@Composable
fun ProtectionScreen(
    state: ProtectionScreenState,
    onRecheck: () -> Unit,
    onOpenAllowedApps: () -> Unit,
    onAdministratorEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🛡", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            text = titleFor(state.reason),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = bodyFor(state.reason),
            style = MaterialTheme.typography.bodyLarge
        )

        state.secondsRemaining?.let {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "$it",
                style = MaterialTheme.typography.displayMedium
            )
        }

        Spacer(Modifier.height(24.dp))

        GuardexaPrimaryButton(
            text = if (state.rechecking) "Checking…" else "Recheck",
            enabled = !state.rechecking,
            onClick = onRecheck
        )

        if (state.allowedAppsAvailable) {
            Spacer(Modifier.height(10.dp))
            GuardexaSecondaryButton(
                text = "Allowed applications",
                onClick = onOpenAllowedApps
            )
        }

        if (state.administratorEntryAvailable) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onAdministratorEntry) {
                Text("Administrator")
            }
        }
    }
}

private fun titleFor(reason: ProtectionScreenReason): String =
    when (reason) {
        ProtectionScreenReason.GLASSES_REQUIRED -> "Please wear your glasses"
        ProtectionScreenReason.FACE_NOT_VISIBLE -> "Look at the screen"
        ProtectionScreenReason.LOW_LIGHT -> "More light is needed"
        ProtectionScreenReason.CAMERA_OBSTRUCTED -> "Clear the camera"
        ProtectionScreenReason.DAILY_LIMIT_REACHED -> "Daily time has ended"
        ProtectionScreenReason.OUTSIDE_SCHEDULE -> "Not available at this time"
        ProtectionScreenReason.APPLICATION_BLOCKED -> "Application unavailable"
        ProtectionScreenReason.SAFE_MODE -> "Protection is in safe mode"
    }

private fun bodyFor(reason: ProtectionScreenReason): String =
    when (reason) {
        ProtectionScreenReason.GLASSES_REQUIRED ->
            "Use will continue automatically after your glasses are verified."
        ProtectionScreenReason.FACE_NOT_VISIBLE ->
            "Return your face to the camera view to continue."
        ProtectionScreenReason.LOW_LIGHT ->
            "Move to a brighter place so Guardexa can verify safely."
        ProtectionScreenReason.CAMERA_OBSTRUCTED ->
            "Remove anything covering the front camera."
        ProtectionScreenReason.DAILY_LIMIT_REACHED ->
            "The configured daily usage limit has been reached."
        ProtectionScreenReason.OUTSIDE_SCHEDULE ->
            "This application is outside its allowed schedule."
        ProtectionScreenReason.APPLICATION_BLOCKED ->
            "This application is blocked by the active protection profile."
        ProtectionScreenReason.SAFE_MODE ->
            "Only emergency and explicitly allowed applications are available."
    }
