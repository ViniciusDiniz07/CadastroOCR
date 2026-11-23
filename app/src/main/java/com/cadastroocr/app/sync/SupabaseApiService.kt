package com.cadastroocr.app.sync

import com.cadastroocr.app.auth.AuthConfig
import com.cadastroocr.app.data.Cliente
import com.cadastroocr.app.data.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP direto e leve para a API do Supabase (REST e Auth),
 * utilizando OkHttp. Não necessita de SDKs externos pesados.
 */
class SupabaseApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getBaseUrl(): String = AuthConfig.SUPABASE_URL.trimEnd('/')

    /**
     * Realiza login no Supabase GoTrue Auth via email e senha.
     * Retorna Triple(accessToken, refreshToken, userEmail) em caso de sucesso.
     */
    suspend fun login(email: String, password: String): Result<Triple<String, String, String>> = withContext(Dispatchers.IO) {
        if (!AuthConfig.configurado()) {
            return@withContext Result.failure(Exception("Supabase ainda não configurado em AuthConfig.kt"))
        }

        try {
            val url = "${getBaseUrl()}/auth/v1/token?grant_type=password"
            val bodyObj = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", AuthConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(bodyObj.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token", "")
                val userObj = json.optJSONObject("user")
                val userEmail = userObj?.optString("email", email) ?: email
                Result.success(Triple(accessToken, refreshToken, userEmail))
            } else {
                val json = try { JSONObject(responseBody) } catch (e: Exception) { null }
                val msg = json?.optString("error_description", "")?.ifBlank { json.optString("msg", "") }
                    ?: "Falha na autenticação (Código ${response.code})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cadastra um novo usuário no Supabase.
     */
    suspend fun cadastrar(email: String, password: String): Result<Triple<String, String, String>> = withContext(Dispatchers.IO) {
        if (!AuthConfig.configurado()) {
            return@withContext Result.failure(Exception("Supabase ainda não configurado em AuthConfig.kt"))
        }

        try {
            val url = "${getBaseUrl()}/auth/v1/signup"
            val bodyObj = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", AuthConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(bodyObj.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")
                val userObj = json.optJSONObject("user")
                val userEmail = userObj?.optString("email", email) ?: email
                Result.success(Triple(accessToken, refreshToken, userEmail))
            } else {
                val json = try { JSONObject(responseBody) } catch (e: Exception) { null }
                val msg = json?.optString("msg", "")?.ifBlank { json.optString("error_description", "") }
                    ?: "Falha no cadastro (Código ${response.code})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Busca todos os clientes atualizados desde o timestamp informado (Pull incremental).
     */
    suspend fun buscarClientesModificados(accessToken: String, desdeTimestamp: Long): Result<List<Cliente>> = withContext(Dispatchers.IO) {
        if (!AuthConfig.configurado()) {
            return@withContext Result.success(emptyList())
        }

        try {
            val url = "${getBaseUrl()}/rest/v1/clientes?atualizado_em=gte.$desdeTimestamp&order=atualizado_em.asc"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", AuthConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val jsonArray = JSONArray(responseBody)
                val lista = mutableListOf<Cliente>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    lista.add(parseClienteJson(obj))
                }
                Result.success(lista)
            } else {
                Result.failure(Exception("Erro ao buscar clientes da nuvem: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Envia cliente para o Supabase (Upsert via POST com resolução de duplicatas por ID/UUID).
     */
    suspend fun upsertCliente(accessToken: String, cliente: Cliente): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AuthConfig.configurado()) {
            return@withContext Result.success(Unit)
        }

        try {
            val url = "${getBaseUrl()}/rest/v1/clientes"
            val bodyJson = clienteToJson(cliente).toString()
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", AuthConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(bodyJson.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val erro = response.body?.string().orEmpty()
                Result.failure(Exception("Erro ao enviar cliente: ${response.code} - $erro"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exclui cliente definitivamente no Supabase ou marca como deletado.
     */
    suspend fun excluirCliente(accessToken: String, uuid: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AuthConfig.configurado()) {
            return@withContext Result.success(Unit)
        }

        try {
            val url = "${getBaseUrl()}/rest/v1/clientes?id=eq.$uuid"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", AuthConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erro ao excluir cliente: ${response.code}"))
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
            id = 0L, // Room autogenerates local id or keeps existing
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
            lastSyncedAt = System.currentTimeMillis(),
            criadoEm = obj.optLong("criado_em", System.currentTimeMillis()),
            atualizadoEm = obj.optLong("atualizado_em", System.currentTimeMillis())
        )
    }
}
