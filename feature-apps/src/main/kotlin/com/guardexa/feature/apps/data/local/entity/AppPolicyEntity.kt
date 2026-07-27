
package com.guardexa.feature.apps.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "app_policies",
    primaryKeys = ["packageName", "profileId"],
    foreignKeys = [
        ForeignKey(
            entity = InstalledAppEntity::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["profileId"]),
        Index(value = ["accessPolicy"]),
        Index(value = ["requiresGlasses"]),
        Index(value = ["alwaysAllowed"])
    ]
)
data class AppPolicyEntity(
    val packageName: String,
    val profileId: String,
    val accessPolicy: String,
    val dailyLimitMinutes: Int?,
    val requiresGlasses: Boolean,
    val alwaysAllowed: Boolean,
    val emergencyApp: Boolean,
    val allowCurrentActivityToFinish: Boolean,
    val gracePeriodSeconds: Int,
    val categoryOverride: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val recordVersion: Int = 1,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
