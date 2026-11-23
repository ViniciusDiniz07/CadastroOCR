package com.cadastroocr.app.camera

import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cadastroocr.app.ocr.DocumentImageStore
import com.cadastroocr.app.ui.theme.AccentGreen
import java.io.File

/**
 * Câmera própria para fotografar o documento (Seção 19). Mostra uma moldura
 * indicando onde posicionar o RG/CNH e captura de fato uma foto real via
 * CameraX — nada aqui é simulado.
 */
@Composable
fun CameraScreen(
    onFotoCapturada: (File) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidViewCameraPreview(
            imageCapture = imageCapture,
            lifecycleOwner = lifecycleOwner
        )

        // Moldura indicando onde posicionar o documento
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.88f)
                .aspectRatio(1.586f) // proporção padrão de um cartão de identidade
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        Text(
            text = "Posicione o documento dentro da área",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancelar) {
                Icon(Icons.Filled.Close, contentDescription = "Cancelar", tint = Color.White)
            }

            FloatingActionButton(
                onClick = {
                    val arquivo = DocumentImageStore.novoArquivoTemporario(context)
                    val opcoes = ImageCapture.OutputFileOptions.Builder(arquivo).build()
                    imageCapture.takePicture(
                        opcoes,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onFotoCapturada(arquivo)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                Toast.makeText(
                                    context,
                                    "Não foi possível capturar a foto. Tente novamente.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                },
                containerColor = AccentGreen,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Capturar", tint = Color.Black)
            }

            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun AndroidViewCameraPreview(
    imageCapture: ImageCapture,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
