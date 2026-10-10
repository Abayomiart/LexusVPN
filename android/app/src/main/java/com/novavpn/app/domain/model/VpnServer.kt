package com.novavpn.app.domain.model

data class VpnServer(
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
    val favorite: Boolean = false,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
