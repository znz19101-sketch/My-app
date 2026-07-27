
package com.guardexa.feature.apps.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardexa.feature.apps.domain.model.*
import com.guardexa.feature.apps.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppsFilterState(
    val query: String = "",
    val onlyUnreviewed: Boolean = false,
    val onlyRequiresGlasses: Boolean = false,
    val onlyAlwaysAllowed: Boolean = false
)

data class AppsUiState(
    val loading: Boolean = true,
    val apps: List<AppWithPolicy> = emptyList(),
    val filter: AppsFilterState = AppsFilterState(),
    val selected: AppWithPolicy? = null,
    val errorMessage: String? = null
)

sealed interface AppsAction {
    data class Search(val value: String) : AppsAction
    data object ToggleUnreviewed : AppsAction
    data object ToggleRequiresGlasses : AppsAction
    data object ToggleAlwaysAllowed : AppsAction
    data class Select(val app: AppWithPolicy?) : AppsAction
    data class SavePolicy(val policy: AppPolicy) : AppsAction
    data class MarkReviewed(val packageName: String) : AppsAction
    data class ChangeCategory(
        val packageName: String,
        val category: AppCategory
    ) : AppsAction
}

class AppsViewModel(
    private val profileId: String,
    observeApps: ObserveAppsUseCase,
    private val filterApps: FilterAppsUseCase,
    private val savePolicy: SaveAppPolicyUseCase,
    private val reviewApp: ReviewAppUseCase,
    private val changeCategory: ChangeAppCategoryUseCase
) : ViewModel() {

    private val filterState = MutableStateFlow(AppsFilterState())
    private val selected = MutableStateFlow<AppWithPolicy?>(null)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AppsUiState> =
        combine(
            observeApps(profileId),
            filterState,
            selected,
            error
        ) { apps, filter, selectedApp, errorMessage ->
            AppsUiState(
                loading = false,
                apps = filterApps(
                    apps = apps,
                    query = filter.query,
                    onlyUnreviewed = filter.onlyUnreviewed,
                    onlyRequiresGlasses = filter.onlyRequiresGlasses,
                    onlyAlwaysAllowed = filter.onlyAlwaysAllowed
                ),
                filter = filter,
                selected = selectedApp,
                errorMessage = errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppsUiState()
        )

    fun onAction(action: AppsAction) {
        when (action) {
            is AppsAction.Search ->
                filterState.update { it.copy(query = action.value) }

            AppsAction.ToggleUnreviewed ->
                filterState.update { it.copy(onlyUnreviewed = !it.onlyUnreviewed) }

            AppsAction.ToggleRequiresGlasses ->
                filterState.update { it.copy(onlyRequiresGlasses = !it.onlyRequiresGlasses) }

            AppsAction.ToggleAlwaysAllowed ->
                filterState.update { it.copy(onlyAlwaysAllowed = !it.onlyAlwaysAllowed) }

            is AppsAction.Select ->
                selected.value = action.app

            is AppsAction.SavePolicy -> viewModelScope.launch {
                runCatching { savePolicy(action.policy) }
                    .onSuccess { selected.value = null }
                    .onFailure { error.value = it.message }
            }

            is AppsAction.MarkReviewed -> viewModelScope.launch {
                runCatching { reviewApp(action.packageName) }
                    .onFailure { error.value = it.message }
            }

            is AppsAction.ChangeCategory -> viewModelScope.launch {
                runCatching {
                    changeCategory(action.packageName, profileId, action.category)
                }.onFailure { error.value = it.message }
            }
        }
    }
}
