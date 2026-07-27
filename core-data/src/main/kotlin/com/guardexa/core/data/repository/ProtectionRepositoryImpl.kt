
package com.guardexa.core.data.repository

import com.guardexa.core.domain.model.ProtectionStatus
import com.guardexa.core.domain.repository.ProtectionRepository

class ProtectionRepositoryImpl: ProtectionRepository{
    override suspend fun getStatus(): ProtectionStatus =
        ProtectionStatus(false,null)

    override suspend fun setEnabled(enabled:Boolean){
        // TODO: connect to datasource
    }
}
