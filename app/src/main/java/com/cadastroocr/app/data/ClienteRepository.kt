package com.cadastroocr.app.data

import com.cadastroocr.app.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ClienteRepository(
    private val dao: ClienteDao,
    val syncManager: SyncManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun observarClientes(termoBusca: String): Flow<List<Cliente>> {
        return if (termoBusca.isBlank()) dao.observarTodos() else dao.pesquisar(termoBusca.trim())
    }

    fun observarQuantidadePendentes(): Flow<Int> = dao.observarQuantidadePendentes()

    suspend fun buscarPorId(id: Long): Cliente? = dao.buscarPorId(id)

    suspend fun salvar(cliente: Cliente): Long {
        val idSalvo = if (cliente.id == 0L) {
            val novo = cliente.copy(
                syncStatus = SyncStatus.PENDING_INSERT,
                criadoEm = System.currentTimeMillis(),
                atualizadoEm = System.currentTimeMillis()
            )
            dao.inserir(novo)
        } else {
            val atualizado = cliente.copy(
                syncStatus = SyncStatus.PENDING_UPDATE,
                atualizadoEm = System.currentTimeMillis()
            )
            dao.atualizar(atualizado)
            cliente.id
        }

        // Dispara sincronização em segundo plano assim que salva
        coroutineScope.launch {
            syncManager.sincronizar()
        }

        return idSalvo
    }

    suspend fun excluir(cliente: Cliente) {
        dao.marcarComoExcluido(cliente.id, System.currentTimeMillis())
        coroutineScope.launch {
            syncManager.sincronizar()
        }
    }

    suspend fun sincronizarAgora(): Result<Unit> = syncManager.sincronizar()
}

