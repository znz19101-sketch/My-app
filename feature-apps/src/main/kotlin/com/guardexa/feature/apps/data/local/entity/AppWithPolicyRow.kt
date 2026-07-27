
package com.guardexa.feature.apps.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AppWithPolicyRow(
    @Embedded
    val app: InstalledAppEntity,

    @Relation(
        parentColumn = "packageName",
        entityColumn = "packageName"
    )
    val policies: List<AppPolicyEntity>
)
