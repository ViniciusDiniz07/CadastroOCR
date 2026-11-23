package com.cadastroocr.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {

    @Query("SELECT * FROM clientes WHERE isDeleted = 0 ORDER BY nomeCompleto ASC")
    fun observarTodos(): Flow<List<Cliente>>

    /**
     * Pesquisa por nome, CPF, e-mail ou telefone.
     */
    @Query(
        """
        SELECT * FROM clientes
        WHERE isDeleted = 0
          AND (nomeCompleto LIKE '%' || :termo || '%'
            OR cpf LIKE '%' || :termo || '%'
            OR email LIKE '%' || :termo || '%'
            OR telefone LIKE '%' || :termo || '%')
        ORDER BY nomeCompleto ASC
        """
    )
    fun pesquisar(termo: String): Flow<List<Cliente>>

    @Query("SELECT * FROM clientes WHERE id = :id AND isDeleted = 0")
    suspend fun buscarPorId(id: Long): Cliente?

    @Query("SELECT * FROM clientes WHERE uuid = :uuid LIMIT 1")
    suspend fun buscarPorUuid(uuid: String): Cliente?

    @Query("SELECT * FROM clientes WHERE syncStatus != 'SYNCED'")
    suspend fun buscarPendentes(): List<Cliente>

    @Query("SELECT MAX(lastSyncedAt) FROM clientes")
    suspend fun obterUltimoTimestampSincronizacao(): Long?

    @Query("SELECT COUNT(*) FROM clientes WHERE syncStatus != 'SYNCED'")
    fun observarQuantidadePendentes(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(cliente: Cliente): Long

    @Update
    suspend fun atualizar(cliente: Cliente)

    @Query("UPDATE clientes SET syncStatus = :status, lastSyncedAt = :syncedAt WHERE id = :id")
    suspend fun atualizarStatusSincronizacao(id: Long, status: String, syncedAt: Long = System.currentTimeMillis())

    @Query("UPDATE clientes SET isDeleted = 1, syncStatus = 'PENDING_DELETE', atualizadoEm = :atualizadoEm WHERE id = :id")
    suspend fun marcarComoExcluido(id: Long, atualizadoEm: Long = System.currentTimeMillis())

    @Query("DELETE FROM clientes WHERE uuid = :uuid")
    suspend fun excluirFisicoPorUuid(uuid: String)

    @Delete
    suspend fun excluirFisico(cliente: Cliente)
}

