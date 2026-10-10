package com.novavpn.app.ui.screens.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novavpn.app.ui.components.NovaTopBar
import com.novavpn.app.ui.theme.CyanPrimary
import com.novavpn.app.ui.theme.CyanSecondary
import com.novavpn.app.ui.theme.DarkBackground
import com.novavpn.app.ui.theme.DarkSurface
import com.novavpn.app.ui.theme.StatusConnected
import com.novavpn.app.ui.theme.StatusConnecting
import com.novavpn.app.ui.theme.StatusDisconnected
import com.novavpn.app.ui.theme.StatusError
import com.novavpn.app.vpn.VpnConnectionState
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    onNavigateToServers: () -> Unit,
    onConnectClicked: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()

    var durationSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(connectionState) {
        if (connectionState is VpnConnectionState.Connected) {
            val start = (connectionState as VpnConnectionState.Connected).connectedSince
            while (true) {
                durationSeconds = (System.currentTimeMillis() - start) / 1000
                delay(1000)
            }
        } else {
            durationSeconds = 0L
        }
    }

    val statusTitle = when (connectionState) {
        is VpnConnectionState.Connected -> "Conectado"
        is VpnConnectionState.Connecting -> "Conectando..."
        is VpnConnectionState.Disconnected -> "Desconectado"
        is VpnConnectionState.Error -> "Erro de Conexão"
    }

    val statusColor by animateColorAsState(
        targetValue = when (connectionState) {
            is VpnConnectionState.Connected -> StatusConnected
            is VpnConnectionState.Connecting -> StatusConnecting
            is VpnConnectionState.Disconnected -> StatusDisconnected
            is VpnConnectionState.Error -> StatusError
        },
        label = "statusColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        NovaTopBar(title = "NEXUS VPN")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Seletor de Servidor Ativo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToServers() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertAlignment,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DarkBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Servidor",
                                tint = CyanPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = selectedServer?.name ?: "Nenhum servidor selecionado",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = selectedServer?.host ?: "Toque para escolher",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Trocar servidor",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Indicador Central e Botão Circular Gigante
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Rótulo de status
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusTitle.uppercase(),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Animação de pulso quando conectando ou conectado
                val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (connectionState is VpnConnectionState.Connecting) 1.15f else 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(240.dp)
                ) {
                    if (connectionState is VpnConnectionState.Connected || connectionState is VpnConnectionState.Connecting) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.12f))
                        )
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.2f))
                        )
                    }

                    // Botão Principal Circular
                    Box(
                        modifier = Modifier
                            .size(175.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        DarkSurface,
                                        if (connectionState is VpnConnectionState.Connected) Color(0xFF0F2F2F) else Color(0xFF1E293B)
                                    )
                                )
                            )
                            .border(3.dp, statusColor, CircleShape)
                            .clickable {
                                viewModel.toggleConnection(context, onConnectClicked)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (connectionState is VpnConnectionState.Connecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(60.dp),
                                color = CyanPrimary,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Conectar/Desconectar",
                                tint = statusColor,
                                modifier = Modifier.size(68.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Tempo decorrido de conexão
                val hours = durationSeconds / 3600
                val minutes = (durationSeconds % 3600) / 60
                val seconds = durationSeconds % 60
                val formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Tempo",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (connectionState is VpnConnectionState.Connected) formattedTime else "--:--:--",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            // Cards de Telemetria: IP, Ping, Download e Upload
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val connectedState = connectionState as? VpnConnectionState.Connected

                    MetricItem(
                        icon = Icons.Default.NetworkCheck,
                        label = "PING",
                        value = if (connectedState != null) "${connectedState.pingMs} ms" else "--",
                        tint = CyanPrimary
                    )

                    MetricItem(
                        icon = Icons.Default.CloudDownload,
                        label = "DOWNLOAD",
                        value = if (connectedState != null) formatBytes(connectedState.bytesIn) else "--",
                        tint = CyanSecondary
                    )

                    MetricItem(
                        icon = Icons.Default.CloudUpload,
                        label = "UPLOAD",
                        value = if (connectedState != null) formatBytes(connectedState.bytesOut) else "--",
                        tint = Color(0xFFA78BFA)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
