package com.novavpn.app

import android.app.Application
import com.novavpn.app.data.local.NovaVpnDatabase
import com.novavpn.app.data.local.PreferencesDataStore
import com.novavpn.app.data.repository.VpnServerRepositoryImpl
import com.novavpn.app.domain.repository.VpnServerRepository
import com.novavpn.app.security.KeyStoreManager
import com.novavpn.app.vpn.VpnManager

class NovaVpnApplication : Application() {

    val database by lazy { NovaVpnDatabase.getDatabase(this) }
    val preferencesDataStore by lazy { PreferencesDataStore(this) }
    val keyStoreManager by lazy { KeyStoreManager() }
    val serverRepository: VpnServerRepository by lazy {
        VpnServerRepositoryImpl(database.vpnServerDao())
    }
    val vpsRepository: com.novavpn.app.domain.repository.VpsConfigRepository by lazy {
        com.novavpn.app.data.repository.VpsConfigRepositoryImpl(database.vpsConfigDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Inicializa o gerenciador da VPN com o repositório e configurações
        VpnManager.init(
            context = this,
            serverRepository = serverRepository,
            preferencesDataStore = preferencesDataStore,
            keyStoreManager = keyStoreManager
        )
    }

    companion object {
        lateinit var instance: NovaVpnApplication
            private set
    }
}
