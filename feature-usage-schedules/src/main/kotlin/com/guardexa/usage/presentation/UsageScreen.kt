
package com.guardexa.usage.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guardexa.usage.model.BonusTargetType

@Composable
fun UsageScreen(
    state: UsageUiState,
    onAction: (UsageAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Usage & Bonus Time",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onAction(
                        UsageAction.GrantDeviceTime(
                            durationMillis = 15 * 60_000L,
                            reason = null
                        )
                    )
                }
            ) {
                Text("+15 min device")
            }

            Button(
                onClick = {
                    onAction(
                        UsageAction.GrantDeviceTime(
                            durationMillis = 30 * 60_000L,
                            reason = null
                        )
                    )
                }
            ) {
                Text("+30 min device")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (state.loading) {
            CircularProgressIndicator()
            return@Column
        }

        Text(
            text = "Active bonus grants",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = state.bonusGrants,
                key = { it.id }
            ) { grant ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            if (grant.targetType == BonusTargetType.DEVICE) {
                                "Device bonus"
                            } else {
                                "App bonus: ${grant.targetPackageName}"
                            }
                        )
                        Text("${grant.durationMillis / 60_000L} minutes")
                        grant.reason?.let { Text(it) }

                        TextButton(
                            onClick = {
                                onAction(
                                    UsageAction.CancelGrant(grant.id)
                                )
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}
