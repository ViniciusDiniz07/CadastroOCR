package com.cadastroocr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadastroocr.app.auth.EstadoAuth
import com.cadastroocr.app.sync.EstadoSincronizacao
import com.cadastroocr.app.ui.components.ClientCard
import com.cadastroocr.app.ui.theme.AccentGreen
import com.cadastroocr.app.ui.theme.BorderSubtle
import com.cadastroocr.app.ui.theme.DangerRed
import com.cadastroocr.app.ui.theme.StatusApprovedBg
import com.cadastroocr.app.ui.theme.StatusApprovedText
import com.cadastroocr.app.ui.theme.StatusPendingBg
import com.cadastroocr.app.ui.theme.StatusPendingText
import com.cadastroocr.app.ui.theme.SurfaceDark
import com.cadastroocr.app.ui.theme.TextMuted
import com.cadastroocr.app.ui.theme.TextSecondary
import com.cadastroocr.app.viewmodel.ClienteViewModel

@Composable
fun ClientListScreen(
    viewModel: ClienteViewModel,
    onNovoCliente: () -> Unit,
    onAbrirCliente: (Long) -> Unit,
    onLogout: () -> Unit
) {
    val clientes by viewModel.clientes.collectAsStateWithLifecycle()
    val termoBusca by viewModel.termoBusca.collectAsStateWithLifecycle()
    val estadoSync by viewModel.estadoSincronizacao.collectAsStateWithLifecycle()
    val estadoAuth by viewModel.estadoAuth.collectAsStateWithLifecycle()
    val qtdPendentes by viewModel.quantidadePendentes.collectAsStateWithLifecycle()

    var mostrarDialogoLogout by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovoCliente,
                containerColor = AccentGreen,
                contentColor = Color.Black,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Novo Cliente", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Barra Superior com Título, Status de Sincronização e Logout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Cadastro OCR", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        when (estadoAuth) {
                            is EstadoAuth.Autenticado -> (estadoAuth as EstadoAuth.Autenticado).email
                            is EstadoAuth.ModoOffline -> "Modo Offline (Local)"
                            else -> "Não conectado"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botão Sincronizar Agora
                    IconButton(
                        onClick = { viewModel.sincronizarManual() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Sincronizar",
                            tint = AccentGreen
                        )
                    }

                    // Botão Logout
                    IconButton(onClick = { mostrarDialogoLogout = true }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Sair", tint = TextMuted)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Barra de Status da Sincronização
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (estadoSync) {
                            is EstadoSincronizacao.Sincronizado -> {
                                Icon(Icons.Filled.CloudDone, contentDescription = null, tint = StatusApprovedText, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Nuvem sincronizada", color = StatusApprovedText, style = MaterialTheme.typography.labelMedium)
                            }
                            is EstadoSincronizacao.Sincronizando -> {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentGreen, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text((estadoSync as EstadoSincronizacao.Sincronizando).progresso, color = AccentGreen, style = MaterialTheme.typography.labelMedium)
                            }
                            is EstadoSincronizacao.Offline -> {
                                Icon(Icons.Filled.CloudOff, contentDescription = null, tint = StatusPendingText, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Sem internet (Offline)", color = StatusPendingText, style = MaterialTheme.typography.labelMedium)
                            }
                            is EstadoSincronizacao.ModoLocal -> {
                                Icon(Icons.Filled.CloudOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Modo Local", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                            }
                            is EstadoSincronizacao.Erro -> {
                                Icon(Icons.Filled.CloudOff, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Erro ao sincronizar", color = DangerRed, style = MaterialTheme.typography.labelMedium)
                            }
                            else -> {}
                        }
                    }

                    if (qtdPendentes > 0) {
                        Surface(
                            color = StatusPendingBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "$qtdPendentes pendência(s)",
                                color = StatusPendingText,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = termoBusca,
                onValueChange = viewModel::atualizarBusca,
                placeholder = { Text("Buscar por nome, CPF, e-mail ou telefone...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            Text(
                "Clientes cadastrados (${clientes.size})",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            if (clientes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (termoBusca.isBlank()) "Nenhum cliente cadastrado ainda."
                        else "Nenhum cliente encontrado para \"$termoBusca\".",
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(clientes, key = { it.id }) { cliente ->
                        ClientCard(cliente = cliente, onClick = { onAbrirCliente(cliente.id) })
                    }
                }
            }
        }
    }

    if (mostrarDialogoLogout) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoLogout = false },
            title = { Text("Sair da conta") },
            text = { Text("Deseja desconectar sua conta? Seus dados locais continuarão salvos no aparelho.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoLogout = false
                        viewModel.logout()
                        onLogout()
                    }
                ) {
                    Text("Sair", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoLogout = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

