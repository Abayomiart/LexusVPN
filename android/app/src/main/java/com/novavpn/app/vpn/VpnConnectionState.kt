package com.novavpn.app.vpn

import com.novavpn.app.domain.model.VpnServer

sealed class VpnConnectionState {
    data object Disconnected : VpnConnectionState()
    data class Connecting(val server: VpnServer) : VpnConnectionState()
    data class Connected(
        val server: VpnServer,
        val connectedSince: Long,
        val bytesIn: Long = 0,
        val bytesOut: Long = 0,
        val pingMs: Int = 28
    ) : VpnConnectionState()
    data class Error(val message: String) : VpnConnectionState()
}
