package com.cadastroocr.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cadastroocr.app.ocr.DocumentImageStore
import com.cadastroocr.app.ui.navigation.AppNavGraph
import com.cadastroocr.app.ui.theme.CadastroOcrTheme
import com.cadastroocr.app.viewmodel.ClienteViewModel
import kotlinx.coroutines.launch

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

        // Sincroniza ao abrir o app e continua a cada ~20 s enquanto ele estiver visível,
        // para que cadastros feitos em outro aparelho apareçam sozinhos.
        val syncManager = (application as CadastroOcrApp).syncManager
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncManager.executarEnquantoAtivo()
            }
        }

        setContent {
            CadastroOcrTheme {
                AppNavGraph(viewModel = viewModel)
            }
        }
    }
}
