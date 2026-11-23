package com.cadastroocr.app.viewmodel

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cadastroocr.app.CadastroOcrApp
import com.cadastroocr.app.ai.GeminiInterpreter
import com.cadastroocr.app.ai.TipoOrigem
import com.cadastroocr.app.data.Cliente
import com.cadastroocr.app.data.ClienteRepository
import com.cadastroocr.app.ocr.ConversationDataParser
import com.cadastroocr.app.ocr.DadosExtraidos
import com.cadastroocr.app.ocr.DocumentDataParser
import com.cadastroocr.app.ocr.DocumentImageStore
import com.cadastroocr.app.ocr.NivelReconhecimento
import com.cadastroocr.app.ocr.OcrAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class EstadoOcr {
    data object Ocioso : EstadoOcr()
    data object Processando : EstadoOcr()
    data class Concluido(val dados: DadosExtraidos, val nivel: NivelReconhecimento) : EstadoOcr()
    data class Falhou(val motivo: String) : EstadoOcr()
}

class ClienteViewModel(
    private val repository: ClienteRepository,
    val authManager: com.cadastroocr.app.auth.AuthManager
) : ViewModel() {

    private val ocrAnalyzer = OcrAnalyzer()

    val estadoAuth = authManager.estadoAuth
    val estadoSincronizacao = repository.syncManager.estadoSincronizacao
    val quantidadePendentes = repository.observarQuantidadePendentes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun sincronizarManual() {
        viewModelScope.launch {
            repository.sincronizarAgora()
        }
    }

    fun fazerLogin(email: String, senha: String, aoConcluir: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val resultado = authManager.login(email, senha)
            if (resultado.isSuccess) {
                repository.syncManager.resetarCursor()
                repository.sincronizarAgora()
            }
            aoConcluir(resultado)
        }
    }

    fun fazerCadastro(email: String, senha: String, aoConcluir: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val resultado = authManager.cadastrar(email, senha)
            if (resultado.isSuccess) {
                repository.syncManager.resetarCursor()
                repository.sincronizarAgora()
            }
            aoConcluir(resultado)
        }
    }

    fun usarModoOffline() {
        authManager.ativarModoOffline()
    }

    fun logout() {
        authManager.logout()
        repository.syncManager.resetarCursor()
    }

    /**
     * OCR "avulso", sem passar pelo fluxo de estado (_estadoOcr) usado pelo
     * documento — usado pela importação de conversa, que roda direto dentro
     * da tela de revisão para não perder os campos já preenchidos.
     */
    suspend fun ocrTextoDeImagem(bitmap: android.graphics.Bitmap): String =
        ocrAnalyzer.reconhecerTexto(bitmap)

    suspend fun ocrTextoDeImagem(contexto: android.content.Context, uri: Uri): String =
        ocrAnalyzer.reconhecerTexto(contexto, uri)

    private val _termoBusca = MutableStateFlow("")
    val termoBusca: StateFlow<String> = _termoBusca

    private val _ordem = MutableStateFlow(OrdemClientes.NOME_AZ)
    val ordem: StateFlow<OrdemClientes> = _ordem

    fun definirOrdem(ordem: OrdemClientes) {
        _ordem.value = ordem
    }

    // Busca (no banco) + ordenação (só na apresentação), sobre TODOS os registros locais,
    // inclusive os que vieram da nuvem.
    val clientes: StateFlow<List<Cliente>> = combine(
        _termoBusca.flatMapLatest { termo -> repository.observarClientes(termo) },
        _ordem
    ) { lista, ordem -> ordenarClientes(lista, ordem) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _estadoOcr = MutableStateFlow<EstadoOcr>(EstadoOcr.Ocioso)
    val estadoOcr: StateFlow<EstadoOcr> = _estadoOcr

    // Guarda a foto da "frente" do documento (câmera = File, galeria = Uri)
    // enquanto o usuário decide se vai adicionar o verso também.
    private var frentePendente: File? = null
    private var frenteUriPendente: Uri? = null

    fun definirFrentePendente(arquivo: File) {
        frentePendente = arquivo
        frenteUriPendente = null
    }

    fun definirFrenteUriPendente(uri: Uri) {
        frenteUriPendente = uri
        frentePendente = null
    }

    fun frentePendente(): File? = frentePendente
    fun frenteUriPendente(): Uri? = frenteUriPendente

    fun atualizarBusca(termo: String) {
        _termoBusca.value = termo
    }

    private fun temInternet(contexto: android.content.Context): Boolean {
        val cm = contexto.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val rede = cm.activeNetwork ?: return false
        val capacidades = cm.getNetworkCapabilities(rede) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Ponto único de interpretação de texto (documento OU conversa).
     * Tenta a IA remota primeiro (mais robusta, entende qualquer layout sem
     * precisar de regra nova); se não tiver internet, o backend falhar, ou
     * demorar demais, cai automaticamente para o parser de regras local —
     * o app nunca fica "travado" sem conseguir preencher nada.
     */
    suspend fun interpretarTexto(texto: String, tipo: TipoOrigem, contexto: android.content.Context): DadosExtraidos {
        val resultadoIa = if (temInternet(contexto)) {
            withContext(kotlinx.coroutines.Dispatchers.IO) { GeminiInterpreter.interpretar(texto) }
        } else {
            null
        }
        return resultadoIa ?: when (tipo) {
            TipoOrigem.DOCUMENTO -> DocumentDataParser.interpretar(texto)
            TipoOrigem.CONVERSA -> ConversationDataParser.interpretar(texto)
        }
    }

    /**
     * Dispara o OCR real sobre uma ou mais imagens do documento (frente e,
     * opcionalmente, verso — o nome dos pais costuma estar em um dos dois
     * lados dependendo do documento). O texto de todas as imagens é somado
     * antes de interpretar os campos.
     */
    fun processarDocumentos(
        arquivos: List<File> = emptyList(),
        urisGaleria: List<Uri> = emptyList(),
        contexto: android.content.Context
    ) {
        _estadoOcr.value = EstadoOcr.Processando
        viewModelScope.launch {
            try {
                val textos = mutableListOf<String>()
                for (arquivo in arquivos) {
                    textos.add(ocrAnalyzer.reconhecerTexto(arquivo))
                }
                for (uri in urisGaleria) {
                    textos.add(ocrAnalyzer.reconhecerTexto(contexto, uri))
                }
                arquivos.forEach { DocumentImageStore.limparImagemTemporaria(it) }
                frentePendente = null
                frenteUriPendente = null

                val textoCombinado = textos.joinToString("\n")
                val dados = interpretarTexto(textoCombinado, TipoOrigem.DOCUMENTO, contexto)
                _estadoOcr.value = EstadoOcr.Concluido(dados, dados.nivel())
            } catch (e: Exception) {
                arquivos.forEach { DocumentImageStore.limparImagemTemporaria(it) }
                frentePendente = null
                frenteUriPendente = null
                _estadoOcr.value = EstadoOcr.Falhou(
                    "A imagem está difícil de ler. Tente fotografar novamente com mais iluminação e mantenha o documento alinhado."
                )
            }
        }
    }

    fun resetarEstadoOcr() {
        _estadoOcr.value = EstadoOcr.Ocioso
    }

    suspend fun buscarPorId(id: Long): Cliente? = repository.buscarPorId(id)

    fun salvar(cliente: Cliente, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repository.salvar(cliente)
            aoConcluir()
        }
    }

    fun excluir(cliente: Cliente, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repository.excluir(cliente)
            aoConcluir()
        }
    }

    companion object {
        fun factory(app: CadastroOcrApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ClienteViewModel(app.repository, app.authManager) as T
            }
        }
    }
}
