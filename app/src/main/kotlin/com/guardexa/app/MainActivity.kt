
package com.guardexa.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.guardexa.app.navigation.*
import com.guardexa.app.ui.MainViewModel
import com.guardexa.resilience.model.SafeModeState
import com.guardexa.ui.dashboard.DashboardUiState
import com.guardexa.ui.protection.*
import com.guardexa.ui.state.SetupUiState
import com.guardexa.core.ui.theme.GuardexaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            GuardexaTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val mainState by viewModel.uiState.collectAsState()

                if (mainState.loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    return@GuardexaTheme
                }

                val navController = rememberNavController()

                val bindings = remember {
                    AppUiBindings(
                        setupState = SetupUiState(),
                        dashboardState = DashboardUiState(
                            protectionActive = false,
                            privacyMode = true,
                            cards = emptyList()
                        ),
                        safeModeState = SafeModeState(
                            enabled = false,
                            reason = null,
                            enabledAtEpochMillis = null,
                            allowedPackages = emptySet(),
                            recoveryAttempts = 0,
                            lastRecoveryAtEpochMillis = null
                        ),
                        protectionState = ProtectionScreenState(
                            reason = ProtectionScreenReason.GLASSES_REQUIRED,
                            secondsRemaining = null,
                            rechecking = false,
                            allowedAppsAvailable = true,
                            administratorEntryAvailable = true
                        ),
                        onSetupAction = {},
                        onPermissionRequest = {},
                        onDashboardCardAction = {},
                        onSafeModeRecovery = {},
                        onAdministratorEntry = {},
                        onProtectionRecheck = {},
                        onOpenAllowedApps = {}
                    )
                }

                GuardexaNavGraph(
                    navController = navController,
                    startDestination = mainState.startRoute,
                    bindings = bindings
                )
            }
        }
    }
}
