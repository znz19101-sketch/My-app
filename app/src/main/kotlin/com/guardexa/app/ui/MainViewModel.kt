
package com.guardexa.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.guardexa.app.navigation.Routes
import com.guardexa.app.runtime.ApplicationStateRepository
import com.guardexa.app.runtime.GuardexaLaunchState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val loading: Boolean = true,
    val startRoute: String = Routes.SETUP,
    val errorMessage: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val applicationStateRepository: ApplicationStateRepository
) : ViewModel() {

    val uiState: StateFlow<MainUiState> =
        applicationStateRepository.observe()
            .map { runtime ->
                MainUiState(
                    loading = false,
                    startRoute = when (runtime.launchState) {
                        GuardexaLaunchState.FIRST_RUN,
                        GuardexaLaunchState.SETUP_IN_PROGRESS ->
                            Routes.SETUP

                        GuardexaLaunchState.READY,
                        GuardexaLaunchState.PROTECTION_ACTIVE ->
                            Routes.DASHBOARD

                        GuardexaLaunchState.SAFE_MODE ->
                            Routes.SAFE_MODE
                    }
                )
            }
            .catch { error ->
                emit(
                    MainUiState(
                        loading = false,
                        startRoute = Routes.SAFE_MODE,
                        errorMessage = error.message
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MainUiState()
            )

    fun enableProtection() {
        viewModelScope.launch {
            applicationStateRepository.setProtectionEnabled(true)
        }
    }

    fun disableProtection() {
        viewModelScope.launch {
            applicationStateRepository.setProtectionEnabled(false)
        }
    }
}
