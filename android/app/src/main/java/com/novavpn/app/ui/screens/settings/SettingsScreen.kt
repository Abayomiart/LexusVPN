package com.novavpn.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novavpn.app.ui.components.NovaTopBar
import com.novavpn.app.ui.theme.CyanPrimary
import com.novavpn.app.ui.theme.DarkBackground
import com.novavpn.app.ui.theme.DarkSurface
import com.novavpn.app.ui.theme.DarkSurfaceVariant

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingDns by remember { mutableStateOf(false) }
    var tempDns by remember { mutableStateOf(uiState.customDns) }

    Scaffold(
        topBar = { NovaTopBar(title = "Configurações") },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seletor de Tema
            item {
                SectionHeader("Aparência & Tema")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, tint = CyanPrimary)
                            Spacer(modifier = Modifier.size(12.dp))
                            Text("Tema da Interface", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeOptionButton(
                                label = "Sistema",
                                isSelected = uiState.themeMode == "SYSTEM",
                                onClick = { viewModel.setThemeMode("SYSTEM") },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeOptionButton(
                                label = "Escuro",
                                isSelected = uiState.themeMode == "DARK",
                                onClick = { viewModel.setThemeMode("DARK") },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeOptionButton(
                                label = "Claro",
                                isSelected = uiState.themeMode == "LIGHT",
                                onClick = { viewModel.setThemeMode("LIGHT") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Segurança & Túnel
            item {
                SectionHeader("Segurança & Comportamento")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SettingToggleItem(
                            icon = Icons.Default.Autorenew,
                            title = "Conexão Automática",
                            description = "Conectar ao servidor favorito ao abrir o app",
                            checked = uiState.autoConnect,
                            onCheckedChange = { viewModel.setAutoConnect(it) }
                        )

                        HorizontalDivider(color = Color(0xFF1E293B))

                        SettingToggleItem(
                            icon = Icons.Default.Shield,
                            title = "Kill Switch",
                            description = "Bloquear conexões se o túnel VPN cair",
                            checked = uiState.killSwitch,
                            onCheckedChange = { viewModel.setKillSwitch(it) }
                        )

                        HorizontalDivider(color = Color(0xFF1E293B))

                        SettingToggleItem(
                            icon = Icons.Default.AltRoute,
                            title = "Split Tunneling",
                            description = "Permitir que apps selecionados contornem a VPN",
                            checked = uiState.splitTunneling,
                            onCheckedChange = { viewModel.setSplitTunneling(it) }
                        )
                    }
                }
            }

            // DNS Personalizado
            item {
                SectionHeader("Rede Avançada")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Dns, contentDescription = null, tint = CyanPrimary)
                                Spacer(modifier = Modifier.size(12.dp))
                                Column {
                                    Text("DNS Personalizado", fontWeight = FontWeight.SemiBold, color = Color.White)
                                    Text(uiState.customDns, fontSize = 13.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = tempDns,
                            onValueChange = {
                                tempDns = it
                                viewModel.setCustomDns(it)
                            },
                            placeholder = { Text("Ex: 1.1.1.1, 8.8.8.8", color = Color(0xFF64748B)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground,
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // Diagnósticos e Logs
            item {
                SectionHeader("Diagnóstico & Logs")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingToggleItem(
                            icon = Icons.Default.BugReport,
                            title = "Exibir Logs de Diagnóstico",
                            description = "Histórico de eventos do serviço VpnService em tempo real",
                            checked = uiState.showDiagnostics,
                            onCheckedChange = { viewModel.setShowDiagnostics(it) }
                        )

                        if (uiState.showDiagnostics) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF070A10))
                                    .padding(12.dp)
                            ) {
                                LazyColumn {
                                    items(uiState.diagnosticLogs) { logLine ->
                                        Text(
                                            text = logLine,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = if (logLine.contains("ERRO")) Color(0xFFEF4444) else Color(0xFF38BDF8),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF94A3B8),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun ThemeOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) CyanPrimary else DarkSurfaceVariant)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (isSelected) DarkBackground else Color.White
        )
    }
}

@Composable
private fun SettingToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                Text(text = description, color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkBackground,
                checkedTrackColor = CyanPrimary,
                uncheckedThumbColor = Color(0xFF64748B),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}
