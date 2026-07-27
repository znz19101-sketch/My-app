
package com.guardexa.feature.apps.data.repository

import com.guardexa.feature.apps.data.local.dao.AppsDao
import com.guardexa.feature.apps.data.local.mapper.AppsEntityMapper.toDomain
import com.guardexa.feature.apps.data.local.mapper.AppsEntityMapper.toEntity
import com.guardexa.feature.apps.domain.model.*
import com.guardexa.feature.apps.domain.repository.AppsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAppsRepository(
    private val dao: AppsDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : AppsRepository {

    override fun observeApps(profileId: String): Flow<List<AppWithPolicy>> =
        dao.observeInstalledAppsWithPolicies().map { rows ->
            rows.map { row ->
                val policy = row.policies.firstOrNull {
                    it.profileId == profileId && !it.isDeleted
                }
                AppWithPolicy(
                    app = row.app.toDomain(),
                    policy = policy?.toDomain()
                )
            }
        }

    override suspend fun getApp(
        packageName: String,
        profileId: String
    ): AppWithPolicy? {
        val row = dao.getAppWithPolicies(packageName) ?: return null
        val policy = row.policies.firstOrNull {
            it.profileId == profileId && !it.isDeleted
        }
        return AppWithPolicy(
            app = row.app.toDomain(),
            policy = policy?.toDomain()
        )
    }

    override suspend fun savePolicy(policy: AppPolicy) {
        val now = clock()
        val existing = dao.getPolicy(policy.packageName, policy.profileId)
        dao.upsertPolicy(policy.toEntity(now = now, existing = existing))
    }

    override suspend fun markReviewed(packageName: String) {
        dao.markReviewed(packageName, clock())
    }

    override suspend fun updateCategory(
        packageName: String,
        profileId: String,
        category: AppCategory
    ) {
        val now = clock()
        val existing = dao.getPolicy(packageName, profileId)
        if (existing == null) {
            savePolicy(
                AppPolicy(
                    packageName = packageName,
                    profileId = profileId,
                    accessPolicy = AppAccessPolicy.USE_GLOBAL_POLICY,
                    dailyLimitMinutes = null,
                    requiresGlasses = false,
                    alwaysAllowed = false,
                    emergencyApp = false,
                    allowCurrentActivityToFinish = false,
                    gracePeriodSeconds = 30,
                    categoryOverride = category
                )
            )
        } else {
            dao.updateCategoryOverride(
                packageName = packageName,
                profileId = profileId,
                category = category.name,
                now = now
            )
        }
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
                packageName = app.packageName,
                profileId = profileId,
                accessPolicy = AppAccessPolicy.ALWAYS_BLOCKED,
                dailyLimitMinutes = null,
                requiresGlasses = false,
                alwaysAllowed = false,
                emergencyApp = false,
                allowCurrentActivityToFinish = false,
                gracePeriodSeconds = defaultGracePeriodSeconds
            )

            NewAppDefaultPolicy.ALLOW -> AppPolicy(
                packageName = app.packageName,
                profileId = profileId,
                accessPolicy = AppAccessPolicy.ALWAYS_ALLOWED,
                dailyLimitMinutes = null,
                requiresGlasses = false,
                alwaysAllowed = true,
                emergencyApp = false,
                allowCurrentActivityToFinish = true,
                gracePeriodSeconds = defaultGracePeriodSeconds
            )

            NewAppDefaultPolicy.REQUIRE_GLASSES -> AppPolicy(
                packageName = app.packageName,
                profileId = profileId,
                accessPolicy = AppAccessPolicy.GLASSES_REQUIRED,
                dailyLimitMinutes = null,
                requiresGlasses = true,
                alwaysAllowed = false,
                emergencyApp = false,
                allowCurrentActivityToFinish = false,
                gracePeriodSeconds = defaultGracePeriodSeconds
            )

            NewAppDefaultPolicy.USE_DAILY_LIMIT -> AppPolicy(
                packageName = app.packageName,
                profileId = profileId,
                accessPolicy = AppAccessPolicy.ALLOWED_BY_DAILY_LIMIT,
                dailyLimitMinutes = defaultDailyLimitMinutes,
                requiresGlasses = false,
                alwaysAllowed = false,
                emergencyApp = false,
                allowCurrentActivityToFinish = false,
                gracePeriodSeconds = defaultGracePeriodSeconds
            )

            NewAppDefaultPolicy.CUSTOM -> AppPolicy(
                packageName = app.packageName,
                profileId = profileId,
                accessPolicy = AppAccessPolicy.USE_GLOBAL_POLICY,
                dailyLimitMinutes = null,
                requiresGlasses = false,
                alwaysAllowed = false,
                emergencyApp = false,
                allowCurrentActivityToFinish = false,
                gracePeriodSeconds = defaultGracePeriodSeconds
            )
        }

        savePolicy(policy)
        return policy
    }
}
