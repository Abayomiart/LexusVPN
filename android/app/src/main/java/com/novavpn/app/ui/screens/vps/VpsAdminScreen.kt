package com.novavpn.app.ui.screens.vps

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import com.novavpn.app.domain.model.SshAuthType
import com.novavpn.app.ui.components.ConfirmationDialog
import com.novavpn.app.ui.components.NovaTopBar
import com.novavpn.app.ui.theme.CyanPrimary
import com.novavpn.app.ui.theme.DarkBackground
import com.novavpn.app.ui.theme.DarkSurface
import com.novavpn.app.ui.theme.DarkSurfaceVariant
import com.novavpn.app.ui.theme.StatusConnected
import com.novavpn.app.ui.theme.StatusError
import com.novavpn.app.vps.installer.StepStatus

@Composable
fun VpsAdminScreen(
    onNavigateBack: () -> Unit,
    viewModel: VpsAdminViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Diálogo de confirmação para ações de impacto
    uiState.pendingActionConfirmDialog?.let { (msg, action) ->
        ConfirmationDialog(
            title = "Confirmação Administrativa",
            message = msg,
            confirmButtonText = "Confirmar",
            dismissButtonText = "Cancelar",
            onConfirm = {
                action()
                viewModel.dismissConfirmDialog()
            },
            onDismiss = { viewModel.dismissConfirmDialog() }
        )
    }

    Scaffold(
        topBar = {
            NovaTopBar(
                title = "Painel VPS Admin",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshVpsData() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Atualizar", tint = CyanPrimary)
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra de Abas (8 Abas com estilo neon)
            VpsTabRow(
                activeTab = uiState.activeTab,
                onSelectTab = { viewModel.selectTab(it) }
            )

            // Conteúdo da Aba Ativa
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                when (uiState.activeTab) {
                    VpsAdminTab.DASHBOARD -> DashboardTab(uiState, viewModel)
                    VpsAdminTab.CONFIG_FORM -> ConfigFormTab(uiState, viewModel)
                    VpsAdminTab.AUTO_INSTALL -> AutoInstallTab(uiState, viewModel)
                    VpsAdminTab.SERVICES -> ServicesTab(uiState, viewModel)
                    VpsAdminTab.PORTS_FIREWALL -> PortsFirewallTab(uiState, viewModel)
                    VpsAdminTab.LOGS_TERMINAL -> LogsTerminalTab(uiState, viewModel)
                    VpsAdminTab.DIAGNOSTICS -> DiagnosticsTab(uiState, viewModel)
                    VpsAdminTab.SECURITY -> SecurityTab(uiState, viewModel)
                }
            }
        }
    }
}

@Composable
private fun VpsTabRow(
    activeTab: VpsAdminTab,
    onSelectTab: (VpsAdminTab) -> Unit
) {
    val tabs = listOf(
        Pair(VpsAdminTab.DASHBOARD, "Dashboard"),
        Pair(VpsAdminTab.CONFIG_FORM, "Configurar VPS"),
        Pair(VpsAdminTab.AUTO_INSTALL, "Instalação"),
        Pair(VpsAdminTab.SERVICES, "Serviços"),
        Pair(VpsAdminTab.PORTS_FIREWALL, "Portas & FW"),
        Pair(VpsAdminTab.LOGS_TERMINAL, "Terminal & Logs"),
        Pair(VpsAdminTab.DIAGNOSTICS, "Diagnóstico"),
        Pair(VpsAdminTab.SECURITY, "Segurança")
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs) { (tab, title) ->
            val isSelected = activeTab == tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) CyanPrimary else DarkSurfaceVariant)
                    .clickable { onSelectTab(tab) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) DarkBackground else Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun DashboardTab(uiState: VpsAdminUiState, viewModel: VpsAdminViewModel) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card do Servidor Selecionado
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = uiState.selectedConfig?.name ?: "Nenhuma VPS selecionada",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${uiState.selectedConfig?.sshUser ?: "root"}@${uiState.selectedConfig?.host ?: "0.0.0.0"}:${uiState.selectedConfig?.sshPort ?: 22}",
                            fontSize = 13.sp,
                            color = CyanPrimary
                        )
                    }
                    Button(
                        onClick = { viewModel.testSshConnection() },
                        enabled = !uiState.isConnecting && uiState.selectedConfig != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                    ) {
                        if (uiState.isConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Testar SSH", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (uiState.connectionTestResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = uiState.connectionTestResult ?: "",
                        fontSize = 12.sp,
                        color = if (uiState.connectionTestResult!!.contains("bem-sucedida")) StatusConnected else StatusError,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Métricas do Sistema
        Text("Utilização de Recursos da VPS", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                title = "CPU",
                value = "${uiState.metrics.cpuUsagePercent.toInt()}%",
                subtitle = "Carga média",
                color = CyanPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "RAM",
                value = "${uiState.metrics.ramUsedMb} MB",
                subtitle = "de ${uiState.metrics.ramTotalMb} MB",
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "DISCO",
                value = "${uiState.metrics.diskPercent}%",
                subtitle = "${uiState.metrics.diskUsedGb.toInt()}GB / ${uiState.metrics.diskTotalGb.toInt()}GB",
                color = Color(0xFFA855F7),
                modifier = Modifier.weight(1f)
            )
        }

        // Informações Adicionais
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Sistema Operacional:", uiState.metrics.osName)
                HorizontalDivider(color = Color(0xFF1E293B))
                InfoRow("Uptime:", uiState.metrics.uptime)
                HorizontalDivider(color = Color(0xFF1E293B))
                InfoRow("Porta TLS Transport:", "${uiState.selectedConfig?.tlsTransportPort ?: 443} (SNI: ${uiState.selectedConfig?.sniHostname?.ifBlank { "Padrão" } ?: "Padrão"})")
            }
        }
    }
}

