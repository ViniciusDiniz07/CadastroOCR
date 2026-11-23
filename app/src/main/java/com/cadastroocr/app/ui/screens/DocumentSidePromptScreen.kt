package com.cadastroocr.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cadastroocr.app.ui.theme.AccentGreen
import com.cadastroocr.app.ui.theme.TextSecondary

/**
 * Depois de capturar/escolher a frente do documento, pergunta se o usuário
 * quer adicionar o verso também — pela câmera ou pela galeria. Muitos RGs
 * trazem o nome da mãe e do pai (filiação) no verso, então isso aumenta
 * bastante a taxa de acerto do OCR.
 */
@Composable
fun DocumentSidePromptScreen(
    onFotografarVerso: () -> Unit,
    onVersoDaGaleriaEscolhido: (Uri) -> Unit,
    onContinuarSemVerso: () -> Unit
) {
    val seletorGaleria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onVersoDaGaleriaEscolhido(it) }
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Frente capturada!", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Documentos como o RG costumam trazer o nome da mãe e do pai no verso. " +
                    "Adicionar o verso também aumenta a chance de preencher esses campos automaticamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onFotografarVerso,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = androidx.compose.ui.graphics.Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Fotografar verso")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { seletorGaleria.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Escolher verso da galeria")
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onContinuarSemVerso, modifier = Modifier.fillMaxWidth()) {
                Text("Continuar só com a frente")
            }
        }
    }
}
