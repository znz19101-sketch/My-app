
package com.guardexa.core.database.dao

import androidx.room.*
import com.guardexa.core.database.entity.ProtectionProfileEntity

@Dao
interface ProtectionProfileDao {

    @Query("SELECT * FROM protection_profiles")
    suspend fun getAll(): List<ProtectionProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProtectionProfileEntity)
}