@Composable
private fun ConfigFormTab(uiState: VpsAdminUiState, viewModel: VpsAdminViewModel) {
    val scroll = rememberScrollState()
    val cfg = uiState.selectedConfig

    var name by remember(cfg) { mutableStateOf(cfg?.name ?: "") }
    var host by remember(cfg) { mutableStateOf(cfg?.host ?: "") }
    var port by remember(cfg) { mutableStateOf((cfg?.sshPort ?: 22).toString()) }
    var user by remember(cfg) { mutableStateOf(cfg?.sshUser ?: "root") }
    var secret by remember(cfg) { mutableStateOf("") }
    var authType by remember(cfg) { mutableStateOf(cfg?.authType ?: SshAuthType.PASSWORD) }
    var tlsPort by remember(cfg) { mutableStateOf((cfg?.tlsTransportPort ?: 443).toString()) }
    var sni by remember(cfg) { mutableStateOf(cfg?.sniHostname ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Configuração de Servidor VPS", color = CyanPrimary, fontWeight = FontWeight.Bold)

        InputField("Nome do Servidor", name, { name = it }, "Ex: Minha VPS Frankfurt")
        InputField("IP ou Hostname", host, { host = it }, "198.51.100.1 ou vps.exemplo.com")

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                InputField("Porta SSH", port, { port = it }, "22")
            }
            Box(modifier = Modifier.weight(1f)) {
                InputField("Usuário SSH", user, { user = it }, "root")
            }
        }

        Text("Tipo de Autenticação", fontSize = 12.sp, color = Color(0xFF94A3B8))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { authType = SshAuthType.PASSWORD },
                colors = ButtonDefaults.buttonColors(containerColor = if (authType == SshAuthType.PASSWORD) CyanPrimary else DarkSurfaceVariant)
            ) {
                Text("Senha", color = if (authType == SshAuthType.PASSWORD) DarkBackground else Color.White)
            }
            Button(
                onClick = { authType = SshAuthType.PRIVATE_KEY },
                colors = ButtonDefaults.buttonColors(containerColor = if (authType == SshAuthType.PRIVATE_KEY) CyanPrimary else DarkSurfaceVariant)
            ) {
                Text("Chave Privada", color = if (authType == SshAuthType.PRIVATE_KEY) DarkBackground else Color.White)
            }
        }

        InputField(
            if (authType == SshAuthType.PASSWORD) "Palavra-passe SSH" else "Chave Privada OpenSSH (RSA/Ed25519)",
            secret,
            { secret = it },
            "Armazenada no Android KeyStore com AES-256",
            isPassword = true
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                InputField("Porta TLS Transport", tlsPort, { tlsPort = it }, "443")
            }
            Box(modifier = Modifier.weight(1f)) {
                InputField("Hostname SNI", sni, { sni = it }, "sni.exemplo.com")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                viewModel.saveVpsConfig(
                    name = name,
                    host = host,
                    sshPort = port.toIntOrNull() ?: 22,
                    sshUser = user,
                    authType = authType,
                    secretInput = secret,
                    tlsPort = tlsPort.toIntOrNull() ?: 443,
                    sni = sni,
                    onSuccess = { viewModel.selectTab(VpsAdminTab.DASHBOARD) }
                )
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
        ) {
            Text("Guardar Configuração VPS", fontWeight = FontWeight.Bold, color = DarkBackground)
        }
    }
}

