
package com.guardexa.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guardexa.ui.components.GuardexaStatusCard

enum class DashboardCardType {
    CRITICAL_SECURITY,
    LOST_PERMISSION,
    NEW_APPLICATIONS,
    TIME_EXHAUSTED,
    CAMERA_PROBLEM,
    GLASSES_STATUS,
    CURRENT_APPLICATION,
    TIME_REMAINING,
    ACTIVE_PROFILE,
    LAST_EVENT
}

data class DashboardCard(
    val type: DashboardCardType,
    val title: String,
    val value: String,
    val supportingText: String? = null,
    val priority: Int,
    val actionLabel: String? = null
)

data class DashboardUiState(
    val protectionActive: Boolean,
    val privacyMode: Boolean,
    val cards: List<DashboardCard>
)

class SmartDashboardOrderer {
    fun order(cards: List<DashboardCard>): List<DashboardCard> =
        cards.sortedWith(
            compareByDescending<DashboardCard> { it.priority }
                .thenBy { it.type.ordinal }
        )
}

@Composable
fun SmartDashboardScreen(
    state: DashboardUiState,
    onCardAction: (DashboardCardType) -> Unit,
    modifier: Modifier = Modifier,
    orderer: SmartDashboardOrderer = SmartDashboardOrderer()
) {
    val ordered = orderer.order(state.cards)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (state.protectionActive) {
                    "Protection active"
                } else {
                    "Protection inactive"
                },
                style = MaterialTheme.typography.headlineMedium
            )
        }

        items(ordered, key = { it.type }) { card ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    GuardexaStatusCard(
                        title = card.title,
                        value = if (state.privacyMode) {
                            when (card.type) {
                                DashboardCardType.CURRENT_APPLICATION,
                                DashboardCardType.LAST_EVENT,
                                DashboardCardType.CRITICAL_SECURITY ->
                                    "Protected information"
                                else -> card.value
                            }
                        } else {
                            card.value
                        },
                        supportingText = if (state.privacyMode) null else card.supportingText
                    )

                    card.actionLabel?.let { label ->
                        TextButton(
                            onClick = { onCardAction(card.type) }
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}
