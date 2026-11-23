package com.cadastroocr.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cadastroocr.app.ui.theme.AccentGreen
import com.cadastroocr.app.ui.theme.SurfaceDark
import com.cadastroocr.app.ui.theme.TextSecondary
import com.cadastroocr.app.viewmodel.ClienteViewModel

@Composable
fun NewClientScreen(
    viewModel: ClienteViewModel,
    onVoltar: () -> Unit,
    onEscolherCamera: () -> Unit,
    onImagensDaGaleriaEscolhidas: (List<Uri>) -> Unit,
    onDigitacaoManual: () -> Unit
) {
    val context = LocalContext.current
    val seletorGaleria = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) onImagensDaGaleriaEscolhidas(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo Cliente") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Text(
                "Escolha como deseja importar as informações do cliente",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(20.dp))

            OpcaoCadastro(
                icone = Icons.Filled.CameraAlt,
                titulo = "Escanear Documento (OCR)",
                descricao = "Capture uma foto nítida do RG ou CNH. O sistema processará e preencherá os dados automaticamente.",
                acaoTexto = "Iniciar Câmera →",
                destaque = true,
                onClick = onEscolherCamera
            )
            Spacer(Modifier.height(14.dp))
            OpcaoCadastro(
                icone = Icons.Filled.Image,
                titulo = "Escolher da Galeria",
                descricao = "Selecione uma ou duas fotos do documento (frente e verso) já existentes no celular.",
                acaoTexto = "Abrir galeria →",
                destaque = false,
                onClick = { seletorGaleria.launch("image/*") }
            )
            Spacer(Modifier.height(14.dp))
            OpcaoCadastro(
                icone = Icons.Filled.Edit,
                titulo = "Digitação Manual",
                descricao = "Se preferir, digite todos os dados do cliente manualmente através do nosso formulário guiado.",
                acaoTexto = "Preencher formulário →",
                destaque = false,
                onClick = onDigitacaoManual
            )
        }
    }
}

@Composable
private fun OpcaoCadastro(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    descricao: String,
    acaoTexto: String,
    destaque: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (destaque) AccentGreen.copy(alpha = 0.12f) else SurfaceDark,
        shape = RoundedCornerShape(18.dp),
        border = if (destaque) BorderStroke(1.dp, AccentGreen) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Icon(icone, contentDescription = null, tint = if (destaque) AccentGreen else TextSecondary)
            Spacer(Modifier.height(10.dp))
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(descricao, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(10.dp))
            Text(acaoTexto, style = MaterialTheme.typography.labelMedium, color = AccentGreen)
        }
    }
}
