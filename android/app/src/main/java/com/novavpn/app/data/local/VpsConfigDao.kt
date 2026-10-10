package com.novavpn.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VpsConfigDao {

    @Query("SELECT * FROM vps_configurations ORDER BY createdAt DESC")
    fun getAllConfigs(): Flow<List<VpsConfigEntity>>

    @Query("SELECT * FROM vps_configurations WHERE id = :id")
    suspend fun getConfigById(id: String): VpsConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: VpsConfigEntity)

    @Update
    suspend fun updateConfig(config: VpsConfigEntity)

    @Delete
    suspend fun deleteConfig(config: VpsConfigEntity)

    @Query("DELETE FROM vps_configurations WHERE id = :id")
    suspend fun deleteConfigById(id: String)
}
