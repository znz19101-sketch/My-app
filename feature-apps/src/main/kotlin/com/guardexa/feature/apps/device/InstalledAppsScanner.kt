
package com.guardexa.feature.apps.device

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.guardexa.feature.apps.domain.model.AppCategory
import com.guardexa.feature.apps.domain.model.InstalledApp

interface InstalledAppsScanner {
    suspend fun scan(): List<InstalledApp>
    suspend fun read(packageName: String): InstalledApp?
}

class AndroidInstalledAppsScanner(
    private val context: Context,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : InstalledAppsScanner {

    private val packageManager: PackageManager
        get() = context.packageManager

    override suspend fun scan(): List<InstalledApp> =
        installedPackages()
            .asSequence()
            .mapNotNull(::mapPackage)
            .filterNot { it.packageName == context.packageName }
            .sortedBy { it.originalName.lowercase() }
            .toList()

    override suspend fun read(packageName: String): InstalledApp? {
        val packageInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
        }.getOrNull()

        return packageInfo?.let(::mapPackage)
    }

    private fun installedPackages(): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(0)
        }

    private fun mapPackage(info: PackageInfo): InstalledApp? {
        val appInfo = info.applicationInfo ?: return null
        val now = clock()

        return InstalledApp(
            packageName = info.packageName,
            originalName = packageManager
                .getApplicationLabel(appInfo)
                .toString()
                .ifBlank { info.packageName },
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            },
            versionName = info.versionName,
            installerPackage = installerPackageName(info.packageName),
            isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            category = detectCategory(appInfo),
            firstDetectedAt = now,
            lastUpdatedAt = info.lastUpdateTime.coerceAtLeast(now),
            reviewed = false
        )
    }

    private fun installerPackageName(packageName: String): String? =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        }.getOrNull()

    private fun detectCategory(info: ApplicationInfo): AppCategory {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                AppCategory.SYSTEM
            } else {
                AppCategory.OTHER
            }
        }

        return when (info.category) {
            ApplicationInfo.CATEGORY_GAME -> AppCategory.GAME
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE -> AppCategory.VIDEO
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.TOOLS
            ApplicationInfo.CATEGORY_NEWS -> AppCategory.BOOKS
            ApplicationInfo.CATEGORY_MAPS -> AppCategory.TOOLS
            ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.HEALTH
            else -> if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                AppCategory.SYSTEM
            } else {
                AppCategory.OTHER
            }
        }
    }
}
