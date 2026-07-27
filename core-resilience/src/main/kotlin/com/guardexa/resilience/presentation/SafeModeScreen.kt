
package com.guardexa.resilience.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guardexa.resilience.model.SafeModeState

@Composable
fun SafeModeScreen(
    state: SafeModeState,
    recovering: Boolean,
    onStartRecovery: () -> Unit,
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
        Text("⚠", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "Guardexa is in safe mode",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Protection remains active with a reduced set of allowed applications."
        )
        Spacer(Modifier.height(8.dp))
        Text("Reason: ${state.reason ?: "Unknown"}")

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onStartRecovery,
            enabled = !recovering,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (recovering) "Recovering…" else "Try recovery")
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onAdministratorEntry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Administrator")
        }
    }
}
