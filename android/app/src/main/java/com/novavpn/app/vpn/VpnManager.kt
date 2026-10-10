package com.novavpn.app.vpn

import android.content.Context
import android.content.Intent
import android.os.Build
import com.novavpn.app.data.local.PreferencesDataStore
import com.novavpn.app.domain.repository.VpnServerRepository
import com.novavpn.app.security.KeyStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ponto central de gerenciamento e emissão de estados reativos da VPN via StateFlow.
 * Controla os comandos de ciclo de vida enviados ao NovaVpnService.
 */
object VpnManager {

    private val _connectionState = MutableStateFlow<VpnConnectionState>(VpnConnectionState.Disconnected)
    val connectionState: StateFlow<VpnConnectionState> = _connectionState.asStateFlow()

    private val _diagnosticLogs = MutableStateFlow<List<String>>(
        listOf("Sistema NovaVPN inicializado com sucesso.")
    )
    val diagnosticLogs: StateFlow<List<String>> = _diagnosticLogs.asStateFlow()

    private var appContext: Context? = null
    var serverRepository: VpnServerRepository? = null
    var preferencesDataStore: PreferencesDataStore? = null
    var keyStoreManager: KeyStoreManager? = null

    fun init(
        context: Context,
        serverRepository: VpnServerRepository,
        preferencesDataStore: PreferencesDataStore,
        keyStoreManager: KeyStoreManager
    ) {
        this.appContext = context.applicationContext
        this.serverRepository = serverRepository
        this.preferencesDataStore = preferencesDataStore
        this.keyStoreManager = keyStoreManager
    }

    fun updateState(state: VpnConnectionState) {
        _connectionState.value = state
        val logEntry = when (state) {
            is VpnConnectionState.Disconnected -> "VPN Desconectada. Interface TUN liberada."
            is VpnConnectionState.Connecting -> "A ligar a ${state.server.name} via túnel seguro..."
            is VpnConnectionState.Connected -> "Conectado a ${state.server.name} (${state.server.host}:${state.server.port}) com protect(socket) ativo."
            is VpnConnectionState.Error -> "ERRO: ${state.message}"
        }
        addLog(logEntry)
    }

    fun addLog(entry: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val formatted = "[$timestamp] $entry"
        _diagnosticLogs.value = (_diagnosticLogs.value + formatted).takeLast(100)
    }

    fun startVpnService(context: Context, serverId: String? = null) {
        val intent = Intent(context, NovaVpnService::class.java).apply {
            action = NovaVpnService.ACTION_CONNECT
            if (serverId != null) {
                putExtra(NovaVpnService.EXTRA_SERVER_ID, serverId)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopVpnService(context: Context) {
        val intent = Intent(context, NovaVpnService::class.java).apply {
            action = NovaVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }
}
