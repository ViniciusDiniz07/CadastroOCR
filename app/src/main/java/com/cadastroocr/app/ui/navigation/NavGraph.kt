package com.cadastroocr.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cadastroocr.app.camera.CameraScreen
import com.cadastroocr.app.data.Cliente
import com.cadastroocr.app.ui.screens.*
import com.cadastroocr.app.viewmodel.ClienteViewModel
import com.cadastroocr.app.viewmodel.EstadoOcr

private object Rotas {
    const val LOGIN = "login"
    const val LISTA = "lista"
    const val NOVO = "novo"
    const val CAMERA_FRENTE = "camera/frente"
    const val CAMERA_VERSO = "camera/verso"
    const val PERGUNTA_VERSO = "pergunta_verso"
    const val PROCESSANDO = "processando"
    const val REVISAO_OCR = "revisao_ocr"
    const val REVISAO_MANUAL = "revisao_manual"
    const val REVISAO_EDICAO = "revisao_edicao/{clienteId}"
    const val DETALHE = "detalhe/{clienteId}"
    const val FALHA_OCR = "falha_ocr"
}

@Composable
fun AppNavGraph(viewModel: ClienteViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val inicio = if (viewModel.authManager.estaAutenticado()) Rotas.LISTA else Rotas.LOGIN

    NavHost(navController = navController, startDestination = inicio) {

        composable(Rotas.LOGIN) {
            LoginScreen(
                viewModel = viewModel,
                onLoginSucesso = {
                    navController.navigate(Rotas.LISTA) {
                        popUpTo(Rotas.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Rotas.LISTA) {
            ClientListScreen(
                viewModel = viewModel,
                onNovoCliente = { navController.navigate(Rotas.NOVO) },
                onAbrirCliente = { id -> navController.navigate("detalhe/$id") },
                onLogout = {
                    navController.navigate(Rotas.LOGIN) {
                        popUpTo(Rotas.LISTA) { inclusive = true }
                    }
                }
            )
        }

        composable(Rotas.NOVO) {
            NewClientScreen(
                viewModel = viewModel,
                onVoltar = { navController.popBackStack() },
                onEscolherCamera = { navController.navigate(Rotas.CAMERA_FRENTE) },
                onImagensDaGaleriaEscolhidas = { uris ->
                    when {
                        // usuário conseguiu marcar frente + verso de uma vez na galeria
                        uris.size >= 2 -> {
                            viewModel.processarDocumentos(urisGaleria = uris.take(2), contexto = context)
                            navController.navigate(Rotas.PROCESSANDO)
                        }
                        // só veio 1 foto — pergunta se quer adicionar o verso (comum:
                        // o seletor padrão do celular às vezes só permite marcar 1 por vez)
                        uris.size == 1 -> {
                            viewModel.definirFrenteUriPendente(uris[0])
                            navController.navigate(Rotas.PERGUNTA_VERSO)
                        }
                    }
                },
                onDigitacaoManual = { navController.navigate(Rotas.REVISAO_MANUAL) }
            )
        }

        composable(Rotas.CAMERA_FRENTE) {
            CameraScreen(
                onFotoCapturada = { arquivo ->
                    viewModel.definirFrentePendente(arquivo)
                    navController.navigate(Rotas.PERGUNTA_VERSO) {
                        popUpTo(Rotas.CAMERA_FRENTE) { inclusive = true }
                    }
                },
                onCancelar = { navController.popBackStack() }
            )
        }

        composable(Rotas.PERGUNTA_VERSO) {
            DocumentSidePromptScreen(
                onFotografarVerso = { navController.navigate(Rotas.CAMERA_VERSO) },
                onVersoDaGaleriaEscolhido = { uriVerso ->
                    val frenteFile = viewModel.frentePendente()
                    val frenteUri = viewModel.frenteUriPendente()
                    viewModel.processarDocumentos(
                        arquivos = listOfNotNull(frenteFile),
                        urisGaleria = listOfNotNull(frenteUri, uriVerso),
                        contexto = context
                    )
                    navController.navigate(Rotas.PROCESSANDO) {
                        popUpTo(Rotas.PERGUNTA_VERSO) { inclusive = true }
                    }
                },
                onContinuarSemVerso = {
                    val frenteFile = viewModel.frentePendente()
                    val frenteUri = viewModel.frenteUriPendente()
                    viewModel.processarDocumentos(
                        arquivos = listOfNotNull(frenteFile),
                        urisGaleria = listOfNotNull(frenteUri),
                        contexto = context
                    )
                    navController.navigate(Rotas.PROCESSANDO) {
                        popUpTo(Rotas.PERGUNTA_VERSO) { inclusive = true }
                    }
                }
            )
        }

        composable(Rotas.CAMERA_VERSO) {
            CameraScreen(
                onFotoCapturada = { arquivoVerso ->
                    val frenteFile = viewModel.frentePendente()
                    val frenteUri = viewModel.frenteUriPendente()
                    viewModel.processarDocumentos(
                        arquivos = listOfNotNull(frenteFile, arquivoVerso),
                        urisGaleria = listOfNotNull(frenteUri),
                        contexto = context
                    )
                    navController.navigate(Rotas.PROCESSANDO) {
                        popUpTo(Rotas.CAMERA_VERSO) { inclusive = true }
                    }
                },
                onCancelar = { navController.popBackStack() }
            )
        }

        composable(Rotas.PROCESSANDO) {
            ProcessingScreen(
                viewModel = viewModel,
                onConcluidoComDados = {
                    navController.navigate(Rotas.REVISAO_OCR) {
                        popUpTo(Rotas.PROCESSANDO) { inclusive = true }
                    }
                },
                onFalhou = {
                    navController.navigate(Rotas.FALHA_OCR) {
                        popUpTo(Rotas.PROCESSANDO) { inclusive = true }
                    }
                }
            )
        }

        composable(Rotas.FALHA_OCR) {
            OcrFailureScreen(
                onTentarNovamente = {
                    viewModel.resetarEstadoOcr()
                    navController.navigate(Rotas.CAMERA_FRENTE) {
                        popUpTo(Rotas.FALHA_OCR) { inclusive = true }
                    }
                },
                onPreencherManualmente = {
                    navController.navigate(Rotas.REVISAO_MANUAL) {
                        popUpTo(Rotas.FALHA_OCR) { inclusive = true }
                    }
                }
            )
        }

        composable(Rotas.REVISAO_OCR) {
            val estado by viewModel.estadoOcr.collectAsState()
            val dados = (estado as? EstadoOcr.Concluido)?.dados
            val parcial = (estado as? EstadoOcr.Concluido)?.nivel == com.cadastroocr.app.ocr.NivelReconhecimento.PARCIAL
            ReviewScreen(
                viewModel = viewModel,
                clienteExistente = null,
                dadosOcr = dados,
                avisoOcrParcial = parcial,
                onVoltar = { navController.popBackStack(Rotas.LISTA, inclusive = false) },
                onSalvo = {
                    viewModel.resetarEstadoOcr()
                    navController.navigate(Rotas.LISTA) { popUpTo(Rotas.LISTA) { inclusive = true } }
                }
            )
        }

        composable(Rotas.REVISAO_MANUAL) {
            ReviewScreen(
                viewModel = viewModel,
                clienteExistente = null,
                dadosOcr = null,
                avisoOcrParcial = false,
                onVoltar = { navController.popBackStack(Rotas.LISTA, inclusive = false) },
                onSalvo = {
                    navController.navigate(Rotas.LISTA) { popUpTo(Rotas.LISTA) { inclusive = true } }
                }
            )
        }

        composable(
            Rotas.REVISAO_EDICAO,
            arguments = listOf(navArgument("clienteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("clienteId") ?: 0L
            var cliente by remember { mutableStateOf<Cliente?>(null) }
            LaunchedEffect(id) { cliente = viewModel.buscarPorId(id) }

            if (cliente == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                ReviewScreen(
                    viewModel = viewModel,
                    clienteExistente = cliente,
                    dadosOcr = null,
                    avisoOcrParcial = false,
                    onVoltar = { navController.popBackStack() },
                    onSalvo = { navController.popBackStack() }
                )
            }
        }

        composable(
            Rotas.DETALHE,
            arguments = listOf(navArgument("clienteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("clienteId") ?: 0L
            ClientDetailScreen(
                viewModel = viewModel,
                clienteId = id,
                onVoltar = { navController.popBackStack() },
                onEditar = { cliente -> navController.navigate("revisao_edicao/${cliente.id}") }
            )
        }
    }
}
