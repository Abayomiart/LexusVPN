package com.novavpn.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novavpn.app.NovaVpnApplication
import com.novavpn.app.vpn.VpnManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val autoConnect: Boolean = false,
    val killSwitch: Boolean = false,
    val splitTunneling: Boolean = false,
    val customDns: String = "1.1.1.1",
    val showDiagnostics: Boolean = true,
    val diagnosticLogs: List<String> = emptyList()
)

class SettingsViewModel : ViewModel() {

    private val app = NovaVpnApplication.instance
    private val preferences = app.preferencesDataStore

    val logs: StateFlow<List<String>> = VpnManager.diagnosticLogs

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.themeModeFlow,
        preferences.autoConnectFlow,
        preferences.killSwitchFlow,
        preferences.splitTunnelingFlow,
        preferences.customDnsFlow,
        preferences.showDiagnosticsFlow,
        logs
    ) { theme, auto, kill, split, dns, diag, logList ->
        SettingsUiState(
            themeMode = theme,
            autoConnect = auto,
            killSwitch = kill,
            splitTunneling = split,
            customDns = dns,
            showDiagnostics = diag,
            diagnosticLogs = logList
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setThemeMode(mode: String) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setAutoConnect(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoConnect(enabled) }
    }

    fun setKillSwitch(enabled: Boolean) {
        viewModelScope.launch { preferences.setKillSwitch(enabled) }
    }

    fun setSplitTunneling(enabled: Boolean) {
        viewModelScope.launch { preferences.setSplitTunneling(enabled) }
    }

    fun setCustomDns(dns: String) {
        viewModelScope.launch { preferences.setCustomDns(dns) }
    }

    fun setShowDiagnostics(enabled: Boolean) {
        viewModelScope.launch { preferences.setShowDiagnostics(enabled) }
    }

    fun clearLogs() {
        VpnManager.addLog("Logs de diagnóstico reiniciados.")
    }
}
