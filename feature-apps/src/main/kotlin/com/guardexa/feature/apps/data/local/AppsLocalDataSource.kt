
package com.guardexa.feature.apps.data.local

import androidx.room.withTransaction
import com.guardexa.core.database.AppDatabase
import com.guardexa.feature.apps.data.local.dao.AppsDao
import com.guardexa.feature.apps.data.local.mapper.AppsEntityMapper.toEntity
import com.guardexa.feature.apps.device.InstalledAppsScanner
import com.guardexa.feature.apps.domain.model.InstalledApp

class AppsLocalDataSource(
    private val database: AppDatabase,
    private val dao: AppsDao,
    private val scanner: InstalledAppsScanner,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun synchronizeInstalledApps(): SyncResult {
        val now = clock()
        val scanned = scanner.scan()
        val scannedByPackage = scanned.associateBy { it.packageName }
        val stored = dao.getAllPresentApps()

        database.withTransaction {
            scanned.forEach { discovered ->
                val old = dao.getInstalledApp(discovered.packageName)
                val merged = merge(old?.let {
                    InstalledApp(
                        packageName = it.packageName,
                        originalName = it.originalName,
                        versionCode = it.versionCode,
                        versionName = it.versionName,
                        installerPackage = it.installerPackage,
                        isSystemApp = it.isSystemApp,
                        category = runCatching {
                            com.guardexa.feature.apps.domain.model.AppCategory.valueOf(it.category)
                        }.getOrDefault(
                            com.guardexa.feature.apps.domain.model.AppCategory.OTHER
                        ),
                        firstDetectedAt = it.firstDetectedAt,
                        lastUpdatedAt = it.lastUpdatedAt,
                        reviewed = it.reviewed
                    )
                }, discovered)

                dao.upsertInstalledApp(
                    merged.toEntity(
                        lastSeenAt = now,
                        isRemoved = false
                    )
                )
            }

            stored
                .asSequence()
                .filterNot { scannedByPackage.containsKey(it.packageName) }
                .forEach { dao.markRemoved(it.packageName, now) }
        }

        return SyncResult(
            discoveredCount = scanned.count { discovered ->
                stored.none { it.packageName == discovered.packageName }
            },
            updatedCount = scanned.count { discovered ->
                stored.any {
                    it.packageName == discovered.packageName &&
                        it.versionCode != discovered.versionCode
                }
            },
            removedCount = stored.count {
                !scannedByPackage.containsKey(it.packageName)
            }
        )
    }

    suspend fun synchronizeSinglePackage(packageName: String): PackageSyncResult {
        val now = clock()
        val discovered = scanner.read(packageName)

        return database.withTransaction {
            if (discovered == null) {
                dao.markRemoved(packageName, now)
                PackageSyncResult.Removed(packageName)
            } else {
                val old = dao.getInstalledApp(packageName)
                val oldDomain = old?.let {
                    InstalledApp(
                        packageName = it.packageName,
                        originalName = it.originalName,
                        versionCode = it.versionCode,
                        versionName = it.versionName,
                        installerPackage = it.installerPackage,
                        isSystemApp = it.isSystemApp,
                        category = runCatching {
                            com.guardexa.feature.apps.domain.model.AppCategory.valueOf(it.category)
                        }.getOrDefault(
                            com.guardexa.feature.apps.domain.model.AppCategory.OTHER
                        ),
                        firstDetectedAt = it.firstDetectedAt,
                        lastUpdatedAt = it.lastUpdatedAt,
                        reviewed = it.reviewed
                    )
                }

                val merged = merge(oldDomain, discovered)
                dao.upsertInstalledApp(
                    merged.toEntity(lastSeenAt = now)
                )

                when {
                    old == null -> PackageSyncResult.Installed(merged)
                    old.versionCode != merged.versionCode ->
                        PackageSyncResult.Updated(merged)
                    else -> PackageSyncResult.Refreshed(merged)
                }
            }
        }
    }

    private fun merge(
        old: InstalledApp?,
        discovered: InstalledApp
    ): InstalledApp =
        discovered.copy(
            firstDetectedAt = old?.firstDetectedAt ?: discovered.firstDetectedAt,
            reviewed = old?.reviewed ?: false
        )
}

data class SyncResult(
    val discoveredCount: Int,
    val updatedCount: Int,
    val removedCount: Int
)

sealed interface PackageSyncResult {
    data class Installed(val app: InstalledApp) : PackageSyncResult
    data class Updated(val app: InstalledApp) : PackageSyncResult
    data class Refreshed(val app: InstalledApp) : PackageSyncResult
    data class Removed(val packageName: String) : PackageSyncResult
}
