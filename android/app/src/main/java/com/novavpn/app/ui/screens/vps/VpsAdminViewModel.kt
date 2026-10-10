package com.novavpn.app.ui.screens.vps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novavpn.app.NovaVpnApplication
import com.novavpn.app.domain.model.SshAuthType
import com.novavpn.app.domain.model.VpsServerConfig
import com.novavpn.app.vps.installer.InstallStep
import com.novavpn.app.vps.installer.VpsAutoInstaller
import com.novavpn.app.vps.ssh.PortStatus
import com.novavpn.app.vps.ssh.SshClientManager
import com.novavpn.app.vps.ssh.VpsServiceStatus
import com.novavpn.app.vps.ssh.VpsSystemMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

enum class VpsAdminTab {
    DASHBOARD,
    CONFIG_FORM,
    AUTO_INSTALL,
    SERVICES,
    PORTS_FIREWALL,
    LOGS_TERMINAL,
    DIAGNOSTICS,
    SECURITY
}

data class VpsAdminUiState(
    val activeTab: VpsAdminTab = VpsAdminTab.DASHBOARD,
    val configs: List<VpsServerConfig> = emptyList(),
    val selectedConfig: VpsServerConfig? = null,
    val isConnecting: Boolean = false,
    val connectionTestResult: String? = null,
    val metrics: VpsSystemMetrics = VpsSystemMetrics(),
    val services: List<VpsServiceStatus> = emptyList(),
    val ports: List<PortStatus> = emptyList(),
    val terminalOutput: String = "Terminal pronto para comandos administrativos autorizados.\n",
    val isExecutingCommand: Boolean = false,
    val diagnosticResult: String = "",
    val installSteps: List<InstallStep> = emptyList(),
    val isInstalling: Boolean = false,
    val installerLog: String = "",
    val pendingActionConfirmDialog: Pair<String, () -> Unit>? = null
)

class VpsAdminViewModel : ViewModel() {

    private val app = NovaVpnApplication.instance
    private val vpsRepo = app.vpsRepository
    private val keyStore = app.keyStoreManager

    private val _uiState = MutableStateFlow(VpsAdminUiState())
    val uiState: StateFlow<VpsAdminUiState> = _uiState.asStateFlow()

    private var autoInstaller: VpsAutoInstaller? = null

    init {
        viewModelScope.launch {
            vpsRepo.getAllConfigs().collect { list ->
                val currentSelected = _uiState.value.selectedConfig
                val selected = if (currentSelected != null) {
                    list.find { it.id == currentSelected.id } ?: list.firstOrNull()
                } else {
                    list.firstOrNull()
                }

                _uiState.value = _uiState.value.copy(
                    configs = list,
                    selectedConfig = selected
                )
            }
        }
    }

    fun selectTab(tab: VpsAdminTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
        if (tab == VpsAdminTab.DASHBOARD || tab == VpsAdminTab.SERVICES || tab == VpsAdminTab.PORTS_FIREWALL) {
            refreshVpsData()
        }
    }

    fun selectConfig(config: VpsServerConfig) {
        _uiState.value = _uiState.value.copy(
            selectedConfig = config,
            connectionTestResult = null,
            terminalOutput = "Conectado ao perfil VPS: ${config.name} (${config.host})\n"
        )
        refreshVpsData()
    }

    private fun getSshManager(): SshClientManager? {
        val config = _uiState.value.selectedConfig ?: return null
        val secret = if (config.authType == SshAuthType.PASSWORD) {
            val alias = config.passwordKeyAlias ?: return null
            try { keyStore.decryptSensitiveData(alias, alias) } catch (e: Exception) { "" }
        } else {
            val alias = config.privateKeyAlias ?: return null
            try { keyStore.decryptSensitiveData(alias, alias) } catch (e: Exception) { "" }
        }
        return SshClientManager(config, secret)
    }

    fun testSshConnection() {
        val manager = getSshManager() ?: run {
            _uiState.value = _uiState.value.copy(connectionTestResult = "Credenciais não configuradas ou ausentes no KeyStore.")
            return
        }

        _uiState.value = _uiState.value.copy(isConnecting = true, connectionTestResult = null)
        viewModelScope.launch {
            val res = manager.testConnection()
            _uiState.value = _uiState.value.copy(
                isConnecting = false,
                connectionTestResult = res.fold(
                    onSuccess = { it },
                    onFailure = { it.message ?: "Erro desconhecido" }
                )
            )
        }
    }

    fun refreshVpsData() {
        val manager = getSshManager() ?: return
        viewModelScope.launch {
            val metrics = manager.getSystemMetrics()
            val services = manager.checkVpnServices()
            val targetPorts = listOf(22, 80, 443, 1194, 51820)
            val ports = manager.checkPorts(targetPorts)

            _uiState.value = _uiState.value.copy(
                metrics = metrics,
                services = services,
                ports = ports
            )
        }
    }

