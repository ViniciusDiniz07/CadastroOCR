package com.cadastroocr.app.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.cadastroocr.app.auth.AuthConfig
import com.cadastroocr.app.auth.AuthManager
import com.cadastroocr.app.auth.EstadoAuth
import com.cadastroocr.app.data.Cliente
import com.cadastroocr.app.data.ClienteDao
import com.cadastroocr.app.data.SyncStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

sealed class EstadoSincronizacao {
    data object Sincronizado : EstadoSincronizacao()
    data class Sincronizando(val progresso: String = "Sincronizando...") : EstadoSincronizacao()
    data object Offline : EstadoSincronizacao()
    data object ModoLocal : EstadoSincronizacao()
    data class Pendente(val quantidade: Int) : EstadoSincronizacao()
    data class Erro(val motivo: String) : EstadoSincronizacao()
}

/**
 * Sincronização local-first entre o Room (celular) e o Supabase.
 *
 * Ciclo: PULL (baixa o que mudou na nuvem) -> PUSH (envia a fila pendente) -> PULL de confirmação.
 *
 * Pontos-chave:
 *  - O cursor do pull é o `updated_at` do SERVIDOR (guardado em SharedPreferences). Ele não depende do
 *    relógio do celular nem de quando este aparelho enviou algo, então nenhum cadastro novo é "pulado".
 *  - "Sincronizado" só aparece se pull e push deram certo; falhas viram estado de Erro/Offline + retry.
 *  - O push só conta como feito se o servidor devolver a linha gravada.
 *  - Exclusões são lógicas (is_deleted) e chegam aos outros aparelhos pelo pull.
 */
