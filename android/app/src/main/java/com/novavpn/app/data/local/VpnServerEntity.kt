package com.novavpn.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.novavpn.app.domain.model.VpnServer

@Entity(tableName = "vpn_servers")
data class VpnServerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val country: String,
    val host: String,
    val port: Int,
    val publicKey: String,
    val privateKeyAlias: String,
    val clientAddress: String,
    val dns: String,
    val allowedIps: String,
    val mtu: Int,
    val favorite: Boolean,
    val enabled: Boolean,
    val createdAt: Long
)

fun VpnServerEntity.toDomain(): VpnServer = VpnServer(
    id = id,
    name = name,
    country = country,
    host = host,
    port = port,
    publicKey = publicKey,
    privateKeyAlias = privateKeyAlias,
    clientAddress = clientAddress,
    dns = dns,
    allowedIps = allowedIps,
    mtu = mtu,
    favorite = favorite,
    enabled = enabled,
    createdAt = createdAt
)

fun VpnServer.toEntity(): VpnServerEntity = VpnServerEntity(
    id = id,
    name = name,
    country = country,
    host = host,
    port = port,
    publicKey = publicKey,
    privateKeyAlias = privateKeyAlias,
    clientAddress = clientAddress,
    dns = dns,
    allowedIps = allowedIps,
    mtu = mtu,
    favorite = favorite,
    enabled = enabled,
    createdAt = createdAt
)