    fun executeTerminalCommand(cmd: String) {
        val manager = getSshManager() ?: run {
            _uiState.value = _uiState.value.copy(terminalOutput = _uiState.value.terminalOutput + "\n[ERRO] VPS não conectada.\n")
            return
        }

        _uiState.value = _uiState.value.copy(isExecutingCommand = true)
        viewModelScope.launch {
            val res = manager.executeCommand(cmd)
            val out = "\n$ ${cmd}\n" + (if (res.stdout.isNotBlank()) res.stdout else res.stderr)
            _uiState.value = _uiState.value.copy(
                terminalOutput = _uiState.value.terminalOutput + out,
                isExecutingCommand = false
            )
        }
    }

    fun requestServiceAction(serviceName: String, action: String) {
        _uiState.value = _uiState.value.copy(
            pendingActionConfirmDialog = Pair("Deseja realmente executar '$action' no serviço '$serviceName'?") {
                executeServiceAction(serviceName, action)
            }
        )
    }

    private fun executeServiceAction(serviceName: String, action: String) {
        val manager = getSshManager() ?: return
        viewModelScope.launch {
            manager.manageService(serviceName, action)
            refreshVpsData()
        }
    }

    fun startAutoInstaller() {
        val config = _uiState.value.selectedConfig ?: return
        val manager = getSshManager() ?: return

        _uiState.value = _uiState.value.copy(
            pendingActionConfirmDialog = Pair("Confirma o início da instalação automática do servidor VPN na VPS '${config.host}'? Esta operação irá configurar pacotes, firewall e serviços.") {
                runInstallerInternal(config, manager)
            }
        )
    }

    private fun runInstallerInternal(config: VpsServerConfig, manager: SshClientManager) {
        val installer = VpsAutoInstaller(config, manager)
        autoInstaller = installer

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInstalling = true)
            launch {
                installer.steps.collect { st ->
                    _uiState.value = _uiState.value.copy(installSteps = st)
                }
            }
            launch {
                installer.currentLog.collect { lg ->
                    _uiState.value = _uiState.value.copy(installerLog = lg)
                }
            }
            installer.runInstallation()
            _uiState.value = _uiState.value.copy(isInstalling = false)
            refreshVpsData()
        }
    }

    fun saveVpsConfig(
        name: String,
        host: String,
        sshPort: Int,
        sshUser: String,
        authType: SshAuthType,
        secretInput: String,
        tlsPort: Int,
        sni: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val id = _uiState.value.selectedConfig?.id ?: UUID.randomUUID().toString()
            val keyAlias = "vps_${id}_secret"

            if (secretInput.isNotBlank()) {
                keyStore.encryptSensitiveData(keyAlias, secretInput)
            }

            val newConfig = VpsServerConfig(
                id = id,
                name = name.ifBlank { "VPS $host" },
                host = host.trim(),
                sshPort = sshPort,
                sshUser = sshUser.trim().ifBlank { "root" },
                authType = authType,
                passwordKeyAlias = if (authType == SshAuthType.PASSWORD) keyAlias else null,
                privateKeyAlias = if (authType == SshAuthType.PRIVATE_KEY) keyAlias else null,
                tlsTransportPort = tlsPort,
                sniHostname = sni.trim()
            )

            vpsRepo.insertConfig(newConfig)
            _uiState.value = _uiState.value.copy(selectedConfig = newConfig)
            onSuccess()
        }
    }

    fun dismissConfirmDialog() {
        _uiState.value = _uiState.value.copy(pendingActionConfirmDialog = null)
    }

    fun runDiagnostics() {
        val config = _uiState.value.selectedConfig ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val report = StringBuilder()
            report.append("=== DIAGNÓSTICO DE CONEXÃO & REDE ===\n")

            // 1. Resolução DNS
            val dnsStart = System.currentTimeMillis()
            try {
                val addresses = java.net.InetAddress.getAllByName(config.host)
                val dnsTime = System.currentTimeMillis() - dnsStart
                report.append("✔ Resolução DNS (${dnsTime}ms): ${addresses.joinToString { it.hostAddress }}\n")
            } catch (e: Exception) {
                report.append("✖ Falha na resolução DNS: ${e.message}\n")
            }

            // 2. Teste de Socket TCP na porta SSH
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(config.host, config.sshPort), 3000)
                    report.append("✔ Porta SSH (${config.sshPort}): Aberta e acessível.\n")
                }
            } catch (e: Exception) {
                report.append("✖ Porta SSH (${config.sshPort}): Inacessível (${e.message})\n")
            }

            // 3. Teste de Socket na porta TLS
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(config.host, config.tlsTransportPort), 3000)
                    report.append("✔ Porta TLS Transport (${config.tlsTransportPort}): Aberta e acessível.\n")
                }
            } catch (e: Exception) {
                report.append("✖ Porta TLS Transport (${config.tlsTransportPort}): Fechada ou firewall bloqueando (${e.message})\n")
            }

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(diagnosticResult = report.toString())
            }
        }
    }
}
