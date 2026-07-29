package com.guardexa.app

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.guardexa.app.navigation.AppUiBindings
import com.guardexa.app.navigation.GuardexaNavGraph
import com.guardexa.app.runtime.ApplicationStateRepository
import com.guardexa.app.ui.MainViewModel
import com.guardexa.core.ui.theme.GuardexaTheme
import com.guardexa.device.service.ProtectionForegroundService
import com.guardexa.resilience.model.SafeModeState
import com.guardexa.ui.dashboard.DashboardCard
import com.guardexa.ui.dashboard.DashboardCardType
import com.guardexa.ui.dashboard.DashboardUiState
import com.guardexa.ui.onboarding.SetupCompletionHandler
import com.guardexa.ui.onboarding.SetupViewModel
import com.guardexa.ui.protection.ProtectionScreenReason
import com.guardexa.ui.protection.ProtectionScreenState
import com.guardexa.ui.state.PermissionCardState
import com.guardexa.ui.state.SetupAction
import com.guardexa.ui.state.SetupStep
import com.guardexa.ui.state.SetupUiState
import dagger.hilt.android.AndroidEntryPoint
import com.guardexa.feature.apps.data.local.AppsLocalDataSource
import com.guardexa.feature.apps.domain.model.AppAccessPolicy
import com.guardexa.feature.apps.domain.model.AppPolicy
import com.guardexa.feature.apps.domain.repository.AppsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var applicationStateRepository: ApplicationStateRepository

    @Inject
    lateinit var appsLocalDataSource: AppsLocalDataSource

    @Inject
    lateinit var appsRepository: AppsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            GuardexaTheme {
                val mainViewModel: MainViewModel = hiltViewModel()
                val mainState by mainViewModel.uiState.collectAsState()

                if (mainState.loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    return@GuardexaTheme
                }

                val completionHandler = remember {
                    object : SetupCompletionHandler {
                        override suspend fun persist(state: SetupUiState) {
                            applicationStateRepository.markSetupInProgress()

                            appsLocalDataSource.synchronizeInstalledApps()

                            val profileId = "default-profile"
                            val installedApps = appsRepository
                                .observeApps(profileId)
                                .first()

                            val selectedPackages =
                                if (state.selectedPackages.isNotEmpty()) {
                                    state.selectedPackages
                                } else {
                                    installedApps
                                        .asSequence()
                                        .map { it.app }
                                        .filterNot { it.isSystemApp }
                                        .filterNot {
                                            it.packageName ==
                                                this@MainActivity.packageName
                                        }
                                        .map { it.packageName }
                                        .toSet()
                                }

                            installedApps.forEach { item ->
                                val app = item.app

                                if (
                                    app.packageName ==
                                    this@MainActivity.packageName
                                ) {
                                    return@forEach
                                }

                                val selected =
                                    app.packageName in selectedPackages

                                val requiresGlasses =
                                    selected && state.glassesRequired

                                appsRepository.savePolicy(
                                    AppPolicy(
                                        packageName = app.packageName,
                                        profileId = profileId,
                                        accessPolicy =
                                            if (requiresGlasses) {
                                                AppAccessPolicy.GLASSES_REQUIRED
                                            } else {
                                                AppAccessPolicy.ALWAYS_ALLOWED
                                            },
                                        dailyLimitMinutes =
                                            state.dailyLimitMinutes
                                                .takeIf { it > 0 },
                                        requiresGlasses = requiresGlasses,
                                        alwaysAllowed = !requiresGlasses,
                                        emergencyApp = false,
                                        allowCurrentActivityToFinish = false,
                                        gracePeriodSeconds =
                                            state.gracePeriodSeconds
                                    )
                                )
                            }
                        }

                        override suspend fun activateProtection() {
                            applicationStateRepository.markSetupComplete(
                                activeProfileId = "default-profile"
                            )
                            applicationStateRepository.setProtectionEnabled(true)

                            ProtectionForegroundService.start(
                                this@MainActivity.applicationContext
                            )
                        }
                    }
                }

                val setupFactory = remember(completionHandler) {
                    SetupViewModelFactory(completionHandler)
                }

                val setupViewModel: SetupViewModel = viewModel(
                    factory = setupFactory
                )

                val setupState by setupViewModel.state.collectAsState()

                val cameraPermissionLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        setupViewModel.onAction(
                            SetupAction.PermissionResult(
                                id = "camera",
                                state = if (granted) {
                                    PermissionCardState.GRANTED
                                } else {
                                    PermissionCardState.DENIED
                                }
                            )
                        )
                    }

                val notificationPermissionLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        setupViewModel.onAction(
                            SetupAction.PermissionResult(
                                id = "notifications",
                                state = if (granted) {
                                    PermissionCardState.GRANTED
                                } else {
                                    PermissionCardState.DENIED
                                }
                            )
                        )
                    }

                val usageAccessLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) {
                        val granted =
                            this@MainActivity.hasUsageAccessPermission()

                        setupViewModel.onAction(
                            SetupAction.PermissionResult(
                                id = "usage_access",
                                state = if (granted) {
                                    PermissionCardState.GRANTED
                                } else {
                                    PermissionCardState.DENIED
                                }
                            )
                        )
                    }

                LaunchedEffect(Unit) {
                    val cameraGranted =
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.CAMERA
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    setupViewModel.onAction(
                        SetupAction.PermissionResult(
                            id = "camera",
                            state = if (cameraGranted) {
                                PermissionCardState.GRANTED
                            } else {
                                PermissionCardState.NOT_REQUESTED
                            }
                        )
                    )

                    val usageGranted =
                        this@MainActivity.hasUsageAccessPermission()

                    setupViewModel.onAction(
                        SetupAction.PermissionResult(
                            id = "usage_access",
                            state = if (usageGranted) {
                                PermissionCardState.GRANTED
                            } else {
                                PermissionCardState.NOT_REQUESTED
                            }
                        )
                    )

                    val notificationsGranted =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }

                    setupViewModel.onAction(
                        SetupAction.PermissionResult(
                            id = "notifications",
                            state = if (notificationsGranted) {
                                PermissionCardState.GRANTED
                            } else {
                                PermissionCardState.NOT_REQUESTED
                            }
                        )
                    )
                }

                DisposableEffect(setupState.currentStep) {
                    val protectScreen =
                        setupState.currentStep in setOf(
                            SetupStep.CREATE_PIN,
                            SetupStep.CONFIRM_PIN
                        )

                    if (protectScreen) {
                        window.addFlags(
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        window.clearFlags(
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    }

                    onDispose {
                        window.clearFlags(
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    }
                }

                val navController = rememberNavController()

                val bindings = AppUiBindings(
                    setupState = setupState,
                    dashboardState = DashboardUiState(
                        protectionActive = mainState.protectionActive,
                        privacyMode = false,
                        cards = listOf(
                            DashboardCard(
                                type = DashboardCardType.CRITICAL_SECURITY,
                                title = "Protection service",
                                value = if (mainState.protectionActive) {
                                    "Running"
                                } else {
                                    "Stopped"
                                },
                                supportingText = if (mainState.protectionActive) {
                                    "Foreground application monitoring is active."
                                } else {
                                    "Start protection to monitor application usage."
                                },
                                priority = 100,
                                actionLabel = if (mainState.protectionActive) {
                                    "Stop protection"
                                } else {
                                    "Start protection"
                                }
                            ),
                            DashboardCard(
                                type = DashboardCardType.ACTIVE_PROFILE,
                                title = "Active profile",
                                value = "Default Protection",
                                supportingText = "The first protection profile.",
                                priority = 80
                            ),
                            DashboardCard(
                                type = DashboardCardType.GLASSES_STATUS,
                                title = "Glasses verification",
                                value = "Integration pending",
                                supportingText =
                                    "The AI verification pipeline will be connected next.",
                                priority = 70
                            )
                        )
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
                    onSetupAction = setupViewModel::onAction,
                    onPermissionRequest = { permissionId ->
                        when (permissionId) {
                            "camera" -> {
                                cameraPermissionLauncher.launch(
                                    Manifest.permission.CAMERA
                                )
                            }

                            "notifications" -> {
                                if (
                                    Build.VERSION.SDK_INT >=
                                    Build.VERSION_CODES.TIRAMISU
                                ) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    setupViewModel.onAction(
                                        SetupAction.PermissionResult(
                                            id = "notifications",
                                            state = PermissionCardState.GRANTED
                                        )
                                    )
                                }
                            }

                            "usage_access" -> {
                                val intent = Intent(
                                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                                ).apply {
                                    data = Uri.parse(
                                        "package:${this@MainActivity.packageName}"
                                    )
                                }

                                usageAccessLauncher.launch(intent)
                            }
                        }
                    },
                    onDashboardCardAction = { cardType ->
                        if (cardType == DashboardCardType.CRITICAL_SECURITY) {
                            if (mainState.protectionActive) {
                                mainViewModel.disableProtection()
                                ProtectionForegroundService.stop(
                                    this@MainActivity.applicationContext
                                )
                            } else {
                                mainViewModel.enableProtection()
                                ProtectionForegroundService.start(
                                    this@MainActivity.applicationContext
                                )
                            }
                        }
                    },
                    onSafeModeRecovery = {},
                    onAdministratorEntry = {},
                    onProtectionRecheck = {},
                    onOpenAllowedApps = {}
                )

                GuardexaNavGraph(
                    navController = navController,
                    startDestination = mainState.startRoute,
                    bindings = bindings
                )
            }
        }
    }
}

private fun Context.hasUsageAccessPermission(): Boolean {
    val appOpsManager =
        getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    val mode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }

    return mode == AppOpsManager.MODE_ALLOWED
}

private class SetupViewModelFactory(
    private val completionHandler: SetupCompletionHandler
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(SetupViewModel::class.java)) {
            return SetupViewModel(completionHandler) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
