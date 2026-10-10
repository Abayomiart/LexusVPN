package com.novavpn.app.ui.screens.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novavpn.app.NovaVpnApplication
import com.novavpn.app.domain.model.VpnServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ServersUiState(
    val servers: List<VpnServer> = emptyList(),
    val searchQuery: String = "",
    val showOnlyFavorites: Boolean = false,
    val selectedServerId: String? = null,
    val serverToDelete: VpnServer? = null
)

class ServersViewModel : ViewModel() {

    private val app = NovaVpnApplication.instance
    private val repository = app.serverRepository
    private val preferences = app.preferencesDataStore

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites.asStateFlow()

    private val _serverToDelete = MutableStateFlow<VpnServer?>(null)
    val serverToDelete: StateFlow<VpnServer?> = _serverToDelete.asStateFlow()

    val uiState: StateFlow<ServersUiState> = combine(
        repository.getAllServers(),
        _searchQuery,
        _showOnlyFavorites,
        preferences.selectedServerIdFlow,
        _serverToDelete
    ) { servers, query, onlyFavs, selectedId, toDelete ->
        val filtered = servers.filter { server ->
            val matchesQuery = server.name.contains(query, ignoreCase = true) ||
                    server.country.contains(query, ignoreCase = true) ||
                    server.host.contains(query, ignoreCase = true)
            val matchesFav = if (onlyFavs) server.favorite else true
            matchesQuery && matchesFav
        }
        ServersUiState(
            servers = filtered,
            searchQuery = query,
            showOnlyFavorites = onlyFavs,
            selectedServerId = selectedId ?: servers.firstOrNull()?.id,
            serverToDelete = toDelete
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServersUiState())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesFilter() {
        _showOnlyFavorites.value = !_showOnlyFavorites.value
    }

    fun selectServer(server: VpnServer) {
        viewModelScope.launch {
            preferences.setSelectedServerId(server.id)
        }
    }

    fun toggleFavorite(server: VpnServer) {
        viewModelScope.launch {
            repository.toggleFavorite(server.id, !server.favorite)
        }
    }

    fun confirmDelete(server: VpnServer) {
        _serverToDelete.value = server
    }

    fun dismissDeleteDialog() {
        _serverToDelete.value = null
    }

    fun executeDeleteServer() {
        val server = _serverToDelete.value ?: return
        viewModelScope.launch {
            repository.deleteServer(server.id)
            app.keyStoreManager.deleteKey(server.privateKeyAlias)
            _serverToDelete.value = null
        }
    }

    fun autoSelectBestServer() {
        viewModelScope.launch {
            // Seleciona o primeiro servidor favorito ou com menor latência estimada
            val all = uiState.value.servers
            val best = all.find { it.favorite } ?: all.firstOrNull()
            if (best != null) {
                preferences.setSelectedServerId(best.id)
            }
        }
    }
}
