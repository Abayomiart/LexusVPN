package com.novavpn.app.data.repository

import com.novavpn.app.data.local.VpnServerDao
import com.novavpn.app.data.local.toDomain
import com.novavpn.app.data.local.toEntity
import com.novavpn.app.domain.model.VpnServer
import com.novavpn.app.domain.repository.VpnServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VpnServerRepositoryImpl(
    private val dao: VpnServerDao
) : VpnServerRepository {

    override fun getAllServers(): Flow<List<VpnServer>> {
        return dao.getAllServers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteServers(): Flow<List<VpnServer>> {
        return dao.getFavoriteServers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getServerById(id: String): VpnServer? {
        return dao.getServerById(id)?.toDomain()
    }

    override suspend fun insertServer(server: VpnServer) {
        dao.insertServer(server.toEntity())
    }

    override suspend fun updateServer(server: VpnServer) {
        dao.updateServer(server.toEntity())
    }

    override suspend fun toggleFavorite(id: String, favorite: Boolean) {
        dao.updateFavorite(id, favorite)
    }

    override suspend fun deleteServer(id: String) {
        dao.deleteServerById(id)
    }
}
