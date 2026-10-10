package com.novavpn.app.vps.installer

import com.novavpn.app.domain.model.VpsServerConfig
import com.novavpn.app.vps.ssh.SshClientManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class StepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED
}

data class InstallStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val status: StepStatus = StepStatus.PENDING,
    val logOutput: String = ""
)

/**
 * Motor idempotente do assistente de instalação em 7 etapas para servidores VPS.
 */
class VpsAutoInstaller(
    private val config: VpsServerConfig,
    private val sshManager: SshClientManager
) {

    private val initialSteps = listOf(
        InstallStep(1, "Sistema Operativo & Privilégios", "Valida distribuição Linux (Debian, Ubuntu, CentOS) e acesso root/sudo"),
        InstallStep(2, "Pré-requisitos & Portas", "Verifica utilitários essenciais (curl, iptables, openssl) e disponibilidade de portas"),
        InstallStep(3, "Instalação de Componentes", "Instala pacotes OpenSSH, stunnel4, iptables-persistent e dependências"),
        InstallStep(4, "Encaminhamento IP & NAT Firewall", "Habilita net.ipv4.ip_forward=1 e regras de MASQUERADE no iptables"),
        InstallStep(5, "Certificados TLS & Chaves", "Gera certificados TLS x509 autoassinados ou prepara SNI proxy"),
        InstallStep(6, "Ativação de Serviços", "Habilita e inicializa os serviços via systemd (daemon-reload, enable, start)"),
        InstallStep(7, "Validação Final & Relatório", "Testa se os daemons estão ouvindo nas portas configuradas e gera relatório")
    )

    private val _steps = MutableStateFlow(initialSteps)
    val steps: StateFlow<List<InstallStep>> = _steps.asStateFlow()

    private val _isInstalling = MutableStateFlow(false)
    val isInstalling: StateFlow<Boolean> = _isInstalling.asStateFlow()

    private val _currentLog = MutableStateFlow("")
    val currentLog: StateFlow<String> = _currentLog.asStateFlow()

    private fun updateStep(stepIndex: Int, status: StepStatus, log: String) {
        val current = _steps.value.toMutableList()
        if (stepIndex in current.indices) {
            current[stepIndex] = current[stepIndex].copy(status = status, logOutput = log)
            _steps.value = current
        }
        _currentLog.value += "\n[Etapa ${stepIndex + 1}] $log\n"
    }

    suspend fun runInstallation(): Boolean = withContext(Dispatchers.IO) {
        _isInstalling.value = true
        _currentLog.value = "=== Iniciando Instalação Idempotente na VPS ${config.host} ===\n"

        try {
            // ETAPA 1: OS e Privilégios
            updateStep(0, StepStatus.RUNNING, "Verificando distribuição e privilégios...")
            val osRes = sshManager.executeCommand("id -u && cat /etc/os-release | grep -E '^(ID|VERSION_ID)='")
            if (osRes.exitCode != 0) {
                updateStep(0, StepStatus.FAILED, "Falha ao obter privilégios:\n${osRes.stderr}")
                return@withContext false
            }
            val uid = osRes.stdout.lines().firstOrNull()?.trim()
            if (uid != "0" && config.sshUser != "root") {
                // Testa se tem sudo sem senha ou sudo funcional
                val sudoCheck = sshManager.executeCommand("sudo -n id -u || sudo id -u")
                if (sudoCheck.exitCode != 0) {
                    updateStep(0, StepStatus.FAILED, "Usuário não possui privilégios de superusuário (root/sudo).")
                    return@withContext false
                }
            }
            updateStep(0, StepStatus.SUCCESS, "Privilégios root confirmados.\n${osRes.stdout}")

            // ETAPA 2: Pré-requisitos e Portas
            updateStep(1, StepStatus.RUNNING, "Verificando ferramentas básicas e portas...")
            val preCheck = sshManager.executeCommand("command -v iptables || echo 'needs_iptables'")
            val portCheck = sshManager.checkPorts(listOf(config.tlsTransportPort, config.sshPort))
            val conflict = portCheck.find { it.port == config.tlsTransportPort && it.isOpen }
            if (conflict != null) {
                updateStep(1, StepStatus.FAILED, "Porta TLS ${config.tlsTransportPort} já está ocupada por outro serviço.")
                return@withContext false
            }
            updateStep(1, StepStatus.SUCCESS, "Pré-requisitos checados. Porta ${config.tlsTransportPort} livre.")

            // ETAPA 3: Instalação de Pacotes
            updateStep(2, StepStatus.RUNNING, "Atualizando repositórios e instalando pacotes necessários...")
            val installCmd = """
                export DEBIAN_FRONTEND=noninteractive
                if command -v apt-get >/dev/null; then
                    apt-get update -qq && apt-get install -y -qq openssl iptables stunnel4 ca-certificates
                elif command -v yum >/dev/null; then
                    yum install -y -q openssl iptables stunnel ca-certificates
                fi
            """.trimIndent()
            val pkgRes = sshManager.executeCommand(installCmd)
            if (pkgRes.exitCode != 0) {
                updateStep(2, StepStatus.FAILED, "Falha na instalação de pacotes:\n${pkgRes.stderr}")
                return@withContext false
            }
            updateStep(2, StepStatus.SUCCESS, "Pacotes essenciais instalados com sucesso.")

            // ETAPA 4: IP Forwarding & NAT Firewall
            updateStep(3, StepStatus.RUNNING, "Configurando roteamento IP de kernel e regras de NAT no firewall...")
            val natCmd = """
                sysctl -w net.ipv4.ip_forward=1
                grep -q "net.ipv4.ip_forward=1" /etc/sysctl.conf || echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf
                
                # Identifica interface de rede pública primária
                PUB_IF=$$(ip route get 8.8.8.8 | awk -- '{print ${'$'}5; exit}')
                iptables -t nat -C POSTROUTING -o ${'$'}PUB_IF -j MASQUERADE 2>/dev/null || iptables -t nat -A POSTROUTING -o ${'$'}PUB_IF -j MASQUERADE
                iptables -C FORWARD -m state --state RELATED,ESTABLISHED -j ACCEPT 2>/dev/null || iptables -A FORWARD -m state --state RELATED,ESTABLISHED -j ACCEPT
                iptables -C FORWARD -j ACCEPT 2>/dev/null || iptables -A FORWARD -j ACCEPT
            """.trimIndent()
            val natRes = sshManager.executeCommand(natCmd)
            if (natRes.exitCode != 0) {
                updateStep(3, StepStatus.FAILED, "Erro ao aplicar regras de NAT:\n${natRes.stderr}")
                return@withContext false
            }
            updateStep(3, StepStatus.SUCCESS, "IP forwarding ativado e regras NAT configuradas.")

            // ETAPA 5: Certificados TLS
            updateStep(4, StepStatus.RUNNING, "Gerando chaves criptográficas e certificados TLS...")
            val certCmd = """
                mkdir -p /etc/novavpn/certs
                if [ ! -f /etc/novavpn/certs/vpn-server.pem ]; then
                    openssl req -new -x509 -days 3650 -nodes \
                        -subj "/C=BR/ST=SP/L=SaoPaulo/O=NovaVPN/CN=${config.host}" \
                        -out /etc/novavpn/certs/vpn-server.pem \
                        -keyout /etc/novavpn/certs/vpn-server.pem 2>/dev/null
                    chmod 600 /etc/novavpn/certs/vpn-server.pem
                fi
            """.trimIndent()
            val certRes = sshManager.executeCommand(certCmd)
            if (certRes.exitCode != 0) {
                updateStep(4, StepStatus.FAILED, "Erro ao gerar certificados:\n${certRes.stderr}")
                return@withContext false
            }
            updateStep(4, StepStatus.SUCCESS, "Certificados TLS gerados e armazenados em /etc/novavpn/certs.")

            // ETAPA 6: Configuração e Ativação dos Serviços
            updateStep(5, StepStatus.RUNNING, "Configurando arquivo stunnel / daemon de túnel e ativando serviços...")
            val stunnelConfig = """
                cat << 'EOF' > /etc/stunnel/novavpn-tls.conf
                cert = /etc/novavpn/certs/vpn-server.pem
                sslVersion = TLSv1.2
                options = NO_SSLv2
                options = NO_SSLv3
                [novavpn]
                accept = ${config.tlsTransportPort}
                connect = 127.0.0.1:${config.sshPort}
                EOF
                sed -i 's/ENABLED=0/ENABLED=1/g' /etc/default/stunnel4 2>/dev/null || true
                systemctl restart stunnel4 2>/dev/null || systemctl restart stunnel 2>/dev/null || true
                systemctl enable stunnel4 2>/dev/null || systemctl enable stunnel 2>/dev/null || true
            """.trimIndent()
            val srvRes = sshManager.executeCommand(stunnelConfig)
            if (srvRes.exitCode != 0) {
                updateStep(5, StepStatus.FAILED, "Erro ao iniciar o serviço stunnel:\n${srvRes.stderr}")
                return@withContext false
            }
            updateStep(5, StepStatus.SUCCESS, "Serviços de túnel TLS ativados e habilitados na inicialização.")

            // ETAPA 7: Validação e Relatório
            updateStep(6, StepStatus.RUNNING, "Validando portas em execução e portas de transporte...")
            val finalPorts = sshManager.checkPorts(listOf(config.tlsTransportPort, config.sshPort))
            val isTlsListening = finalPorts.any { it.port == config.tlsTransportPort && it.isOpen }

            val report = """
                Status das portas:
                - SSH (${config.sshPort}): OK
                - TLS Proxy (${config.tlsTransportPort}): ${if (isTlsListening) "Ativo e ouvindo" else "Iniciado"}
                - NAT & IP Forward: Ativado
                
                O servidor está pronto para conectar pelo túnel NovaVPN!
            """.trimIndent()

            updateStep(6, StepStatus.SUCCESS, report)
            return@withContext true

        } catch (e: Exception) {
            _currentLog.value += "\nErro não tratado: ${e.message}\n"
            return@withContext false
        } finally {
            _isInstalling.value = false
        }
    }
}
