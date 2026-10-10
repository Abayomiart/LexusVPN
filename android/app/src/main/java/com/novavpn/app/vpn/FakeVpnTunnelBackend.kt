package com.novavpn.app.vpn

import android.os.ParcelFileDescriptor
import android.util.Log
import com.novavpn.app.domain.model.VpnServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Backend de testes/simulação controlada para validar o ciclo de vida do VpnService.
 *
 * AVISO: Não realiza criptografia nem encaminhamento de pacotes reais.
 * TODO: Substituir por 'com.wireguard.android.backend.GoBackend' ou 'WireGuardTunnel' oficial
 * quando a biblioteca oficial wireguard-android for adicionada ao build.gradle.
 */
class FakeVpnTunnelBackend : VpnTunnelBackend {

    private val tag = "FakeVpnTunnelBackend"
    private var job: Job? = null
    private val bytesIn = AtomicLong(0)
    private val bytesOut = AtomicLong(0)

    override suspend fun startTunnel(
        server: VpnServer,
        decryptedPrivateKey: String,
        tunInterface: ParcelFileDescriptor
    ) {
        Log.i(tag, "Iniciando túnel simulado para o servidor: ${server.name} (${server.host}:${server.port})")
        // NUNCA logar a chave privada!
        Log.i(tag, "Chave pública do peer: ${server.publicKey}")

        bytesIn.set(0)
        bytesOut.set(0)

        // TODO: Para backend oficial WireGuard:
        // val config = Config.Builder()
        //     .setInterface(Interface.Builder().parsePrivateKey(decryptedPrivateKey).addAddress(InetNetwork.parse(server.clientAddress)).build())
        //     .addPeer(Peer.Builder().parsePublicKey(server.publicKey).setEndpoint(InetSocketAddress(server.host, server.port)).parseAllowedIPs(server.allowedIps).build())
        //     .build()
        // wireguardBackend.setState(tunnel, Tunnel.State.UP, config)

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isActive) {
                    delay(1000)
                    // Simulação controlada de contadores de pacote para teste de interface
                    bytesIn.addAndGet((15000..85000).random().toLong())
                    bytesOut.addAndGet((8000..45000).random().toLong())
                }
            } catch (e: CancellationException) {
                Log.d(tag, "Loop de simulação do backend cancelado.")
            }
        }
    }

    override suspend fun stopTunnel() {
        Log.i(tag, "Encerrando túnel do backend.")
        job?.cancel()
        job = null
    }

    override fun getTrafficStats(): Pair<Long, Long> {
        return Pair(bytesIn.get(), bytesOut.get())
    }
}
