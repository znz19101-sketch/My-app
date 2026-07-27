
package com.guardexa.feature.apps.domain.repository

import com.guardexa.feature.apps.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AppsRepository {
    fun observeApps(profileId: String): Flow<List<AppWithPolicy>>
    suspend fun getApp(packageName: String, profileId: String): AppWithPolicy?
    suspend fun savePolicy(policy: AppPolicy)
    suspend fun markReviewed(packageName: String)
    suspend fun updateCategory(packageName: String, profileId: String, category: AppCategory)
    suspend fun applyDefaultPolicy(
        app: InstalledApp,
        profileId: String,
        defaultPolicy: NewAppDefaultPolicy,
        defaultDailyLimitMinutes: Int,
        defaultGracePeriodSeconds: Int
    ): AppPolicy
}
