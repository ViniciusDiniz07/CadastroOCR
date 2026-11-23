package com.cadastroocr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cadastroocr.app.ui.theme.AccentGreen
import com.cadastroocr.app.ui.theme.StatusPendingText
import com.cadastroocr.app.ui.theme.TextSecondary

@Composable
fun OcrFailureScreen(
    onTentarNovamente: () -> Unit,
    onPreencherManualmente: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = StatusPendingText,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("A imagem está difícil de ler", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tente fotografar novamente com mais iluminação e mantenha o documento alinhado dentro da moldura.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onTentarNovamente,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = androidx.compose.ui.graphics.Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Tentar novamente") }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onPreencherManualmente, modifier = Modifier.fillMaxWidth()) {
                Text("Preencher manualmente")
            }
        }
    }
}
