package com.novavpn.app.vpn

import android.os.ParcelFileDescriptor
import com.novavpn.app.domain.model.VpnServer

/**
 * Interface que desacopla o transporte de dados (WireGuard, OpenVPN, etc.)
 * da infraestrutura do VpnService do Android.
 */
interface VpnTunnelBackend {
    /**
     * Inicia o transporte do túnel sobre o descritor de arquivo TUN do Android.
     */
    suspend fun startTunnel(
        server: VpnServer,
        decryptedPrivateKey: String,
        tunInterface: ParcelFileDescriptor
    )

    /**
     * Interrompe o túnel e encerra conexões ativas.
     */
    suspend fun stopTunnel()

    /**
     * Retorna estatísticas de tráfego (bytes recebidos, bytes enviados)
     */
    fun getTrafficStats(): Pair<Long, Long>
}
