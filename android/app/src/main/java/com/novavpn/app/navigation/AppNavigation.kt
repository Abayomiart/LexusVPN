package com.novavpn.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "Início",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security
    )

    data object Servers : Screen(
        route = "servers",
        title = "Servidores",
        selectedIcon = Icons.Filled.Dns,
        unselectedIcon = Icons.Outlined.Dns
    )

    data object VpsAdmin : Screen(
        route = "vps_admin",
        title = "VPS Admin",
        selectedIcon = Icons.Filled.Storage,
        unselectedIcon = Icons.Outlined.Storage
    )

    data object Settings : Screen(
        route = "settings",
        title = "Ajustes",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object ServerEditor : Screen(
        route = "server_editor?serverId={serverId}",
        title = "Editor de Servidor",
        selectedIcon = Icons.Filled.Dns,
        unselectedIcon = Icons.Outlined.Dns
    ) {
        fun createRoute(serverId: String? = null): String {
            return if (serverId != null) "server_editor?serverId=$serverId" else "server_editor"
        }
    }
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Servers,
    Screen.VpsAdmin,
    Screen.Settings
)
