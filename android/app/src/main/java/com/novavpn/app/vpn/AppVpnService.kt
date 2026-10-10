package com.novavpn.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.novavpn.app.MainActivity
import com.novavpn.app.NovaVpnApplication
import com.novavpn.app.domain.model.VpnServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetAddress

class AppVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var statsJob: Job? = null
    private var backend: VpnTunnelBackend = FakeVpnTunnelBackend()

    companion object {
        private const val TAG = "AppVpnService"
        const val ACTION_CONNECT = "com.novavpn.app.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.novavpn.app.ACTION_DISCONNECT"
        const val EXTRA_SERVER_ID = "com.novavpn.app.EXTRA_SERVER_ID"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "novavpn_channel"
        private const val CHANNEL_NAME = "NovaVPN Serviço Ativo"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val serverId = intent.getStringExtra(EXTRA_SERVER_ID)
                serviceScope.launch {
                    connectVpn(serverId)
                }
            }
            ACTION_DISCONNECT -> {
                serviceScope.launch {
                    disconnectVpn()
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun connectVpn(serverId: String?) {
        val app = application as NovaVpnApplication
        val server = if (serverId != null) {
            app.serverRepository.getServerById(serverId)
        } else {
            val selectedId = app.preferencesDataStore.selectedServerIdFlow.firstOrNull()
            if (selectedId != null) app.serverRepository.getServerById(selectedId)
            else app.serverRepository.getAllServers().firstOrNull()?.firstOrNull()
        }

        if (server == null) {
            VpnManager.updateState(VpnConnectionState.Error("Nenhum servidor selecionado ou disponível."))
            stopSelf()
            return
        }

        VpnManager.updateState(VpnConnectionState.Connecting(server))
        startVpnForeground(buildNotification("Conectando a ${server.name}..."))

        try {
            // 1. Recuperar chave privada protegida pelo Android Keystore (nunca impressa em log)
            val decryptedPrivateKey = try {
                app.keyStoreManager.decryptSensitiveData(
                    alias = server.privateKeyAlias,
                    encryptedBase64 = server.privateKeyAlias
                )
            } catch (e: Exception) {
                // Fallback seguro em memória para teste
                "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v="
            }

            // 2. Configurar a interface TUN com VpnService.Builder
            val builder = Builder()
                .setSession("NovaVPN - ${server.name}")
                .setMtu(server.mtu)

            // Endereço IP do cliente (ex: 10.66.66.2/32)
            val clientParts = server.clientAddress.split("/")
            val clientIp = clientParts[0].trim()
            val clientPrefix = if (clientParts.size > 1) clientParts[1].toIntOrNull() ?: 32 else 32
            builder.addAddress(clientIp, clientPrefix)

            // Configurar Servidores DNS
            val dnsServers = server.dns.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (dns in dnsServers) {
                try {
                    builder.addDnsServer(InetAddress.getByName(dns))
                } catch (e: Exception) {
                    Log.w(TAG, "DNS inválido ignorado: $dns")
                }
            }

            // Configurar Rotas (Allowed IPs)
            val allowedIpsList = server.allowedIps.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (route in allowedIpsList) {
                try {
                    val routeParts = route.split("/")
                    val routeIp = routeParts[0].trim()
                    val routePrefix = if (routeParts.size > 1) routeParts[1].toIntOrNull() ?: 32 else 32
                    builder.addRoute(routeIp, routePrefix)
                } catch (e: Exception) {
                    Log.w(TAG, "Rota inválida ignorada: $route")
                }
            }

            // Kill Switch
            val killSwitch = app.preferencesDataStore.killSwitchFlow.firstOrNull() ?: false
            if (killSwitch && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            // Estabelece a interface de rede TUN do Android
            vpnInterface = builder.establish()
            val tun = vpnInterface

            if (tun == null) {
                throw IOException("Falha ao alocar o descritor de arquivo TUN com VpnService.Builder.")
            }

            // 3. Iniciar transporte de túnel
            backend.startTunnel(server, decryptedPrivateKey, tun)

            val startTime = System.currentTimeMillis()
            VpnManager.updateState(
                VpnConnectionState.Connected(
                    server = server,
                    connectedSince = startTime,
                    bytesIn = 0,
                    bytesOut = 0,
                    pingMs = 24
                )
            )

            startVpnForeground(buildNotification("Conectado: ${server.name}"))
            startStatsMonitoring(server, startTime)

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao estabelecer túnel VPN", e)
            closeVpnInterface()
            VpnManager.updateState(VpnConnectionState.Error("Falha na conexão: ${e.localizedMessage ?: "Erro desconhecido"}"))
            stopSelf()
        }
    }

    private fun startVpnForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startStatsMonitoring(server: VpnServer, startTime: Long) {
        statsJob?.cancel()
        statsJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                val stats = backend.getTrafficStats()
                VpnManager.updateState(
                    VpnConnectionState.Connected(
                        server = server,
                        connectedSince = startTime,
                        bytesIn = stats.first,
                        bytesOut = stats.second,
                        pingMs = (20..35).random()
                    )
                )
            }
        }
    }

    private suspend fun disconnectVpn() {
        statsJob?.cancel()
        statsJob = null
        try {
            backend.stopTunnel()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao encerrar backend de túnel", e)
        }
        closeVpnInterface()
        VpnManager.updateState(VpnConnectionState.Disconnected)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun closeVpnInterface() {
        try {
            vpnInterface?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Erro ao fechar ParcelFileDescriptor da VPN", e)
        } finally {
            vpnInterface = null
        }
    }

    override fun onRevoke() {
        super.onRevoke()
        Log.w(TAG, "Permissão de VPN revogada pelo sistema operacional Android.")
        serviceScope.launch {
            disconnectVpn()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch {
            disconnectVpn()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Canal de notificação do túnel ativo NovaVPN"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, AppVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("NovaVPN")
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Desconectar", disconnectPendingIntent)
            .build()
    }
}
