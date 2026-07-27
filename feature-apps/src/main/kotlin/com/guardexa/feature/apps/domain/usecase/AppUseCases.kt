
package com.guardexa.feature.apps.domain.usecase

import com.guardexa.feature.apps.domain.model.*
import com.guardexa.feature.apps.domain.repository.AppsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveAppsUseCase(private val repository: AppsRepository) {
    operator fun invoke(profileId: String): Flow<List<AppWithPolicy>> =
        repository.observeApps(profileId)
}

class FilterAppsUseCase {
    operator fun invoke(
        apps: List<AppWithPolicy>,
        query: String,
        onlyUnreviewed: Boolean,
        onlyRequiresGlasses: Boolean,
        onlyAlwaysAllowed: Boolean
    ): List<AppWithPolicy> {
        val normalized = query.trim().lowercase()
        return apps
            .asSequence()
            .filter { !onlyUnreviewed || !it.app.reviewed }
            .filter { !onlyRequiresGlasses || it.policy?.requiresGlasses == true }
            .filter { !onlyAlwaysAllowed || it.policy?.alwaysAllowed == true }
            .filter {
                normalized.isBlank() ||
                    it.app.originalName.lowercase().contains(normalized) ||
                    it.app.packageName.lowercase().contains(normalized)
            }
            .sortedWith(
                compareBy<AppWithPolicy> { it.app.reviewed }
                    .thenBy { it.effectiveCategory.name }
                    .thenBy { it.app.originalName.lowercase() }
            )
            .toList()
    }
}

class SaveAppPolicyUseCase(private val repository: AppsRepository) {
    suspend operator fun invoke(policy: AppPolicy) {
        require(policy.gracePeriodSeconds in 0..3600) { "Invalid grace period" }
        require(policy.dailyLimitMinutes == null || policy.dailyLimitMinutes in 0..1440) {
            "Invalid daily limit"
        }
        repository.savePolicy(policy)
    }
}

class ReviewAppUseCase(private val repository: AppsRepository) {
    suspend operator fun invoke(packageName: String) = repository.markReviewed(packageName)
}

class ChangeAppCategoryUseCase(private val repository: AppsRepository) {
    suspend operator fun invoke(
        packageName: String,
        profileId: String,
        category: AppCategory
    ) = repository.updateCategory(packageName, profileId, category)
}
