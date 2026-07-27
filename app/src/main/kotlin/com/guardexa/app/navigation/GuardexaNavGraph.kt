
package com.guardexa.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.guardexa.ui.dashboard.DashboardUiState
import com.guardexa.ui.dashboard.SmartDashboardScreen
import com.guardexa.ui.onboarding.SetupScreen
import com.guardexa.ui.state.SetupUiState
import com.guardexa.resilience.presentation.SafeModeScreen
import com.guardexa.resilience.model.SafeModeState
import com.guardexa.ui.protection.ProtectionScreen
import com.guardexa.ui.protection.ProtectionScreenState

object Routes {
    const val SETUP = "setup"
    const val DASHBOARD = "dashboard"
    const val SAFE_MODE = "safe_mode"
    const val PROTECTION = "protection"
}

data class AppUiBindings(
    val setupState: SetupUiState,
    val dashboardState: DashboardUiState,
    val safeModeState: SafeModeState,
    val protectionState: ProtectionScreenState,
    val onSetupAction: (com.guardexa.ui.state.SetupAction) -> Unit,
    val onPermissionRequest: (String) -> Unit,
    val onDashboardCardAction: (com.guardexa.ui.dashboard.DashboardCardType) -> Unit,
    val onSafeModeRecovery: () -> Unit,
    val onAdministratorEntry: () -> Unit,
    val onProtectionRecheck: () -> Unit,
    val onOpenAllowedApps: () -> Unit
)

@Composable
fun GuardexaNavGraph(
    navController: NavHostController,
    startDestination: String,
    bindings: AppUiBindings
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.SETUP) {
            SetupScreen(
                state = bindings.setupState,
                onAction = bindings.onSetupAction,
                onRequestPermission = bindings.onPermissionRequest
            )
        }

        composable(Routes.DASHBOARD) {
            SmartDashboardScreen(
                state = bindings.dashboardState,
                onCardAction = bindings.onDashboardCardAction
            )
        }

        composable(Routes.SAFE_MODE) {
            SafeModeScreen(
                state = bindings.safeModeState,
                recovering = false,
                onStartRecovery = bindings.onSafeModeRecovery,
                onAdministratorEntry = bindings.onAdministratorEntry
            )
        }

        composable(Routes.PROTECTION) {
            ProtectionScreen(
                state = bindings.protectionState,
                onRecheck = bindings.onProtectionRecheck,
                onOpenAllowedApps = bindings.onOpenAllowedApps,
                onAdministratorEntry = bindings.onAdministratorEntry
            )
        }
    }
}
