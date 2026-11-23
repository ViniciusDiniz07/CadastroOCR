package com.cadastroocr.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cadastroocr.app.data.Cliente
import com.cadastroocr.app.ui.theme.AccentGreen
import com.cadastroocr.app.ui.theme.SurfaceDark
import com.cadastroocr.app.ui.theme.TextSecondary

@Composable
fun ClientCard(cliente: Cliente, onClick: () -> Unit) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Surface(color = AccentGreen.copy(alpha = 0.2f), shape = RoundedCornerShape(50)) {
                    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Text(
                            iniciais(cliente.nomeCompleto),
                            color = AccentGreen,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        cliente.nomeCompleto.ifBlank { "Sem nome" },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (cliente.syncStatus != com.cadastroocr.app.data.SyncStatus.SYNCED) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = com.cadastroocr.app.ui.theme.StatusPendingBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "Pendente",
                                color = com.cadastroocr.app.ui.theme.StatusPendingText,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    "CPF: ${cliente.cpf.ifBlank { "não informado" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (cliente.plano.isNotBlank()) {
                    Text(
                        "Plano: ${cliente.plano} ${if (cliente.valorPlano.isNotBlank()) "• R$ ${cliente.valorPlano}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGreen
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary)
        }
    }
}

private fun iniciais(nome: String): String {
    val partes = nome.trim().split(" ").filter { it.isNotBlank() }
    return when {
        partes.isEmpty() -> "?"
        partes.size == 1 -> partes[0].take(2).uppercase()
        else -> (partes.first().take(1) + partes.last().take(1)).uppercase()
    }
}
