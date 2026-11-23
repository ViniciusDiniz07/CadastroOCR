package com.cadastroocr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cadastroocr.app.ocr.NivelReconhecimento
import com.cadastroocr.app.ui.theme.AccentGreen
import com.cadastroocr.app.ui.theme.TextSecondary
import com.cadastroocr.app.viewmodel.EstadoOcr
import com.cadastroocr.app.viewmodel.ClienteViewModel

@Composable
fun ProcessingScreen(
    viewModel: ClienteViewModel,
    onConcluidoComDados: () -> Unit,
    onFalhou: () -> Unit
) {
    val estado by viewModel.estadoOcr.collectAsStateWithLifecycle()

    LaunchedEffect(estado) {
        when (val e = estado) {
            is EstadoOcr.Concluido -> {
                if (e.nivel == NivelReconhecimento.INSUFICIENTE) onFalhou() else onConcluidoComDados()
            }
            is EstadoOcr.Falhou -> onFalhou()
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AccentGreen)
            Spacer(Modifier.height(20.dp))
            Text("Processando documento...", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Nossos algoritmos estão extraindo os dados do documento",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
