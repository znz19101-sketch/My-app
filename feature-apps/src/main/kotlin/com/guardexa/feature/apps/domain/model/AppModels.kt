
package com.guardexa.feature.apps.domain.model

enum class AppCategory {
    EDUCATION, GAME, VIDEO, SOCIAL, BROWSER, BOOKS, TOOLS, HEALTH, COMMUNICATION, SYSTEM, OTHER
}

enum class AppAccessPolicy {
    ALWAYS_ALLOWED,
    ALLOWED_BY_SCHEDULE,
    ALLOWED_BY_DAILY_LIMIT,
    GLASSES_REQUIRED,
    ALWAYS_BLOCKED,
    EMERGENCY,
    USE_GLOBAL_POLICY
}

enum class NewAppDefaultPolicy {
    BLOCK,
    ALLOW,
    REQUIRE_GLASSES,
    USE_DAILY_LIMIT,
    CUSTOM
}

data class InstalledApp(
    val packageName: String,
    val originalName: String,
    val versionCode: Long,
    val versionName: String?,
    val installerPackage: String?,
    val isSystemApp: Boolean,
    val category: AppCategory,
    val firstDetectedAt: Long,
    val lastUpdatedAt: Long,
    val reviewed: Boolean
)

data class AppPolicy(
    val packageName: String,
    val profileId: String,
    val accessPolicy: AppAccessPolicy,
    val dailyLimitMinutes: Int?,
    val requiresGlasses: Boolean,
    val alwaysAllowed: Boolean,
    val emergencyApp: Boolean,
    val allowCurrentActivityToFinish: Boolean,
    val gracePeriodSeconds: Int,
    val categoryOverride: AppCategory? = null
)

data class AppWithPolicy(
    val app: InstalledApp,
    val policy: AppPolicy?
) {
    val effectiveCategory: AppCategory
        get() = policy?.categoryOverride ?: app.category
}
