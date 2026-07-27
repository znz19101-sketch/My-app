
package com.guardexa.feature.apps.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "installed_apps",
    indices = [
        Index(value = ["originalName"]),
        Index(value = ["category"]),
        Index(value = ["reviewed"]),
        Index(value = ["lastUpdatedAt"])
    ]
)
data class InstalledAppEntity(
    @PrimaryKey
    val packageName: String,
    val originalName: String,
    val versionCode: Long,
    val versionName: String?,
    val installerPackage: String?,
    val isSystemApp: Boolean,
    val category: String,
    val firstDetectedAt: Long,
    val lastUpdatedAt: Long,
    val lastSeenAt: Long,
    val reviewed: Boolean,
    val isRemoved: Boolean = false
)
