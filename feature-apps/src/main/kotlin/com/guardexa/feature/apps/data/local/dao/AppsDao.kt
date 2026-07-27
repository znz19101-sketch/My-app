
package com.guardexa.feature.apps.data.local.dao

import androidx.room.*
import com.guardexa.feature.apps.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppsDao {

    @Transaction
    @Query("""
        SELECT * FROM installed_apps
        WHERE isRemoved = 0
        ORDER BY reviewed ASC, originalName COLLATE NOCASE ASC
    """)
    fun observeInstalledAppsWithPolicies(): Flow<List<AppWithPolicyRow>>

    @Transaction
    @Query("""
        SELECT * FROM installed_apps
        WHERE packageName = :packageName
        LIMIT 1
    """)
    suspend fun getAppWithPolicies(packageName: String): AppWithPolicyRow?

    @Query("""
        SELECT * FROM installed_apps
        WHERE packageName = :packageName
        LIMIT 1
    """)
    suspend fun getInstalledApp(packageName: String): InstalledAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstalledApp(entity: InstalledAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstalledApps(entities: List<InstalledAppEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPolicy(entity: AppPolicyEntity)

    @Query("""
        SELECT * FROM app_policies
        WHERE packageName = :packageName
          AND profileId = :profileId
          AND isDeleted = 0
        LIMIT 1
    """)
    suspend fun getPolicy(
        packageName: String,
        profileId: String
    ): AppPolicyEntity?

    @Query("""
        UPDATE installed_apps
        SET reviewed = 1,
            lastSeenAt = :now
        WHERE packageName = :packageName
    """)
    suspend fun markReviewed(packageName: String, now: Long)

    @Query("""
        UPDATE installed_apps
        SET isRemoved = 1,
            lastSeenAt = :now
        WHERE packageName = :packageName
    """)
    suspend fun markRemoved(packageName: String, now: Long)

    @Query("""
        UPDATE installed_apps
        SET isRemoved = 0,
            lastSeenAt = :now
        WHERE packageName = :packageName
    """)
    suspend fun markPresent(packageName: String, now: Long)

    @Query("""
        UPDATE app_policies
        SET categoryOverride = :category,
            updatedAt = :now
        WHERE packageName = :packageName
          AND profileId = :profileId
          AND isDeleted = 0
    """)
    suspend fun updateCategoryOverride(
        packageName: String,
        profileId: String,
        category: String,
        now: Long
    )

    @Query("""
        SELECT COUNT(*) FROM installed_apps
        WHERE reviewed = 0 AND isRemoved = 0
    """)
    fun observeUnreviewedCount(): Flow<Int>

    @Query("""
        SELECT * FROM installed_apps
        WHERE isRemoved = 0
    """)
    suspend fun getAllPresentApps(): List<InstalledAppEntity>
}
