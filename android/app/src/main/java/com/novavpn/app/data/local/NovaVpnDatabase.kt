package com.novavpn.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(entities = [VpnServerEntity::class, VpsConfigEntity::class], version = 2, exportSchema = false)
abstract class NovaVpnDatabase : RoomDatabase() {

    abstract fun vpnServerDao(): VpnServerDao
    abstract fun vpsConfigDao(): VpsConfigDao

    companion object {
        @Volatile
        private var INSTANCE: NovaVpnDatabase? = null

        fun getDatabase(context: Context): NovaVpnDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NovaVpnDatabase::class.java,
                    "novavpn_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Inserir servidores padrão para uso inicial seguro
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialServers(database.vpnServerDao())
                    }
                }
            }

            private suspend fun populateInitialServers(dao: VpnServerDao) {
                val defaultServers = listOf(
                    VpnServerEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Brasil - São Paulo Ultra",
                        country = "Brasil",
                        host = "br-sp.novavpn.net",
                        port = 51820,
                        publicKey = "4g8xR9kY2pZLm9w8aB7cD6eF5gH4iJ3kL2mN1oP0qRs=",
                        privateKeyAlias = "novavpn_key_br_sp",
                        clientAddress = "10.66.66.2/32",
                        dns = "1.1.1.1, 1.0.0.1",
                        allowedIps = "0.0.0.0/0, ::/0",
                        mtu = 1420,
                        favorite = true,
                        enabled = true,
                        createdAt = System.currentTimeMillis()
                    ),
                    VpnServerEntity(
                        id = UUID.randomUUID().toString(),
                        name = "EUA - Nova York Rápido",
                        country = "Estados Unidos",
                        host = "us-ny.novavpn.net",
                        port = 51820,
                        publicKey = "8h9yT0lZ3qAMn0x9bC8dE7fG6hI5jK4lM3nO2pQ1rSt=",
                        privateKeyAlias = "novavpn_key_us_ny",
                        clientAddress = "10.66.67.2/32",
                        dns = "1.1.1.1, 8.8.8.8",
                        allowedIps = "0.0.0.0/0, ::/0",
                        mtu = 1420,
                        favorite = false,
                        enabled = true,
                        createdAt = System.currentTimeMillis()
                    ),
                    VpnServerEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Alemanha - Frankfurt Secure",
                        country = "Alemanha",
                        host = "de-fra.novavpn.net",
                        port = 51820,
                        publicKey = "3f7wQ8jX1oYKl8v7aA6bC5dE4fG3hI2jK1lM0nO9pQr=",
                        privateKeyAlias = "novavpn_key_de_fra",
                        clientAddress = "10.66.68.2/32",
                        dns = "9.9.9.9, 149.112.112.112",
                        allowedIps = "0.0.0.0/0, ::/0",
                        mtu = 1420,
                        favorite = false,
                        enabled = true,
                        createdAt = System.currentTimeMillis()
                    )
                )
                defaultServers.forEach { dao.insertServer(it) }
            }
        }
    }
}
