package com.novavpn.app.data.repository

import com.novavpn.app.data.local.VpsConfigDao
import com.novavpn.app.data.local.toDomain
import com.novavpn.app.data.local.toEntity
import com.novavpn.app.domain.model.VpsServerConfig
import com.novavpn.app.domain.repository.VpsConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VpsConfigRepositoryImpl(
    private val dao: VpsConfigDao
) : VpsConfigRepository {

    override fun getAllConfigs(): Flow<List<VpsServerConfig>> {
        return dao.getAllConfigs().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getConfigById(id: String): VpsServerConfig? {
        return dao.getConfigById(id)?.toDomain()
    }

    override suspend fun insertConfig(config: VpsServerConfig) {
        dao.insertConfig(config.toEntity())
    }

    override suspend fun updateConfig(config: VpsServerConfig) {
        dao.updateConfig(config.toEntity())
    }

    override suspend fun deleteConfig(id: String) {
        dao.deleteConfigById(id)
    }
}
