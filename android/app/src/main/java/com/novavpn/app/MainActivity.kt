package com.novavpn.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.novavpn.app.navigation.Screen
import com.novavpn.app.navigation.bottomNavItems
import com.novavpn.app.ui.components.NovaBottomBar
import com.novavpn.app.ui.screens.dashboard.DashboardScreen
import com.novavpn.app.ui.screens.editor.ServerEditorScreen
import com.novavpn.app.ui.screens.servers.ServersScreen
import com.novavpn.app.ui.screens.settings.SettingsScreen
import com.novavpn.app.ui.theme.NovaVpnTheme
import com.novavpn.app.vpn.VpnManager

class MainActivity : ComponentActivity() {

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Permissão concedida pelo usuário no diálogo oficial do Android
            VpnManager.startVpnService(this)
        } else {
            Toast.makeText(this, "Permissão de VPN recusada pelo usuário.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val app = application as NovaVpnApplication
            val themeMode by app.preferencesDataStore.themeModeFlow.collectAsState(initial = "SYSTEM")

            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            NovaVpnTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NovaVpnMainApp(
                        onPrepareVpn = { prepareVpn() }
                    )
                }
            }
        }
    }

    /**
     * Solicita autorização oficial do sistema operacional usando VpnService.prepare()
     */
    fun prepareVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            // Sistema operacional exige confirmação do usuário
            vpnPrepareLauncher.launch(intent)
        } else {
            // Permissão já concedida anteriormente
            VpnManager.startVpnService(this)
        }
    }
}

@Composable
fun NovaVpnMainApp(
    onPrepareVpn: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Ocultar a barra inferior de navegação na tela de edição de servidor
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NovaBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToServers = { navController.navigate(Screen.Servers.route) },
                    onConnectClicked = onPrepareVpn
                )
            }

            composable(Screen.Servers.route) {
                ServersScreen(
                    onAddServer = { navController.navigate(Screen.ServerEditor.createRoute()) },
                    onEditServer = { serverId ->
                        navController.navigate(Screen.ServerEditor.createRoute(serverId))
                    }
                )
            }

            composable(Screen.VpsAdmin.route) {
                com.novavpn.app.ui.screens.vps.VpsAdminScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.ServerEditor.route,
                arguments = listOf(
                    navArgument("serverId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getString("serverId")
                ServerEditorScreen(
                    serverId = serverId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
