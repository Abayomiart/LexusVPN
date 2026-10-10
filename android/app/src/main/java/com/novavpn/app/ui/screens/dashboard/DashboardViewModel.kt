package com.novavpn.app.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novavpn.app.NovaVpnApplication
import com.novavpn.app.domain.model.VpnServer
import com.novavpn.app.vpn.VpnConnectionState
import com.novavpn.app.vpn.VpnManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val connectionState: VpnConnectionState = VpnConnectionState.Disconnected,
    val selectedServer: VpnServer? = null,
    val formattedDuration: String = "00:00:00"
)

class DashboardViewModel : ViewModel() {

    private val app = NovaVpnApplication.instance
    private val repository = app.serverRepository
    private val preferences = app.preferencesDataStore

    val connectionState: StateFlow<VpnConnectionState> = VpnManager.connectionState

    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer: StateFlow<VpnServer?> = _selectedServer.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllServers(),
                preferences.selectedServerIdFlow
            ) { servers, selectedId ->
                if (servers.isEmpty()) null
                else if (selectedId != null) servers.find { it.id == selectedId } ?: servers.first()
                else servers.first()
            }.collect { server ->
                _selectedServer.value = server
            }
        }
    }

    fun toggleConnection(context: Context, onPrepareRequired: () -> Unit) {
        when (connectionState.value) {
            is VpnConnectionState.Connected -> {
                VpnManager.stopVpnService(context)
            }
            is VpnConnectionState.Connecting -> {
                VpnManager.stopVpnService(context)
            }
            is VpnConnectionState.Disconnected, is VpnConnectionState.Error -> {
                onPrepareRequired()
            }
        }
    }
}
