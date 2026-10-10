package com.novavpn.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnServerDao {

    @Query("SELECT * FROM vpn_servers ORDER BY favorite DESC, name ASC")
    fun getAllServers(): Flow<List<VpnServerEntity>>

    @Query("SELECT * FROM vpn_servers WHERE id = :id")
    suspend fun getServerById(id: String): VpnServerEntity?

    @Query("SELECT * FROM vpn_servers WHERE favorite = 1")
    fun getFavoriteServers(): Flow<List<VpnServerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: VpnServerEntity)

    @Update
    suspend fun updateServer(server: VpnServerEntity)

    @Query("UPDATE vpn_servers SET favorite = :favorite WHERE id = :id")
    suspend fun updateFavorite(id: String, favorite: Boolean)

    @Delete
    suspend fun deleteServer(server: VpnServerEntity)

    @Query("DELETE FROM vpn_servers WHERE id = :id")
    suspend fun deleteServerById(id: String)
}
