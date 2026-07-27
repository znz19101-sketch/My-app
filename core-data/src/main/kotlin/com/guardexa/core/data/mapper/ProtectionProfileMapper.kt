
package com.guardexa.core.data.mapper

import com.guardexa.core.database.entity.ProtectionProfileEntity
import com.guardexa.core.domain.model.ProtectionStatus

object ProtectionProfileMapper {
    fun toDomain(entity: ProtectionProfileEntity)=
        ProtectionStatus(entity.enabled, entity.name)
}
