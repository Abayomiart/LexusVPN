package com.novavpn.app.vpn.transport

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 * Canal de pacotes bidirecional estabelecido pelo motor de transporte.
 */
data class VpnPacketChannel(
    val socket: Socket,
    val inputStream: InputStream,
    val outputStream: OutputStream
)

/**
 * Interface para os motores de transporte da VPN (SSH + TLS, WireGuard, etc.)
 */
interface VpnTransportEngine {
    /**
     * Estabelece o túnel criptografado com o servidor remoto.
     * @param protectSocket Lambda que invoca VpnService.protect(socket) para evitar loop de roteamento recursivo.
     */
    suspend fun connect(
        protectSocket: (Socket) -> Boolean
    ): VpnPacketChannel

    /**
     * Encerra o túnel e libera sockets e streams.
     */
    fun disconnect()

    /**
     * Retorna se o túnel de transporte está ativo e operante.
     */
    fun isConnected(): Boolean
}
