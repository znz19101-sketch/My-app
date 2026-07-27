
package com.guardexa.feature.apps.data.local.mapper

import com.guardexa.feature.apps.data.local.entity.*
import com.guardexa.feature.apps.domain.model.*

object AppsEntityMapper {

    fun InstalledAppEntity.toDomain(): InstalledApp =
        InstalledApp(
            packageName = packageName,
            originalName = originalName,
            versionCode = versionCode,
            versionName = versionName,
            installerPackage = installerPackage,
            isSystemApp = isSystemApp,
            category = runCatching { AppCategory.valueOf(category) }
                .getOrDefault(AppCategory.OTHER),
            firstDetectedAt = firstDetectedAt,
            lastUpdatedAt = lastUpdatedAt,
            reviewed = reviewed
        )

    fun AppPolicyEntity.toDomain(): AppPolicy =
        AppPolicy(
            packageName = packageName,
            profileId = profileId,
            accessPolicy = runCatching { AppAccessPolicy.valueOf(accessPolicy) }
                .getOrDefault(AppAccessPolicy.USE_GLOBAL_POLICY),
            dailyLimitMinutes = dailyLimitMinutes,
            requiresGlasses = requiresGlasses,
            alwaysAllowed = alwaysAllowed,
            emergencyApp = emergencyApp,
            allowCurrentActivityToFinish = allowCurrentActivityToFinish,
            gracePeriodSeconds = gracePeriodSeconds,
            categoryOverride = categoryOverride?.let {
                runCatching { AppCategory.valueOf(it) }.getOrNull()
            }
        )

    fun InstalledApp.toEntity(
        lastSeenAt: Long,
        isRemoved: Boolean = false
    ): InstalledAppEntity =
        InstalledAppEntity(
            packageName = packageName,
            originalName = originalName,
            versionCode = versionCode,
            versionName = versionName,
            installerPackage = installerPackage,
            isSystemApp = isSystemApp,
            category = category.name,
            firstDetectedAt = firstDetectedAt,
            lastUpdatedAt = lastUpdatedAt,
            lastSeenAt = lastSeenAt,
            reviewed = reviewed,
            isRemoved = isRemoved
        )

    fun AppPolicy.toEntity(
        now: Long,
        existing: AppPolicyEntity? = null
    ): AppPolicyEntity =
        AppPolicyEntity(
            packageName = packageName,
            profileId = profileId,
            accessPolicy = accessPolicy.name,
            dailyLimitMinutes = dailyLimitMinutes,
            requiresGlasses = requiresGlasses,
            alwaysAllowed = alwaysAllowed,
            emergencyApp = emergencyApp,
            allowCurrentActivityToFinish = allowCurrentActivityToFinish,
            gracePeriodSeconds = gracePeriodSeconds,
            categoryOverride = categoryOverride?.name,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            recordVersion = existing?.recordVersion ?: 1,
            isDeleted = false,
            deletedAt = null
        )

    fun AppWithPolicyRow.toDomain(profileId: String): AppWithPolicy {
        val activePolicy = policies.firstOrNull {
            it.profileId == profileId && !it.isDeleted
        }
        return AppWithPolicy(
            app = app.toDomain(),
            policy = activePolicy?.toDomain()
        )
    }
}
