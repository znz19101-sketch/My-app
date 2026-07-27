
package com.guardexa.usage.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardexa.usage.engine.UsageAccountingEngine
import com.guardexa.usage.model.*
import com.guardexa.usage.repository.UsageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class UsageUiState(
    val loading: Boolean = true,
    val appUsage: List<DailyUsage> = emptyList(),
    val bonusGrants: List<BonusTimeGrant> = emptyList(),
    val error: String? = null
)

sealed interface UsageAction {
    data class GrantDeviceTime(
        val durationMillis: Long,
        val reason: String?
    ) : UsageAction

    data class GrantAppTime(
        val packageName: String,
        val durationMillis: Long,
        val reason: String?
    ) : UsageAction

    data class CancelGrant(val id: String) : UsageAction
}

class UsageViewModel(
    usageDateKey: String,
    profileId: String,
    private val repository: UsageRepository,
    private val epochTime: () -> Long
) : ViewModel() {

    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UsageUiState> =
        combine(
            repository.observeDailyUsage(
                usageDateKey = usageDateKey,
                profileId = profileId
            ),
            repository.observeBonusGrants(),
            error
        ) { usage, grants, errorMessage ->
            UsageUiState(
                loading = false,
                appUsage = usage,
                bonusGrants = grants.filter { it.active },
                error = errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UsageUiState()
        )

    fun onAction(action: UsageAction) {
        when (action) {
            is UsageAction.GrantDeviceTime ->
                saveGrant(
                    targetType = BonusTargetType.DEVICE,
                    packageName = null,
                    durationMillis = action.durationMillis,
                    reason = action.reason
                )

            is UsageAction.GrantAppTime ->
                saveGrant(
                    targetType = BonusTargetType.APPLICATION,
                    packageName = action.packageName,
                    durationMillis = action.durationMillis,
                    reason = action.reason
                )

            is UsageAction.CancelGrant ->
                viewModelScope.launch {
                    runCatching {
                        repository.deactivateBonusGrant(action.id)
                    }.onFailure {
                        error.value = it.message
                    }
                }
        }
    }

    private fun saveGrant(
        targetType: BonusTargetType,
        packageName: String?,
        durationMillis: Long,
        reason: String?
    ) {
        viewModelScope.launch {
            runCatching {
                require(durationMillis in 60_000L..86_400_000L)
                repository.saveBonusGrant(
                    BonusTimeGrant(
                        id = UUID.randomUUID().toString(),
                        targetType = targetType,
                        targetPackageName = packageName,
                        durationMillis = durationMillis,
                        grantedAtEpochMillis = epochTime(),
                        expiresAtEpochMillis = null,
                        reason = reason?.take(200),
                        active = true
                    )
                )
            }.onFailure {
                error.value = it.message
            }
        }
    }
}
