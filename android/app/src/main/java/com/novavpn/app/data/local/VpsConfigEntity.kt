package com.novavpn.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.novavpn.app.domain.model.SshAuthType
import com.novavpn.app.domain.model.VpsServerConfig

@Entity(tableName = "vps_configurations")
data class VpsConfigEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val host: String,
    val sshPort: Int,
    val sshUser: String,
    val authType: String, // PASSWORD or PRIVATE_KEY
    val passwordKeyAlias: String?,
    val privateKeyAlias: String?,
    val tlsTransportPort: Int,
    val sniHostname: String,
    val tcpUdpPorts: String,
    val primaryDns: String,
    val secondaryDns: String,
    val mtu: Int,
    val ipv4VirtualSubnet: String,
    val ipv6VirtualSubnet: String,
    val vpnRoutes: String,
    val reconnectIntervalSeconds: Int,
    val connectionTimeoutSeconds: Int,
    val trustedHostKeyFingerprint: String?,
    val createdAt: Long
)

fun VpsConfigEntity.toDomain(): VpsServerConfig = VpsServerConfig(
    id = id,
    name = name,
    host = host,
    sshPort = sshPort,
    sshUser = sshUser,
    authType = if (authType == SshAuthType.PRIVATE_KEY.name) SshAuthType.PRIVATE_KEY else SshAuthType.PASSWORD,
    passwordKeyAlias = passwordKeyAlias,
    privateKeyAlias = privateKeyAlias,
    tlsTransportPort = tlsTransportPort,
    sniHostname = sniHostname,
    tcpUdpPorts = tcpUdpPorts,
    primaryDns = primaryDns,
    secondaryDns = secondaryDns,
    mtu = mtu,
    ipv4VirtualSubnet = ipv4VirtualSubnet,
    ipv6VirtualSubnet = ipv6VirtualSubnet,
    vpnRoutes = vpnRoutes,
    reconnectIntervalSeconds = reconnectIntervalSeconds,
    connectionTimeoutSeconds = connectionTimeoutSeconds,
    trustedHostKeyFingerprint = trustedHostKeyFingerprint,
    createdAt = createdAt
)

fun VpsServerConfig.toEntity(): VpsConfigEntity = VpsConfigEntity(
    id = id,
    name = name,
    host = host,
    sshPort = sshPort,
    sshUser = sshUser,
    authType = authType.name,
    passwordKeyAlias = passwordKeyAlias,
    privateKeyAlias = privateKeyAlias,
    tlsTransportPort = tlsTransportPort,
    sniHostname = sniHostname,
    tcpUdpPorts = tcpUdpPorts,
    primaryDns = primaryDns,
    secondaryDns = secondaryDns,
    mtu = mtu,
    ipv4VirtualSubnet = ipv4VirtualSubnet,
    ipv6VirtualSubnet = ipv6VirtualSubnet,
    vpnRoutes = vpnRoutes,
    reconnectIntervalSeconds = reconnectIntervalSeconds,
    connectionTimeoutSeconds = connectionTimeoutSeconds,
    trustedHostKeyFingerprint = trustedHostKeyFingerprint,
    createdAt = createdAt
)
