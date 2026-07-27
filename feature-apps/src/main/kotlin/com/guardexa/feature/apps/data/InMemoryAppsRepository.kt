
package com.guardexa.feature.apps.data

import com.guardexa.feature.apps.domain.model.*
import com.guardexa.feature.apps.domain.repository.AppsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryAppsRepository(
    initialApps: List<InstalledApp> = emptyList()
) : AppsRepository {

    private val apps = MutableStateFlow(initialApps.associateBy { it.packageName })
    private val policies = MutableStateFlow<Map<Pair<String, String>, AppPolicy>>(emptyMap())

    override fun observeApps(profileId: String): Flow<List<AppWithPolicy>> =
        apps.map { map ->
            map.values.map { app ->
                AppWithPolicy(
                    app = app,
                    policy = policies.value[app.packageName to profileId]
                )
            }
        }

    override suspend fun getApp(packageName: String, profileId: String): AppWithPolicy? {
        val app = apps.value[packageName] ?: return null
        return AppWithPolicy(app, policies.value[packageName to profileId])
    }

    override suspend fun savePolicy(policy: AppPolicy) {
        policies.update { current ->
            current + ((policy.packageName to policy.profileId) to policy)
        }
    }

    override suspend fun markReviewed(packageName: String) {
        apps.update { current ->
            val app = current[packageName] ?: return@update current
            current + (packageName to app.copy(reviewed = true))
        }
    }

    override suspend fun updateCategory(
        packageName: String,
        profileId: String,
        category: AppCategory
    ) {
        val current = policies.value[packageName to profileId]
            ?: AppPolicy(
                packageName = packageName,
                profileId = profileId,
                accessPolicy = AppAccessPolicy.USE_GLOBAL_POLICY,
                dailyLimitMinutes = null,
                requiresGlasses = false,
                alwaysAllowed = false,
                emergencyApp = false,
                allowCurrentActivityToFinish = false,
                gracePeriodSeconds = 30
            )
        savePolicy(current.copy(categoryOverride = category))
    }

    override suspend fun applyDefaultPolicy(
        app: InstalledApp,
        profileId: String,
        defaultPolicy: NewAppDefaultPolicy,
        defaultDailyLimitMinutes: Int,
        defaultGracePeriodSeconds: Int
    ): AppPolicy {
        val policy = when (defaultPolicy) {
            NewAppDefaultPolicy.BLOCK -> AppPolicy(
                app.packageName, profileId, AppAccessPolicy.ALWAYS_BLOCKED,
                null, false, false, false, false, defaultGracePeriodSeconds
            )
            NewAppDefaultPolicy.ALLOW -> AppPolicy(
                app.packageName, profileId, AppAccessPolicy.ALWAYS_ALLOWED,
                null, false, true, false, true, defaultGracePeriodSeconds
            )
            NewAppDefaultPolicy.REQUIRE_GLASSES -> AppPolicy(
                app.packageName, profileId, AppAccessPolicy.GLASSES_REQUIRED,
                null, true, false, false, false, defaultGracePeriodSeconds
            )
            NewAppDefaultPolicy.USE_DAILY_LIMIT -> AppPolicy(
                app.packageName, profileId, AppAccessPolicy.ALLOWED_BY_DAILY_LIMIT,
                defaultDailyLimitMinutes, false, false, false, false, defaultGracePeriodSeconds
            )
            NewAppDefaultPolicy.CUSTOM -> AppPolicy(
                app.packageName, profileId, AppAccessPolicy.USE_GLOBAL_POLICY,
                null, false, false, false, false, defaultGracePeriodSeconds
            )
        }
        savePolicy(policy)
        return policy
    }
}
