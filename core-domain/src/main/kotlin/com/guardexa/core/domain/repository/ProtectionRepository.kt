
package com.guardexa.core.domain.repository

import com.guardexa.core.domain.model.ProtectionStatus

interface ProtectionRepository{
    suspend fun getStatus(): ProtectionStatus
    suspend fun setEnabled(enabled:Boolean)
}
