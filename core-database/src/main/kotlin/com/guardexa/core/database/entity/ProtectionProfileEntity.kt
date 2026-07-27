
package com.guardexa.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="protection_profiles")
data class ProtectionProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val enabled: Boolean,
    val createdAt: Long
)
