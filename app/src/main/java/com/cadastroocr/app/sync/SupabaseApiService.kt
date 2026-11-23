package com.cadastroocr.app.sync

import com.cadastroocr.app.auth.AuthConfig
import com.cadastroocr.app.data.Cliente
import com.cadastroocr.app.data.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Erro HTTP devolvido pelo Supabase (permite distinguir 401 = token expirado de falha de rede). */
class ApiHttpException(val codigo: Int, mensagem: String) : Exception(mensagem)

/** Sessão devolvida pelo Supabase Auth. [expiraEmMs] é a hora (do celular) em que o access token expira. */
data class Sessao(
    val accessToken: String,
    val refreshToken: String,
    val email: String,
    val expiraEmMs: Long
)

/**
 * Uma página do pull. [ultimoUpdatedAt] é o `updated_at` (relógio do SERVIDOR) da última linha
 * recebida e serve de cursor para a próxima sincronização.
 */
data class PaginaClientes(
    val clientes: List<Cliente>,
    val ultimoUpdatedAt: String?,
    val cheia: Boolean
)

/**
 * Cliente HTTP direto e leve para a API do Supabase (REST e Auth),
 * utilizando OkHttp. Não necessita de SDKs externos pesados.
 */
class SupabaseApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getBaseUrl(): String = AuthConfig.SUPABASE_URL.trimEnd('/')

    private fun mensagemDeErro(corpo: String, codigo: Int, padrao: String): String {
        val json = try { JSONObject(corpo) } catch (e: Exception) { null }
        val msg = json?.let {
            it.optString("error_description", "")
                .ifBlank { it.optString("msg", "") }
                .ifBlank { it.optString("message", "") }
        }.orEmpty()
        return if (msg.isNotBlank()) msg else "$padrao (código $codigo)"
    }

    private fun parseSessao(json: JSONObject, emailPadrao: String): Sessao {
        val expiresIn = json.optLong("expires_in", 3600L)
        val email = json.optJSONObject("user")?.optString("email", emailPadrao) ?: emailPadrao
        return Sessao(
            accessToken = json.optString("access_token", ""),
            refreshToken = json.optString("refresh_token", ""),
            email = email,
            expiraEmMs = System.currentTimeMillis() + expiresIn * 1000L
        )
    }

    private fun postAuth(url: String, corpo: JSONObject, emailPadrao: String, padraoErro: String): Result<Sessao> {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", AuthConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(corpo.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(request).execute().use { response ->
                val texto = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(parseSessao(JSONObject(texto), emailPadrao))
                } else {
                    Result.failure(ApiHttpException(response.code, mensagemDeErro(texto, response.code, padraoErro)))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Login por e-mail e senha (GoTrue). */
    suspend fun login(email: String, password: String): Result<Sessao> = withContext(Dispatchers.IO) {
        if (!AuthConfig.configurado()) {
            return@withContext Result.failure(Exception("Supabase ainda não configurado em AuthConfig.kt"))
        }
        val corpo = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
        }
        postAuth("${getBaseUrl()}/auth/v1/token?grant_type=password", corpo, email, "Falha na autenticação")
            .mapCatching { if (it.accessToken.isBlank()) error("Resposta de login sem token") else it }
    }

    /** Cadastra um novo usuário (só funciona se o signup estiver habilitado no painel do Supabase). */
    suspend fun cadastrar(email: String, password: String): Result<Sessao> = withContext(Dispatchers.IO) {
        if (!AuthConfig.configurado()) {
            return@withContext Result.failure(Exception("Supabase ainda não configurado em AuthConfig.kt"))
        }
        val corpo = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
        }
        postAuth("${getBaseUrl()}/auth/v1/signup", corpo, email, "Falha no cadastro")
    }

    /** Troca o refresh token por um novo par de tokens. */
    suspend fun renovarToken(refreshToken: String): Result<Sessao> = withContext(Dispatchers.IO) {
        if (!AuthConfig.configurado()) {
            return@withContext Result.failure(Exception("Supabase ainda não configurado em AuthConfig.kt"))
        }
        val corpo = JSONObject().apply { put("refresh_token", refreshToken) }
        postAuth("${getBaseUrl()}/auth/v1/token?grant_type=refresh_token", corpo, "", "Falha ao renovar a sessão")
            .mapCatching { if (it.accessToken.isBlank()) error("Resposta de renovação sem token") else it }
    }

    /**
     * Pull incremental paginado. Filtra pelo `updated_at` do SERVIDOR (não pelo relógio do celular),
     * incluindo registros apagados (is_deleted = true) para que a exclusão chegue aos outros aparelhos.
     * [desde] é o cursor guardado (inclusivo: um registro de borda pode vir de novo, o que é inofensivo).
     */
    suspend fun buscarClientesModificados(
        accessToken: String,
        desde: String?,
        offset: Int
    ): Result<PaginaClientes> = withContext(Dispatchers.IO) {
        if (!AuthConfig.configurado()) {
            return@withContext Result.success(PaginaClientes(emptyList(), null, false))
        }
        try {
            val builder = "${getBaseUrl()}/rest/v1/clientes".toHttpUrl().newBuilder()
            if (!desde.isNullOrBlank()) builder.addQueryParameter("updated_at", "gte.$desde")
            builder.addQueryParameter("order", "updated_at.asc,id.asc")
            builder.addQueryParameter("limit", LIMITE_PAGINA.toString())
            builder.addQueryParameter("offset", offset.toString())

            val request = Request.Builder()
                .url(builder.build())
                .addHeader("apikey", AuthConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val texto = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        ApiHttpException(response.code, mensagemDeErro(texto, response.code, "Erro ao buscar clientes da nuvem"))
                    )
                }
                val array = JSONArray(texto)
                val lista = ArrayList<Cliente>(array.length())
                var ultimo: String? = null
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    lista.add(parseClienteJson(obj))
                    obj.optString("updated_at", "").takeIf { it.isNotBlank() }?.let { ultimo = it }
                }
                Result.success(PaginaClientes(lista, ultimo, array.length() >= LIMITE_PAGINA))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Envia (upsert por id/uuid) e só considera sucesso se o servidor DEVOLVER a linha gravada.
     * Assim "enviado" significa "confirmado pelo banco", e não apenas "a requisição saiu".
     */
    suspend fun upsertCliente(accessToken: String, cliente: Cliente): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AuthConfig.configurado()) {
            return@withContext Result.success(Unit)
        }
        try {
            val url = "${getBaseUrl()}/rest/v1/clientes?on_conflict=id"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", AuthConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(clienteToJson(cliente).toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val texto = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        ApiHttpException(response.code, mensagemDeErro(texto, response.code, "Erro ao enviar cliente"))
                    )
                }
                val confirmado = try {
                    val arr = JSONArray(texto)
                    arr.length() >= 1 && arr.getJSONObject(0).optString("id") == cliente.uuid
                } catch (e: Exception) {
                    false
                }
                if (confirmado) Result.success(Unit)
                else Result.failure(Exception("O servidor não confirmou a gravação do cliente"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exclusão LÓGICA (is_deleted = true). Um DELETE físico faria o registro sumir do servidor sem
     * deixar rastro, e os outros aparelhos nunca ficariam sabendo da exclusão.
     * Se a linha não existir mais no servidor, a chamada retorna 2xx sem linhas — também é sucesso.
     */
    suspend fun excluirCliente(accessToken: String, uuid: String, atualizadoEm: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AuthConfig.configurado()) {
            return@withContext Result.success(Unit)
        }
        try {
            val url = "${getBaseUrl()}/rest/v1/clientes?id=eq.$uuid"
            val corpo = JSONObject().apply {
                put("is_deleted", true)
                put("atualizado_em", atualizadoEm)
            }
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", AuthConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(corpo.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val texto = response.body?.string().orEmpty()
                    Result.failure(ApiHttpException(response.code, mensagemDeErro(texto, response.code, "Erro ao excluir cliente")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun clienteToJson(cliente: Cliente): JSONObject {
        return JSONObject().apply {
            put("id", cliente.uuid)
            put("nome_completo", cliente.nomeCompleto)
            put("cpf", cliente.cpf)
            put("data_nascimento", cliente.dataNascimento)
            put("nome_da_mae", cliente.nomeDaMae)
            put("nome_do_pai", cliente.nomeDoPai)
            put("rg", cliente.rg)
            put("data_expedicao_rg", cliente.dataExpedicaoRg)
            put("email", cliente.email)
            put("telefone", cliente.telefone)
            put("rua", cliente.rua)
            put("numero", cliente.numero)
            put("complemento", cliente.complemento)
            put("cep", cliente.cep)
            put("bairro", cliente.bairro)
            put("cidade", cliente.cidade)
            put("plano", cliente.plano)
            put("valor_plano", cliente.valorPlano)
            put("forma_pagamento", cliente.formaPagamento)
            put("dia_vencimento", cliente.diaVencimento)
            put("is_deleted", cliente.isDeleted)
            put("criado_em", cliente.criadoEm)
            put("atualizado_em", cliente.atualizadoEm)
        }
    }

    private fun parseClienteJson(obj: JSONObject): Cliente {
        fun s(key: String) = obj.optString(key, "").trim()
        return Cliente(
            id = 0L, // o Room gera o id local (ou o SyncManager reaproveita o existente)
            uuid = obj.optString("id", s("uuid")),
            nomeCompleto = s("nome_completo"),
            cpf = s("cpf"),
            dataNascimento = s("data_nascimento"),
            nomeDaMae = s("nome_da_mae"),
            nomeDoPai = s("nome_do_pai"),
            rg = s("rg"),
            dataExpedicaoRg = s("data_expedicao_rg"),
            email = s("email"),
            telefone = s("telefone"),
            rua = s("rua"),
            numero = s("numero"),
            complemento = s("complemento"),
            cep = s("cep"),
            bairro = s("bairro"),
            cidade = s("cidade"),
            plano = s("plano"),
            valorPlano = s("valor_plano"),
            formaPagamento = s("forma_pagamento"),
            diaVencimento = s("dia_vencimento"),
            isDeleted = obj.optBoolean("is_deleted", false),
            syncStatus = SyncStatus.SYNCED,
            lastSyncedAt = 0L,
            criadoEm = obj.optLong("criado_em", System.currentTimeMillis()),
            atualizadoEm = obj.optLong("atualizado_em", System.currentTimeMillis())
        )
    }

    companion object {
        const val LIMITE_PAGINA = 500
    }
}
