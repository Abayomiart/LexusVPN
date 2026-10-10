package com.novavpn.app.domain.repository

import com.novavpn.app.domain.model.VpsServerConfig
import kotlinx.coroutines.flow.Flow

interface VpsConfigRepository {
    fun getAllConfigs(): Flow<List<VpsServerConfig>>
    suspend fun getConfigById(id: String): VpsServerConfig?
    suspend fun insertConfig(config: VpsServerConfig)
    suspend fun updateConfig(config: VpsServerConfig)
    suspend fun deleteConfig(id: String)
}