@Composable
private fun AutoInstallTab(uiState: VpsAdminUiState, viewModel: VpsAdminViewModel) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Assistente de Instalação", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Configuração de 7 etapas idempotente", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
            Button(
                onClick = { viewModel.startAutoInstaller() },
                enabled = !uiState.isInstalling && uiState.selectedConfig != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                if (uiState.isInstalling) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Iniciar Instalação", fontWeight = FontWeight.Bold)
                }
            }
        }

        uiState.installSteps.forEach { step ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (step.status) {
                        StepStatus.SUCCESS -> Icon(Icons.Default.CheckCircle, null, tint = StatusConnected, modifier = Modifier.size(24.dp))
                        StepStatus.FAILED -> Icon(Icons.Default.Error, null, tint = StatusError, modifier = Modifier.size(24.dp))
                        StepStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(22.dp), color = CyanPrimary, strokeWidth = 2.dp)
                        else -> Box(modifier = Modifier.size(22.dp).border(2.dp, Color(0xFF475569), CircleShape))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Etapa ${step.stepNumber}: ${step.title}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = step.description,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (uiState.installerLog.isNotBlank()) {
            Text("Logs da Instalação", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF090D16))
                    .padding(10.dp)
            ) {
                Text(
                    text = uiState.installerLog,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = CyanPrimary
                )
            }
        }
    }
}

@Composable
private fun ServicesTab(uiState: VpsAdminUiState, viewModel: VpsAdminViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Serviços VPN & Sistema", color = Color.White, fontWeight = FontWeight.Bold)

        if (uiState.services.isEmpty()) {
            Text("Conecte via SSH para consultar o estado dos daemons.", color = Color(0xFF64748B), fontSize = 13.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.services) { svc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(svc.displayName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text(svc.statusText, color = if (svc.isActive) StatusConnected else StatusError, fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { viewModel.requestServiceAction(svc.name, "restart") },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                                ) {
                                    Text("Reiniciar", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { viewModel.requestServiceAction(svc.name, if (svc.isActive) "stop" else "start") },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (svc.isActive) StatusError else StatusConnected)
                                ) {
                                    Text(if (svc.isActive) "Parar" else "Iniciar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PortsFirewallTab(uiState: VpsAdminUiState, viewModel: VpsAdminViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Portas de Transporte & Status de Firewall", color = Color.White, fontWeight = FontWeight.Bold)

        uiState.ports.forEach { p ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Porta ${p.port} (${p.protocol})", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(p.process, color = if (p.isOpen) StatusConnected else Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (p.isOpen) StatusConnected.copy(0.2f) else DarkSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (p.isOpen) "ESCUTANDO" else "FECHADA",
                            color = if (p.isOpen) StatusConnected else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogsTerminalTab(uiState: VpsAdminUiState, viewModel: VpsAdminViewModel) {
    var cmdText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Terminal Administrativo SSH", color = Color.White, fontWeight = FontWeight.Bold)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF070A10))
                .padding(12.dp)
        ) {
            LazyColumn {
                item {
                    Text(
                        text = uiState.terminalOutput,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CyanPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = cmdText,
                onValueChange = { cmdText = it },
                placeholder = { Text("Digite comando (ex: systemctl status stunnel4)...", color = Color(0xFF64748B), fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (cmdText.isNotBlank()) {
                        viewModel.executeTerminalCommand(cmdText)
                        cmdText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Icon(Icons.Default.Send, null, tint = DarkBackground)
            }
        }
    }
}

@Composable
private fun DiagnosticsTab(uiState: VpsAdminUiState, viewModel: VpsAdminViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = { viewModel.runDiagnostics() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
        ) {
            Text("Executar Teste de Diagnóstico Completo", fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF070A10))
                .padding(12.dp)
        ) {
            Text(
                text = uiState.diagnosticResult.ifBlank { "Clique no botão acima para iniciar os testes de rede e portas." },
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFF38BDF8)
            )
        }
    }
}

@Composable
private fun SecurityTab(uiState: VpsAdminUiState, viewModel: VpsAdminViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Políticas de Segurança do Servidor", fontWeight = FontWeight.Bold, color = Color.White)
                InfoRow("Armazenamento:", "Hardware KeyStore (AES-256 GCM)")
                InfoRow("Verificação TLS:", "Ativa (Sem desativação de validação)")
                InfoRow("Host Key Fingerprint:", uiState.selectedConfig?.trustedHostKeyFingerprint ?: "Verificação estrita na primeira conexão")
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 13.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun InputField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String, isPassword: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF475569), fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )
    }
}
