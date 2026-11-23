package com.cadastroocr.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.cadastroocr.app.ui.theme.DangerRed

@Composable
fun ConfirmDeleteDialog(
    nomeCliente: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Deseja realmente excluir este cliente?") },
        text = { Text("\"$nomeCliente\" será removido permanentemente do banco de dados local.") },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text("Excluir", color = DangerRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}
