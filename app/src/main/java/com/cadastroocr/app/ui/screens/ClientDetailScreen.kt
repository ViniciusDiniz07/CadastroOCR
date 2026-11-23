package com.cadastroocr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadastroocr.app.data.Cliente
import com.cadastroocr.app.ui.components.ConfirmDeleteDialog
import com.cadastroocr.app.ui.components.CopyableField
import com.cadastroocr.app.ui.theme.DangerRed
import com.cadastroocr.app.ui.theme.TextSecondary
import com.cadastroocr.app.viewmodel.ClienteViewModel

@Composable
fun ClientDetailScreen(
    viewModel: ClienteViewModel,
    clienteId: Long,
    onVoltar: () -> Unit,
    onEditar: (Cliente) -> Unit
) {
    var cliente by remember { mutableStateOf<Cliente?>(null) }
    var mostrarConfirmacaoExclusao by remember { mutableStateOf(false) }

    LaunchedEffect(clienteId) {
        cliente = viewModel.buscarPorId(clienteId)
    }

    val clienteAtual = cliente
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ficha do Cliente") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (clienteAtual != null) {
                        IconButton(onClick = { onEditar(clienteAtual) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { mostrarConfirmacaoExclusao = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = DangerRed)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (clienteAtual == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(clienteAtual.nomeCompleto.ifBlank { "Sem nome" }, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))

            SecaoDetalhe("Dados pessoais")
            CopyableField("Nome completo", clienteAtual.nomeCompleto)
            CopyableField("CPF", clienteAtual.cpf)
            CopyableField("Data de nascimento", clienteAtual.dataNascimento)
            CopyableField("Nome da mãe", clienteAtual.nomeDaMae)
            CopyableField("Nome do pai", clienteAtual.nomeDoPai)

            SecaoDetalhe("Documento")
            CopyableField("RG", clienteAtual.rg)
            CopyableField("Data de expedição", clienteAtual.dataExpedicaoRg)

            SecaoDetalhe("Contato")
            CopyableField("E-mail", clienteAtual.email)
            CopyableField("N° para contato", clienteAtual.telefone)

            SecaoDetalhe("Endereço")
            CopyableField("Rua", clienteAtual.rua)
            CopyableField("N°", clienteAtual.numero)
            CopyableField("Complemento", clienteAtual.complemento)
            CopyableField("CEP", clienteAtual.cep)
            CopyableField("Bairro", clienteAtual.bairro)
            CopyableField("Cidade", clienteAtual.cidade)

            SecaoDetalhe("Plano e pagamento")
            CopyableField("Plano", clienteAtual.plano)
            CopyableField("Valor", clienteAtual.valorPlano)
            CopyableField("Forma de pagamento", clienteAtual.formaPagamento)
            CopyableField("Vencimento (dia)", clienteAtual.diaVencimento)

            Spacer(Modifier.height(16.dp))
        }

        if (mostrarConfirmacaoExclusao) {
            ConfirmDeleteDialog(
                nomeCliente = clienteAtual.nomeCompleto,
                onConfirmar = {
                    mostrarConfirmacaoExclusao = false
                    viewModel.excluir(clienteAtual, onVoltar)
                },
                onCancelar = { mostrarConfirmacaoExclusao = false }
            )
        }
    }
}

@Composable
private fun SecaoDetalhe(titulo: String) {
    Spacer(Modifier.height(10.dp))
    Text(
        titulo,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
    )
    Spacer(Modifier.height(2.dp))
}
