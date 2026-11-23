package com.cadastroocr.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cadastroocr.app.ui.theme.AccentGreen
import com.cadastroocr.app.ui.theme.SurfaceElevated
import com.cadastroocr.app.ui.theme.TextSecondary
import com.cadastroocr.app.util.ClipboardUtil
import kotlinx.coroutines.delay

/**
 * Exibe um dado do cliente (somente leitura) com um botão de copiar próprio.
 * Usado na Ficha do Cliente — cada campo copia SOMENTE o próprio valor,
 * nunca todos os dados juntos (Seção 8/9 do briefing).
 */
@Composable
fun CopyableField(rotulo: String, valor: String) {
    val context = LocalContext.current
    var copiado by remember { mutableStateOf(false) }

    LaunchedEffect(copiado) {
        if (copiado) {
            delay(1600)
            copiado = false
        }
    }

    Surface(
        color = SurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rotulo.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(
                    valor.ifBlank { "Não informado" },
                    style = MaterialTheme.typography.bodyLarge
                )
                AnimatedVisibility(visible = copiado) {
                    Text(
                        "✓ $rotulo copiado",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGreen,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (valor.isNotBlank()) {
                IconButton(onClick = {
                    ClipboardUtil.copiarCampo(context, rotulo, valor)
                    copiado = true
                }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar $rotulo", tint = AccentGreen)
                }
            }
        }
    }
}
