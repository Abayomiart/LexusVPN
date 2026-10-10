package com.novavpn.app.ui.screens.editor

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novavpn.app.ui.components.NovaTopBar
import com.novavpn.app.ui.theme.CyanPrimary
import com.novavpn.app.ui.theme.DarkBackground
import com.novavpn.app.ui.theme.DarkSurface
import com.novavpn.app.ui.theme.StatusConnected
import com.novavpn.app.ui.theme.StatusError

@Composable
fun ServerEditorScreen(
    serverId: String?,
    onNavigateBack: () -> Unit,
    viewModel: ServerEditorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(serverId) {
        viewModel.loadServer(serverId)
    }

    Scaffold(
        topBar = {
            NovaTopBar(
                title = if (serverId == null) "Adicionar Servidor" else "Editar Servidor",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
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
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = StatusError,
                        modifier = Modifier.padding(14.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (uiState.testConnectionResult != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.testConnectionResult!!.contains("bem-sucedida"))
                            StatusConnected.copy(alpha = 0.2f)
                        else StatusError.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = uiState.testConnectionResult ?: "",
                        color = if (uiState.testConnectionResult!!.contains("bem-sucedida")) StatusConnected else StatusError,
                        modifier = Modifier.padding(14.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Identificação Básica
            EditorSectionTitle("Identificação do Servidor")
            EditorTextField(
                label = "Nome do Servidor",
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                placeholder = "Ex: Brasil - São Paulo Principal"
            )
            EditorTextField(
                label = "País",
                value = uiState.country,
                onValueChange = { viewModel.updateCountry(it) },
                placeholder = "Ex: Brasil"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Rede e Conexão
            EditorSectionTitle("Configurações de Rede")
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(2f)) {
                    EditorTextField(
                        label = "Host / Endereço IP",
                        value = uiState.host,
                        onValueChange = { viewModel.updateHost(it) },
                        placeholder = "vpn.exemplo.com ou 198.51.100.1"
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    EditorTextField(
                        label = "Porta",
                        value = uiState.port,
                        onValueChange = { viewModel.updatePort(it) },
                        placeholder = "51820"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Criptografia e Chaves WireGuard
            EditorSectionTitle("Criptografia WireGuard & Keystore")
            EditorTextField(
                label = "Chave Pública do Servidor",
                value = uiState.publicKey,
                onValueChange = { viewModel.updatePublicKey(it) },
                placeholder = "Chave pública em Base64"
            )

            EditorTextField(
                label = "Chave Privada do Cliente (Opcional)",
                value = uiState.privateKey,
                onValueChange = { viewModel.updatePrivateKey(it) },
                placeholder = "Protegida no Android KeyStore com AES-GCM",
                isPassword = true
            )

            Text(
                text = "Alias no Keystore: ${uiState.privateKeyAlias}",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Parâmetros do Túnel
            EditorSectionTitle("Parâmetros do Túnel")
            EditorTextField(
                label = "Endereço do Cliente (CIDR)",
                value = uiState.clientAddress,
                onValueChange = { viewModel.updateClientAddress(it) },
                placeholder = "10.66.66.2/32"
            )
            EditorTextField(
                label = "Servidores DNS",
                value = uiState.dns,
                onValueChange = { viewModel.updateDns(it) },
                placeholder = "1.1.1.1, 1.0.0.1"
            )
            EditorTextField(
                label = "Allowed IPs (Rotas)",
                value = uiState.allowedIps,
                onValueChange = { viewModel.updateAllowedIps(it) },
                placeholder = "0.0.0.0/0, ::/0"
            )
            EditorTextField(
                label = "MTU",
                value = uiState.mtu,
                onValueChange = { viewModel.updateMtu(it) },
                placeholder = "1420"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botões de Ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.testConnection() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary)
                ) {
                    if (uiState.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CyanPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.NetworkPing, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testar")
                    }
                }

                Button(
                    onClick = {
                        viewModel.saveServer(onSuccess = onNavigateBack)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = DarkBackground)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salvar", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun EditorSectionTitle(title: String) {
    Text(
        text = title,
        color = CyanPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun EditorTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF475569), fontSize = 14.sp) },
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
