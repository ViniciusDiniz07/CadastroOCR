package com.cadastroocr.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entidade principal do app. Representa um cliente cadastrado, seja via OCR
 * ou digitação manual. Todos os campos de texto são opcionais na tabela
 * (Strings vazias) porque o OCR pode não encontrar todos os dados.
 *
 * Inclui identificador único universal (uuid) e metadados para sincronização
 * offline/online em tempo real com o banco de dados na nuvem (Supabase).
 */
@Entity(
    tableName = "clientes",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["cpf"]),
        Index(value = ["isDeleted"])
    ]
)
data class Cliente(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),

    // Dados pessoais
    val nomeCompleto: String = "",
    val cpf: String = "",
    val dataNascimento: String = "",
    val nomeDaMae: String = "",
    val nomeDoPai: String = "",

    // Documento
    val rg: String = "",
    val dataExpedicaoRg: String = "",

    // Contato
    val email: String = "",
    val telefone: String = "",

    // Endereço
    val rua: String = "",
    val numero: String = "",
    val complemento: String = "",
    val cep: String = "",
    val bairro: String = "",
    val cidade: String = "",

    // Plano e pagamento
    val plano: String = "",
    val valorPlano: String = "",
    val formaPagamento: String = "",
    val diaVencimento: String = "",

    // Sincronização e Auditoria
    val isDeleted: Boolean = false,
    val syncStatus: String = SyncStatus.PENDING_INSERT, // SYNCED, PENDING_INSERT, PENDING_UPDATE, PENDING_DELETE
    val lastSyncedAt: Long = 0L,
    val criadoEm: Long = System.currentTimeMillis(),
    val atualizadoEm: Long = System.currentTimeMillis()
)

object SyncStatus {
    const val SYNCED = "SYNCED"
    const val PENDING_INSERT = "PENDING_INSERT"
    const val PENDING_UPDATE = "PENDING_UPDATE"
    const val PENDING_DELETE = "PENDING_DELETE"
}