class SyncManager(
    private val context: Context,
    private val dao: ClienteDao,
    private val apiService: SupabaseApiService,
    private val authManager: AuthManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("cadastro_ocr_sync", Context.MODE_PRIVATE)

    private val _estadoSincronizacao = MutableStateFlow<EstadoSincronizacao>(EstadoSincronizacao.Sincronizado)
    val estadoSincronizacao: StateFlow<EstadoSincronizacao> = _estadoSincronizacao.asStateFlow()

    private val mutex = Mutex()
    @Volatile private var repetirCiclo = false
    @Volatile private var tentativasFalhas = 0
    private val retryAgendado = AtomicBoolean(false)

    init {
        registrarMonitorRede()
    }

    private fun registrarMonitorRede() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Internet voltou: sincroniza o que ficou pendente.
                scope.launch { sincronizar() }
            }

            override fun onLost(network: Network) {
                if (!temInternet()) _estadoSincronizacao.value = EstadoSincronizacao.Offline
            }
        })
    }

    fun temInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val rede = cm.activeNetwork ?: return false
        val cap = cm.getNetworkCapabilities(rede) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Esquece o cursor: a próxima sincronização baixa tudo de novo (usado em login/logout). */
    fun resetarCursor() {
        prefs.edit().remove(KEY_CURSOR).apply()
    }

    /**
     * Sincroniza ao abrir o app e depois a cada [intervaloMs] enquanto o app estiver visível.
     * Chamar dentro de `repeatOnLifecycle(STARTED)`: para sozinho quando o app vai para segundo plano.
     */
    suspend fun executarEnquantoAtivo(intervaloMs: Long = INTERVALO_PADRAO_MS) {
        while (true) {
            sincronizar()
            delay(intervaloMs)
        }
    }

    suspend fun sincronizar(): Result<Unit> = withContext(Dispatchers.IO) {
        // Já existe um ciclo rodando: pede para ele repetir ao terminar (pega o que acabou de ser salvo).
        if (!mutex.tryLock()) {
            repetirCiclo = true
            return@withContext Result.success(Unit)
        }
        try {
            var resultado: Result<Unit>
            do {
                repetirCiclo = false
                resultado = executarCiclo()
            } while (repetirCiclo && resultado.isSuccess)
            resultado
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun executarCiclo(): Result<Unit> {
        val auth = authManager.estadoAuth.value
        if (!AuthConfig.configurado() || auth is EstadoAuth.ModoOffline) {
            _estadoSincronizacao.value = EstadoSincronizacao.ModoLocal
            return Result.success(Unit)
        }
        if (auth is EstadoAuth.Desconectado) {
            _estadoSincronizacao.value = EstadoSincronizacao.Erro("Sessão expirada. Entre novamente.")
            return Result.failure(Exception("Usuário não autenticado"))
        }
        if (!temInternet()) {
            _estadoSincronizacao.value = EstadoSincronizacao.Offline
            agendarRetry()
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        _estadoSincronizacao.value = EstadoSincronizacao.Sincronizando("Sincronizando...")

        var primeiroErro: Throwable? = null
        try {
            // 1) PULL: traz o que mudou na nuvem (novos, edições e exclusões de outros aparelhos).
            puxarDaNuvem().exceptionOrNull()?.let { if (primeiroErro == null) primeiroErro = it }

            // 2) PUSH: envia a fila persistente (os pendentes ficam no Room até o servidor confirmar).
            var enviados = 0
            for (cliente in dao.buscarPendentes()) {
                val r = enviar(cliente)
                if (r.isSuccess) enviados++ else if (primeiroErro == null) primeiroErro = r.exceptionOrNull()
            }

            // 3) PULL de confirmação: consulta real ao servidor depois de enviar.
            if (enviados > 0) {
                puxarDaNuvem().exceptionOrNull()?.let { if (primeiroErro == null) primeiroErro = it }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (primeiroErro == null) primeiroErro = e
        }

        val erro = primeiroErro
        if (erro == null) {
            tentativasFalhas = 0
            _estadoSincronizacao.value = EstadoSincronizacao.Sincronizado
            return Result.success(Unit)
        }

        when {
            authManager.estadoAuth.value is EstadoAuth.Desconectado ->
                _estadoSincronizacao.value = EstadoSincronizacao.Erro("Sessão expirada. Entre novamente.")
            erro is IOException -> {
                _estadoSincronizacao.value = EstadoSincronizacao.Offline
                agendarRetry()
            }
            else -> {
                _estadoSincronizacao.value = EstadoSincronizacao.Erro(erro.message ?: "Erro na sincronização")
                agendarRetry()
            }
        }
        return Result.failure(erro)
    }

    /** Executa [chamada] com um token válido; se levar 401, renova a sessão e tenta mais uma vez. */
    private suspend fun <T> comToken(chamada: suspend (String) -> Result<T>): Result<T> {
        val token = authManager.obterTokenValido().getOrElse { return Result.failure(it) }
        val resultado = chamada(token)
        val erro = resultado.exceptionOrNull()
        if (erro is ApiHttpException && erro.codigo == 401) {
            val novoToken = authManager.obterTokenValido(tokenRecusado = token).getOrElse { return Result.failure(it) }
            return chamada(novoToken)
        }
        return resultado
    }

    private suspend fun puxarDaNuvem(): Result<Unit> {
        val cursorInicial = prefs.getString(KEY_CURSOR, null)
        var maiorUpdatedAt: String? = cursorInicial
        var offset = 0

        while (true) {
            val pagina = comToken { token ->
                apiService.buscarClientesModificados(token, cursorInicial, offset)
            }.getOrElse { return Result.failure(it) }

            aplicarRemotos(pagina.clientes)
            pagina.ultimoUpdatedAt?.let { maiorUpdatedAt = it }

            if (!pagina.cheia) break
            offset += pagina.clientes.size
        }

        // O cursor só avança depois que TODAS as páginas foram aplicadas com sucesso.
        val novoCursor = maiorUpdatedAt
        if (novoCursor != null && novoCursor != cursorInicial) {
            prefs.edit().putString(KEY_CURSOR, novoCursor).apply()
        }
        return Result.success(Unit)
    }

    private suspend fun aplicarRemotos(remotos: List<Cliente>) {
        for (remoto in remotos) {
            val local = dao.buscarPorUuid(remoto.uuid)

            if (remoto.isDeleted) {
                // Exclusão feita em outro aparelho/na web: remove daqui também.
                if (local != null) dao.excluirFisicoPorUuid(remoto.uuid)
                continue
            }

            if (local == null) {
                dao.inserir(remoto.copy(id = 0, syncStatus = SyncStatus.SYNCED))
                continue
            }

            // Exclusão local ainda não enviada: não ressuscitar.
            if (local.syncStatus == SyncStatus.PENDING_DELETE) continue

            // Local sem pendências -> a nuvem manda. Local com edição pendente -> vence a mais recente.
            if (local.syncStatus == SyncStatus.SYNCED || remoto.atualizadoEm >= local.atualizadoEm) {
                val novo = remoto.copy(
                    id = local.id,
                    syncStatus = SyncStatus.SYNCED,
                    lastSyncedAt = local.lastSyncedAt
                )
                if (novo != local) dao.atualizar(novo) // evita regravar (e redesenhar a lista) sem mudança real
            }
        }
    }

    private suspend fun enviar(cliente: Cliente): Result<Unit> {
        return if (cliente.syncStatus == SyncStatus.PENDING_DELETE) {
            comToken { token -> apiService.excluirCliente(token, cliente.uuid, cliente.atualizadoEm) }
                .onSuccess { dao.excluirFisicoSePendenteDelete(cliente.uuid) }
        } else {
            comToken { token -> apiService.upsertCliente(token, cliente) }
                .onSuccess {
                    dao.marcarSincronizadoSeInalterado(cliente.id, cliente.atualizadoEm, cliente.syncStatus)
                }
        }
    }

    /** Nova tentativa automática com espera crescente (15 s, 30 s, 60 s... até 5 min). */
    private fun agendarRetry() {
        if (!retryAgendado.compareAndSet(false, true)) return
        val espera = minOf(BASE_RETRY_MS shl minOf(tentativasFalhas, 5), MAX_RETRY_MS)
        tentativasFalhas++
        scope.launch {
            delay(espera)
            retryAgendado.set(false)
            sincronizar()
        }
    }

    companion object {
        private const val KEY_CURSOR = "cursor_updated_at"
        private const val INTERVALO_PADRAO_MS = 20_000L
        private const val BASE_RETRY_MS = 15_000L
        private const val MAX_RETRY_MS = 300_000L
    }
}
