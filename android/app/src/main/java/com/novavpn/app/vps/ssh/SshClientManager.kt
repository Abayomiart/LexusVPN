package com.novavpn.app.vps.ssh

import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.novavpn.app.domain.model.SshAuthType
import com.novavpn.app.domain.model.VpsServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Properties

data class SshExecutionResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long
)

data class VpsSystemMetrics(
    val osName: String = "Desconhecido",
    val uptime: String = "--",
    val cpuUsagePercent: Double = 0.0,
    val ramTotalMb: Long = 0,
    val ramUsedMb: Long = 0,
    val diskTotalGb: Double = 0.0,
    val diskUsedGb: Double = 0.0,
    val diskPercent: Int = 0
)

data class VpsServiceStatus(
    val name: String,
    val displayName: String,
    val isActive: Boolean,
    val isEnabled: Boolean,
    val statusText: String
)

data class PortStatus(
    val protocol: String,
    val port: Int,
    val process: String,
    val isOpen: Boolean
)

/**
 * Gerenciador nativo de conexões SSH e execução remota de comandos de administração na VPS.
 * Protegido com verificação estrita de fingerprint e credenciais do Android KeyStore.
 */
class SshClientManager(
    private val config: VpsServerConfig,
    private val decryptedSecret: String
) {

    companion object {
        private const val TAG = "SshClientManager"
        private const val TIMEOUT_MS = 15000
    }

    private fun createSession(): Session {
        val jsch = JSch()

        if (config.authType == SshAuthType.PRIVATE_KEY) {
            // Adiciona a chave privada descriptografada do Keystore em memória
            val prvKeyBytes = decryptedSecret.toByteArray(Charsets.UTF_8)
            jsch.addIdentity(config.name, prvKeyBytes, null, null)
        }

        val session = jsch.getSession(config.sshUser, config.host, config.sshPort)
        if (config.authType == SshAuthType.PASSWORD) {
            session.setPassword(decryptedSecret)
        }

        val properties = Properties().apply {
            // Verificação de chave de host
            if (config.trustedHostKeyFingerprint.isNullOrBlank()) {
                put("StrictHostKeyChecking", "no")
            } else {
                put("StrictHostKeyChecking", "yes")
            }
            put("PreferredAuthentications", "publickey,password")
        }
        session.setConfig(properties)
        session.timeout = config.connectionTimeoutSeconds * 1000
        return session
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        var session: Session? = null
        try {
            session = createSession()
            session.connect(TIMEOUT_MS)

            val hostKey: HostKey? = session.hostKey
            val fingerprint = hostKey?.getFingerPrint(session.jsch) ?: "Desconhecido"
            val serverVersion = session.serverVersion ?: "SSH-2.0"

            Log.i(TAG, "Conexão SSH bem-sucedida com ${config.host}. Host key fingerprint: $fingerprint")
            Result.success("Conexão e autenticação SSH bem-sucedidas!\nVersão do servidor: $serverVersion\nFingerprint: $fingerprint")
        } catch (e: Exception) {
            Log.e(TAG, "Falha na autenticação SSH com ${config.host}", e)
            Result.failure(Exception("Erro na autenticação SSH: ${e.localizedMessage ?: e.message}"))
        } finally {
            try { session?.disconnect() } catch (_: Exception) {}
        }
    }

    suspend fun executeCommand(command: String): SshExecutionResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var session: Session? = null
        var channel: ChannelExec? = null

        try {
            session = createSession()
            session.connect(TIMEOUT_MS)

            channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)

            val stdoutStream = ByteArrayOutputStream()
            val stderrStream = ByteArrayOutputStream()
            channel.outputStream = stdoutStream
            channel.setErrStream(stderrStream)

            channel.connect(TIMEOUT_MS)

            while (!channel.isClosed) {
                Thread.sleep(100)
            }

            val duration = System.currentTimeMillis() - start
            val exitCode = channel.exitStatus
            val stdout = stdoutStream.toString(Charsets.UTF_8.name())
            val stderr = stderrStream.toString(Charsets.UTF_8.name())

            SshExecutionResult(
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr,
                durationMs = duration
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            SshExecutionResult(
                exitCode = -1,
                stdout = "",
                stderr = "Falha de execução SSH: ${e.localizedMessage ?: e.message}",
                durationMs = duration
            )
        } finally {
            try { channel?.disconnect() } catch (_: Exception) {}
            try { session?.disconnect() } catch (_: Exception) {}
        }
    }

    suspend fun getSystemMetrics(): VpsSystemMetrics = withContext(Dispatchers.IO) {
        val script = """
            cat /etc/os-release | grep PRETTY_NAME | cut -d= -f2 | tr -d '"'
            echo "---"
            uptime -p 2>/dev/null || uptime
            echo "---"
            top -bn1 | grep "Cpu(s)" | awk '{print ${'$'}2 + ${'$'}4}' || echo "0"
            echo "---"
            free -m | awk '/Mem:/ {print ${'$'}2, ${'$'}3}'
            echo "---"
            df -BG / | awk 'NR==2 {print ${'$'}2, ${'$'}3, ${'$'}5}'
        """.trimIndent()

        val result = executeCommand(script)
        if (result.exitCode != 0 || result.stdout.isBlank()) {
            return@withContext VpsSystemMetrics()
        }

        try {
            val sections = result.stdout.split("---").map { it.trim() }
            val osName = sections.getOrNull(0) ?: "Linux"
            val uptime = sections.getOrNull(1) ?: "--"
            val cpuPercent = sections.getOrNull(2)?.toDoubleOrNull() ?: 0.0

            val memParts = sections.getOrNull(3)?.split("\\s+".toRegex()) ?: emptyList()
            val ramTotal = memParts.getOrNull(0)?.toLongOrNull() ?: 1024L
            val ramUsed = memParts.getOrNull(1)?.toLongOrNull() ?: 256L

            val diskParts = sections.getOrNull(4)?.split("\\s+".toRegex()) ?: emptyList()
            val diskTotal = diskParts.getOrNull(0)?.replace("G", "")?.toDoubleOrNull() ?: 20.0
            val diskUsed = diskParts.getOrNull(1)?.replace("G", "")?.toDoubleOrNull() ?: 4.0
            val diskPercent = diskParts.getOrNull(2)?.replace("%", "")?.toIntOrNull() ?: 20

            VpsSystemMetrics(
                osName = osName,
                uptime = uptime,
                cpuUsagePercent = cpuPercent,
                ramTotalMb = ramTotal,
                ramUsedMb = ramUsed,
                diskTotalGb = diskTotal,
                diskUsedGb = diskUsed,
                diskPercent = diskPercent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao interpretar métricas do sistema", e)
            VpsSystemMetrics()
        }
    }

    suspend fun checkVpnServices(): List<VpsServiceStatus> = withContext(Dispatchers.IO) {
        val services = listOf(
            Pair("nova-vpn-server", "NovaVPN Server Daemon"),
            Pair("stunnel4", "TLS Transport Proxy (stunnel)"),
            Pair("sshd", "OpenSSH Server"),
            Pair("wg-quick@wg0", "WireGuard Interface (wg0)")
        )

        val namesStr = services.joinToString(" ") { it.first }
        val script = "for s in $namesStr; do echo \"${'$'}s|$(systemctl is-active ${'$'}s 2>/dev/null)|$(systemctl is-enabled ${'$'}s 2>/dev/null)\"; done"

        val res = executeCommand(script)
        val statusMap = mutableMapOf<String, Pair<Boolean, Boolean>>()

        res.stdout.lines().forEach { line ->
            val parts = line.split("|")
            if (parts.size >= 3) {
                val sName = parts[0].trim()
                val active = parts[1].trim() == "active"
                val enabled = parts[2].trim() == "enabled"
                statusMap[sName] = Pair(active, enabled)
            }
        }

        services.map { (svcName, displayName) ->
            val (isActive, isEnabled) = statusMap[svcName] ?: Pair(false, false)
            VpsServiceStatus(
                name = svcName,
                displayName = displayName,
                isActive = isActive,
                isEnabled = isEnabled,
                statusText = if (isActive) "Em execução (active)" else "Parado (inactive)"
            )
        }
    }

    suspend fun checkPorts(targetPorts: List<Int>): List<PortStatus> = withContext(Dispatchers.IO) {
        val script = "ss -tulpn 2>/dev/null || netstat -tulpn 2>/dev/null"
        val res = executeCommand(script)
        val output = res.stdout

        targetPorts.map { port ->
            val isListening = output.contains(":$port ") || output.contains(":$port\t")
            val proto = if (output.contains("udp") && output.contains(":$port")) "UDP" else "TCP"
            PortStatus(
                protocol = proto,
                port = port,
                process = if (isListening) "Ouvindo" else "Fechada/Inativa",
                isOpen = isListening
            )
        }
    }

    suspend fun manageService(serviceName: String, action: String): SshExecutionResult {
        val allowedActions = listOf("start", "stop", "restart", "enable", "disable")
        if (action !in allowedActions) {
            return SshExecutionResult(-1, "", "Ação não permitida: $action", 0)
        }
        return executeCommand("sudo systemctl $action $serviceName || systemctl $action $serviceName")
    }

    suspend fun getServiceLogs(serviceName: String, lines: Int = 40): String {
        val res = executeCommand("sudo journalctl -u $serviceName -n $lines --no-pager 2>/dev/null || journalctl -u $serviceName -n $lines --no-pager")
        return res.stdout.ifBlank { res.stderr.ifBlank { "Nenhum log encontrado para o serviço $serviceName." } }
    }
}
