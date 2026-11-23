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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class EstadoSincronizacao {
    data object Sincronizado : EstadoSincronizacao()
    data class Sincronizando(val progresso: String = "Sincronizando...") : EstadoSincronizacao()
    data object Offline : EstadoSincronizacao()
    data object ModoLocal : EstadoSincronizacao()
    data class Pendente(val quantidade: Int) : EstadoSincronizacao()
    data class Erro(val motivo: String) : EstadoSincronizacao()
}

class SyncManager(
    private val context: Context,
    private val dao: ClienteDao,
    private val apiService: SupabaseApiService,
    private val authManager: AuthManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _estadoSincronizacao = MutableStateFlow<EstadoSincronizacao>(EstadoSincronizacao.Sincronizado)
    val estadoSincronizacao: StateFlow<EstadoSincronizacao> = _estadoSincronizacao.asStateFlow()

    private var estaSincronizando = false

    init {
        registrarMonitorRede()
    }

    private fun registrarMonitorRede() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Ao recuperar internet, dispara sincronização automática das alterações pendentes
                scope.launch {
                    sincronizar()
                }
            }

            override fun onLost(network: Network) {
                _estadoSincronizacao.value = EstadoSincronizacao.Offline
            }
        })
    }

    fun temInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val rede = cm.activeNetwork ?: return false
        val cap = cm.getNetworkCapabilities(rede) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun sincronizar(): Result<Unit> = withContext(Dispatchers.IO) {
        if (estaSincronizando) return@withContext Result.success(Unit)

        if (!AuthConfig.configurado() || authManager.estadoAuth.value is EstadoAuth.ModoOffline) {
            _estadoSincronizacao.value = EstadoSincronizacao.ModoLocal
            return@withContext Result.success(Unit)
        }

        if (!temInternet()) {
            _estadoSincronizacao.value = EstadoSincronizacao.Offline
            return@withContext Result.failure(Exception("Sem conexão com a internet"))
        }

        val token = authManager.obterToken()
        if (token.isNullOrBlank()) {
            _estadoSincronizacao.value = EstadoSincronizacao.Erro("Usuário não autenticado")
            return@withContext Result.failure(Exception("Usuário não autenticado"))
        }

        estaSincronizando = true
        _estadoSincronizacao.value = EstadoSincronizacao.Sincronizando("Enviando alterações pendentes...")

        try {
            // 1. FASE DE PUSH: Envia cadastros, edições e exclusões locais pendentes
            val pendentes = dao.buscarPendentes()
            for (cliente in pendentes) {
                when (cliente.syncStatus) {
                    SyncStatus.PENDING_DELETE -> {
                        val res = apiService.excluirCliente(token, cliente.uuid)
                        if (res.isSuccess) {
                            dao.excluirFisicoPorUuid(cliente.uuid)
                        }
                    }
                    SyncStatus.PENDING_INSERT, SyncStatus.PENDING_UPDATE -> {
                        val res = apiService.upsertCliente(token, cliente)
                        if (res.isSuccess) {
                            dao.atualizarStatusSincronizacao(cliente.id, SyncStatus.SYNCED, System.currentTimeMillis())
                        }
                    }
                }
            }

            // 2. FASE DE PULL: Baixa novos clientes e atualizações da nuvem
            _estadoSincronizacao.value = EstadoSincronizacao.Sincronizando("Buscando atualizações da nuvem...")
            val ultimoSync = dao.obterUltimoTimestampSincronizacao() ?: 0L
            // Busca alterações dos últimos 30 dias se for primeiro sync, ou a partir do último sync
            val pontoDeBusca = if (ultimoSync > 0L) ultimoSync - 5000L else 0L

            val resultadoPull = apiService.buscarClientesModificados(token, pontoDeBusca)
            if (resultadoPull.isSuccess) {
                val clientesRemotos = resultadoPull.getOrThrow()
                for (remoto in clientesRemotos) {
                    val local = dao.buscarPorUuid(remoto.uuid)

                    if (remoto.isDeleted) {
                        if (local != null) {
                            dao.excluirFisicoPorUuid(remoto.uuid)
                        }
                        continue
                    }

                    if (local == null) {
                        // Novo cliente criado no PC ou em outro celular -> salva no Room
                        dao.inserir(remoto.copy(id = 0, syncStatus = SyncStatus.SYNCED, lastSyncedAt = System.currentTimeMillis()))
                    } else {
                        // Cliente já existe localmente: resolve conflitos de campo inteligente
                        if (local.syncStatus == SyncStatus.SYNCED || remoto.atualizadoEm >= local.atualizadoEm) {
                            val clienteAtualizado = remoto.copy(
                                id = local.id,
                                syncStatus = SyncStatus.SYNCED,
                                lastSyncedAt = System.currentTimeMillis()
                            )
                            dao.atualizar(clienteAtualizado)
                        }
                    }
                }
            }

            _estadoSincronizacao.value = EstadoSincronizacao.Sincronizado
            Result.success(Unit)
        } catch (e: Exception) {
            _estadoSincronizacao.value = EstadoSincronizacao.Erro(e.message ?: "Erro na sincronização")
            Result.failure(e)
        } finally {
            estaSincronizando = false
        }
    }
}
