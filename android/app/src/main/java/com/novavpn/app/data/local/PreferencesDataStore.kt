package com.novavpn.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "novavpn_preferences")

class PreferencesDataStore(private val context: Context) {

    companion object {
        val SELECTED_SERVER_ID = stringPreferencesKey("selected_server_id")
        val THEME_MODE = stringPreferencesKey("theme_mode") // SYSTEM, LIGHT, DARK
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val KILL_SWITCH = booleanPreferencesKey("kill_switch")
        val SPLIT_TUNNELING = booleanPreferencesKey("split_tunneling")
        val CUSTOM_DNS = stringPreferencesKey("custom_dns")
        val SHOW_DIAGNOSTICS = booleanPreferencesKey("show_diagnostics")
    }

    val selectedServerIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_SERVER_ID]
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "SYSTEM"
    }

    val autoConnectFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_CONNECT] ?: false
    }

    val killSwitchFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KILL_SWITCH] ?: false
    }

    val splitTunnelingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SPLIT_TUNNELING] ?: false
    }

    val customDnsFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_DNS] ?: "1.1.1.1"
    }

    val showDiagnosticsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_DIAGNOSTICS] ?: true
    }

    suspend fun setSelectedServerId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_SERVER_ID] = id
        }
    }

    suspend fun setThemeMode(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = theme
        }
    }

    suspend fun setAutoConnect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CONNECT] = enabled
        }
    }

    suspend fun setKillSwitch(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KILL_SWITCH] = enabled
        }
    }

    suspend fun setSplitTunneling(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SPLIT_TUNNELING] = enabled
        }
    }

    suspend fun setCustomDns(dns: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_DNS] = dns
        }
    }

    suspend fun setShowDiagnostics(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_DIAGNOSTICS] = enabled
        }
    }
}
