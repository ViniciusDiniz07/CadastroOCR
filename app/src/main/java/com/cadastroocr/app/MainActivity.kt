package com.cadastroocr.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.cadastroocr.app.ocr.DocumentImageStore
import com.cadastroocr.app.ui.navigation.AppNavGraph
import com.cadastroocr.app.ui.theme.CadastroOcrTheme
import com.cadastroocr.app.viewmodel.ClienteViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ClienteViewModel by viewModels {
        ClienteViewModel.factory(application as CadastroOcrApp)
    }

    private val solicitarPermissaoCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* se negado, o botão "Iniciar Câmera" simplesmente não abrirá a prévia */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Qualquer foto de documento deixada de sessões anteriores é removida
        // ao abrir o app (Seção 15 — nunca reter a imagem além do necessário).
        DocumentImageStore.limparTudo(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            solicitarPermissaoCamera.launch(Manifest.permission.CAMERA)
        }

        setContent {
            CadastroOcrTheme {
                AppNavGraph(viewModel = viewModel)
            }
        }
    }
}
