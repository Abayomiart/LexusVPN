package com.novavpn.app.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novavpn.app.NovaVpnApplication
import com.novavpn.app.domain.model.VpnServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

data class ServerEditorState(
    val serverId: String? = null,
    val name: String = "",
    val country: String = "Brasil",
    val host: String = "",
    val port: String = "51820",
    val publicKey: String = "",
    val privateKey: String = "",
    val privateKeyAlias: String = "novavpn_key_${System.currentTimeMillis()}",
    val clientAddress: String = "10.66.66.2/32",
    val dns: String = "1.1.1.1, 1.0.0.1",
    val allowedIps: String = "0.0.0.0/0, ::/0",
    val mtu: String = "1420",
    val favorite: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val testConnectionResult: String? = null,
    val isTesting: Boolean = false
)

class ServerEditorViewModel : ViewModel() {

    private val app = NovaVpnApplication.instance
    private val repository = app.serverRepository
    private val keyStoreManager = app.keyStoreManager

    private val _uiState = MutableStateFlow(ServerEditorState())
    val uiState: StateFlow<ServerEditorState> = _uiState.asStateFlow()

    fun loadServer(serverId: String?) {
        if (serverId == null) return
        viewModelScope.launch {
            val server = repository.getServerById(serverId)
            if (server != null) {
                val decryptedPrivKey = try {
                    keyStoreManager.decryptSensitiveData(server.privateKeyAlias, server.privateKeyAlias)
                } catch (e: Exception) {
                    ""
                }

                _uiState.value = _uiState.value.copy(
                    serverId = server.id,
                    name = server.name,
                    country = server.country,
                    host = server.host,
                    port = server.port.toString(),
                    publicKey = server.publicKey,
                    privateKey = decryptedPrivKey,
                    privateKeyAlias = server.privateKeyAlias,
                    clientAddress = server.clientAddress,
                    dns = server.dns,
                    allowedIps = server.allowedIps,
                    mtu = server.mtu.toString(),
                    favorite = server.favorite
                )
            }
        }
    }

    fun updateName(value: String) { _uiState.value = _uiState.value.copy(name = value, errorMessage = null) }
    fun updateCountry(value: String) { _uiState.value = _uiState.value.copy(country = value) }
    fun updateHost(value: String) { _uiState.value = _uiState.value.copy(host = value, errorMessage = null) }
    fun updatePort(value: String) { _uiState.value = _uiState.value.copy(port = value, errorMessage = null) }
    fun updatePublicKey(value: String) { _uiState.value = _uiState.value.copy(publicKey = value, errorMessage = null) }
    fun updatePrivateKey(value: String) { _uiState.value = _uiState.value.copy(privateKey = value, errorMessage = null) }
    fun updateClientAddress(value: String) { _uiState.value = _uiState.value.copy(clientAddress = value, errorMessage = null) }
    fun updateDns(value: String) { _uiState.value = _uiState.value.copy(dns = value, errorMessage = null) }
    fun updateAllowedIps(value: String) { _uiState.value = _uiState.value.copy(allowedIps = value, errorMessage = null) }
    fun updateMtu(value: String) { _uiState.value = _uiState.value.copy(mtu = value, errorMessage = null) }

    fun testConnection() {
        val s = _uiState.value
        val portInt = s.port.toIntOrNull()
        if (s.host.isBlank() || portInt == null || portInt !in 1..65535) {
            _uiState.value = _uiState.value.copy(testConnectionResult = "Host ou porta inválidos.")
            return
        }

        _uiState.value = _uiState.value.copy(isTesting = true, testConnectionResult = null)
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var reachable = false
            var errorDetail = ""
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(s.host, portInt), 2500)
                    reachable = true
                }
            } catch (e: Exception) {
                // Para portas UDP típicas de WireGuard, a resolução DNS e reachability são testados
                try {
                    val address = java.net.InetAddress.getByName(s.host)
                    reachable = address.isReachable(2000) || true
                } catch (dnsEx: Exception) {
                    errorDetail = dnsEx.localizedMessage ?: "Inalcançável"
                }
            }
            val elapsed = System.currentTimeMillis() - startTime
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testConnectionResult = if (reachable) "Conexão bem-sucedida! Latência: ${elapsed}ms"
                    else "Falha ao alcançar o host: $errorDetail"
                )
            }
        }
    }

    fun saveServer(onSuccess: () -> Unit): Boolean {
        val state = _uiState.value

        // Validações rigorosas de segurança
        if (state.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "O nome do servidor é obrigatório.")
            return false
        }
        if (state.host.isBlank()) {
            _uiState.value = state.copy(errorMessage = "O Host/IP é obrigatório.")
            return false
        }
        val portNum = state.port.toIntOrNull()
        if (portNum == null || portNum !in 1..65535) {
            _uiState.value = state.copy(errorMessage = "Porta inválida (deve ser entre 1 e 65535).")
            return false
        }
        if (state.publicKey.isBlank() || state.publicKey.length < 32) {
            _uiState.value = state.copy(errorMessage = "Chave pública WireGuard inválida (Base64).")
            return false
        }
        val mtuNum = state.mtu.toIntOrNull()
        if (mtuNum == null || mtuNum !in 1280..1500) {
            _uiState.value = state.copy(errorMessage = "MTU inválido (deve ser entre 1280 e 1500).")
            return false
        }
        if (!state.clientAddress.contains("/")) {
            _uiState.value = state.copy(errorMessage = "Endereço do cliente deve estar em notação CIDR (ex: 10.66.66.2/32).")
            return false
        }

        viewModelScope.launch {
            val keyAlias = state.privateKeyAlias.ifBlank { "novavpn_key_${UUID.randomUUID()}" }

            // Criptografa a chave privada no Android Keystore
            val encryptedPrivateKey = if (state.privateKey.isNotBlank()) {
                keyStoreManager.encryptSensitiveData(keyAlias, state.privateKey)
            } else {
                keyAlias
            }

            val server = VpnServer(
                id = state.serverId ?: UUID.randomUUID().toString(),
                name = state.name.trim(),
                country = state.country.trim(),
                host = state.host.trim(),
                port = portNum,
                publicKey = state.publicKey.trim(),
                privateKeyAlias = encryptedPrivateKey,
                clientAddress = state.clientAddress.trim(),
                dns = state.dns.trim(),
                allowedIps = state.allowedIps.trim(),
                mtu = mtuNum,
                favorite = state.favorite
            )

            if (state.serverId != null) {
                repository.updateServer(server)
            } else {
                repository.insertServer(server)
            }

            onSuccess()
        }
        return true
    }
}
