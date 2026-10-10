package com.novavpn.app.domain.model

enum class SshAuthType {
    PASSWORD,
    PRIVATE_KEY
}

data class VpsServerConfig(
    val id: String,
    val name: String,
    val host: String,
    val sshPort: Int = 22,
    val sshUser: String = "root",
    val authType: SshAuthType = SshAuthType.PASSWORD,
    val passwordKeyAlias: String? = null,
    val privateKeyAlias: String? = null,
    val tlsTransportPort: Int = 443,
    val sniHostname: String = "",
    val tcpUdpPorts: String = "80,443,1194,51820",
    val primaryDns: String = "1.1.1.1",
    val secondaryDns: String = "1.0.0.1",
    val mtu: Int = 1420,
    val ipv4VirtualSubnet: String = "10.66.77.0/24",
    val ipv6VirtualSubnet: String = "fd00:nova:vpn::/64",
    val vpnRoutes: String = "0.0.0.0/0, ::/0",
    val reconnectIntervalSeconds: Int = 5,
    val connectionTimeoutSeconds: Int = 15,
    val trustedHostKeyFingerprint: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
