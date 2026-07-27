
package com.guardexa.ui.navigation

sealed interface GuardexaDestination {
    data object Splash : GuardexaDestination
    data object Setup : GuardexaDestination
    data object Dashboard : GuardexaDestination
    data object Applications : GuardexaDestination
    data object Profiles : GuardexaDestination
    data object Usage : GuardexaDestination
    data object Schedules : GuardexaDestination
    data object Logs : GuardexaDestination
    data object Security : GuardexaDestination
    data object Settings : GuardexaDestination
    data object Protection : GuardexaDestination
    data object SafeMode : GuardexaDestination
}

data class NavigationDecision(
    val destination: GuardexaDestination,
    val clearBackStack: Boolean
)

enum class ApplicationLaunchState {
    NOT_CONFIGURED,
    SETUP_IN_PROGRESS,
    READY_NOT_ENABLED,
    ACTIVATION_COUNTDOWN,
    PROTECTION_ENABLED,
    SAFE_MODE
}

class StartupNavigationResolver {
    fun resolve(state: ApplicationLaunchState): NavigationDecision =
        when (state) {
            ApplicationLaunchState.NOT_CONFIGURED,
            ApplicationLaunchState.SETUP_IN_PROGRESS ->
                NavigationDecision(
                    destination = GuardexaDestination.Setup,
                    clearBackStack = true
                )

            ApplicationLaunchState.READY_NOT_ENABLED ->
                NavigationDecision(
                    destination = GuardexaDestination.Dashboard,
                    clearBackStack = true
                )

            ApplicationLaunchState.ACTIVATION_COUNTDOWN ->
                NavigationDecision(
                    destination = GuardexaDestination.Setup,
                    clearBackStack = true
                )

            ApplicationLaunchState.PROTECTION_ENABLED ->
                NavigationDecision(
                    destination = GuardexaDestination.Dashboard,
                    clearBackStack = true
                )

            ApplicationLaunchState.SAFE_MODE ->
                NavigationDecision(
                    destination = GuardexaDestination.SafeMode,
                    clearBackStack = true
                )
        }
}
