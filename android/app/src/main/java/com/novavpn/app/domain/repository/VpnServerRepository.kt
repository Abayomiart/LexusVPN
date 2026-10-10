package com.novavpn.app.domain.repository

import com.novavpn.app.domain.model.VpnServer
import kotlinx.coroutines.flow.Flow

interface VpnServerRepository {
    fun getAllServers(): Flow<List<VpnServer>>
    fun getFavoriteServers(): Flow<List<VpnServer>>
    suspend fun getServerById(id: String): VpnServer?
    suspend fun insertServer(server: VpnServer)
    suspend fun updateServer(server: VpnServer)
    suspend fun toggleFavorite(id: String, favorite: Boolean)
    suspend fun deleteServer(id: String)
}
