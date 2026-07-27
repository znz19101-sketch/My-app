
package com.guardexa.feature.apps.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guardexa.feature.apps.domain.model.*

@Composable
fun AppsScreen(
    state: AppsUiState,
    onAction: (AppsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Applications",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.filter.query,
            onValueChange = { onAction(AppsAction.Search(it)) },
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.filter.onlyUnreviewed,
                onClick = { onAction(AppsAction.ToggleUnreviewed) },
                label = { Text("New") }
            )
            FilterChip(
                selected = state.filter.onlyRequiresGlasses,
                onClick = { onAction(AppsAction.ToggleRequiresGlasses) },
                label = { Text("Glasses") }
            )
            FilterChip(
                selected = state.filter.onlyAlwaysAllowed,
                onClick = { onAction(AppsAction.ToggleAlwaysAllowed) },
                label = { Text("Always allowed") }
            )
        }

        Spacer(Modifier.height(12.dp))

        when {
            state.loading -> CircularProgressIndicator()
            state.apps.isEmpty() -> Text("No applications found.")
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = state.apps,
                    key = { it.app.packageName }
                ) { item ->
                    AppRow(
                        item = item,
                        onClick = { onAction(AppsAction.Select(item)) }
                    )
                }
            }
        }
    }

    state.selected?.let { selected ->
        AppPolicyDialog(
            item = selected,
            onDismiss = { onAction(AppsAction.Select(null)) },
            onSave = { onAction(AppsAction.SavePolicy(it)) }
        )
    }
}

@Composable
private fun AppRow(
    item: AppWithPolicy,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.app.originalName, style = MaterialTheme.typography.titleMedium)
            Text(item.app.packageName, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text("Category: ${item.effectiveCategory.name}")
            Text("Policy: ${item.policy?.accessPolicy?.name ?: "GLOBAL"}")
            if (!item.app.reviewed) {
                AssistChip(onClick = {}, label = { Text("New app") })
            }
        }
    }
}

@Composable
private fun AppPolicyDialog(
    item: AppWithPolicy,
    onDismiss: () -> Unit,
    onSave: (AppPolicy) -> Unit
) {
    var accessPolicy by remember {
        mutableStateOf(item.policy?.accessPolicy ?: AppAccessPolicy.USE_GLOBAL_POLICY)
    }
    var requiresGlasses by remember {
        mutableStateOf(item.policy?.requiresGlasses ?: false)
    }
    var alwaysAllowed by remember {
        mutableStateOf(item.policy?.alwaysAllowed ?: false)
    }
    var emergency by remember {
        mutableStateOf(item.policy?.emergencyApp ?: false)
    }
    var finishCurrentActivity by remember {
        mutableStateOf(item.policy?.allowCurrentActivityToFinish ?: false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.app.originalName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Access policy")
                AppAccessPolicy.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { accessPolicy = option },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(option.name)
                        RadioButton(
                            selected = accessPolicy == option,
                            onClick = { accessPolicy = option }
                        )
                    }
                }
                PolicySwitch("Requires glasses", requiresGlasses) {
                    requiresGlasses = it
                }
                PolicySwitch("Always allowed", alwaysAllowed) {
                    alwaysAllowed = it
                }
                PolicySwitch("Emergency app", emergency) {
                    emergency = it
                }
                PolicySwitch("Allow current activity to finish", finishCurrentActivity) {
                    finishCurrentActivity = it
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val base = item.policy
                    onSave(
                        AppPolicy(
                            packageName = item.app.packageName,
                            profileId = base?.profileId ?: "default",
                            accessPolicy = accessPolicy,
                            dailyLimitMinutes = base?.dailyLimitMinutes,
                            requiresGlasses = requiresGlasses,
                            alwaysAllowed = alwaysAllowed,
                            emergencyApp = emergency,
                            allowCurrentActivityToFinish = finishCurrentActivity,
                            gracePeriodSeconds = base?.gracePeriodSeconds ?: 30,
                            categoryOverride = base?.categoryOverride
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PolicySwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
