package com.cadastroocr.app.auth

import android.content.Context
import android.content.SharedPreferences
import com.cadastroocr.app.sync.ApiHttpException
import com.cadastroocr.app.sync.Sessao
import com.cadastroocr.app.sync.SupabaseApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class EstadoAuth {
    data object Desconectado : EstadoAuth()
    data class Autenticado(val email: String) : EstadoAuth()
    data object ModoOffline : EstadoAuth()
}

/** Sessão encerrada de verdade (refresh token recusado pelo servidor): é preciso entrar de novo. */
class SessaoExpiradaException : Exception("Sessão expirada. Entre novamente.")

class AuthManager(context: Context, private val apiService: SupabaseApiService) {

    private val prefs: SharedPreferences = context.getSharedPreferences("cadastro_ocr_auth", Context.MODE_PRIVATE)
    private val renovacaoMutex = Mutex()

    private val _estadoAuth = MutableStateFlow<EstadoAuth>(obterEstadoInicial())
    val estadoAuth: StateFlow<EstadoAuth> = _estadoAuth

    private fun obterEstadoInicial(): EstadoAuth {
        val token = prefs.getString(KEY_TOKEN, null)
        val email = prefs.getString(KEY_EMAIL, null)
        val modoOffline = prefs.getBoolean(KEY_MODO_OFFLINE, false)

        return when {
            modoOffline -> EstadoAuth.ModoOffline
            !token.isNullOrBlank() && !email.isNullOrBlank() -> EstadoAuth.Autenticado(email)
            else -> EstadoAuth.Desconectado
        }
    }

    fun estaAutenticado(): Boolean {
        return _estadoAuth.value is EstadoAuth.Autenticado || _estadoAuth.value is EstadoAuth.ModoOffline
    }

    fun obterToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun obterEmail(): String? = prefs.getString(KEY_EMAIL, null)

    /**
     * Devolve um access token válido, renovando-o com o refresh token quando estiver perto de expirar.
     *
     * - Falha de REDE ao renovar: mantém a sessão (o usuário NÃO é deslogado); a sincronização tenta de novo depois.
     * - Refresh token recusado pelo servidor (400/401/403): aí sim a sessão acabou -> [SessaoExpiradaException].
     *
     * [tokenRecusado]: informe o token que acabou de levar 401 para forçar a renovação
     * (se outra chamada já renovou nesse meio tempo, o token novo é reaproveitado).
     */
    suspend fun obterTokenValido(tokenRecusado: String? = null): Result<String> {
        val atualRapido = prefs.getString(KEY_TOKEN, null)
        if (atualRapido.isNullOrBlank()) return Result.failure(SessaoExpiradaException())
        if (tokenRecusado == null && !tokenPrecisaRenovar()) return Result.success(atualRapido)

        return renovacaoMutex.withLock {
            val atual = prefs.getString(KEY_TOKEN, null)
            if (atual.isNullOrBlank()) return@withLock Result.failure(SessaoExpiradaException())

            // Outra corrotina já renovou enquanto esperávamos o lock.
            if (tokenRecusado != null && atual != tokenRecusado) return@withLock Result.success(atual)
            if (tokenRecusado == null && !tokenPrecisaRenovar()) return@withLock Result.success(atual)

            val refresh = prefs.getString(KEY_REFRESH, null)
            if (refresh.isNullOrBlank()) {
                encerrarSessaoExpirada()
                return@withLock Result.failure(SessaoExpiradaException())
            }

            val resultado = apiService.renovarToken(refresh)
            val sessao = resultado.getOrNull()
            if (sessao != null) {
                salvarSessao(sessao)
                Result.success(sessao.accessToken)
            } else {
                val erro = resultado.exceptionOrNull()
                if (erro is ApiHttpException && erro.codigo in CODIGOS_SESSAO_RECUSADA) {
                    encerrarSessaoExpirada()
                    Result.failure(SessaoExpiradaException())
                } else {
                    Result.failure(erro ?: Exception("Falha ao renovar a sessão"))
                }
            }
        }
    }

    private fun tokenPrecisaRenovar(): Boolean {
        val expiraEm = prefs.getLong(KEY_EXPIRA_EM, 0L)
        return System.currentTimeMillis() >= expiraEm - MARGEM_RENOVACAO_MS
    }

    suspend fun login(email: String, senha: String): Result<Unit> {
        if (!AuthConfig.configurado()) {
            // Se ainda não configurou credenciais na nuvem, permite entrar em modo local/offline
            ativarModoOffline()
            return Result.success(Unit)
        }

        val resultado = apiService.login(email, senha)
        val sessao = resultado.getOrNull()
        return if (sessao != null) {
            salvarSessao(sessao)
            _estadoAuth.value = EstadoAuth.Autenticado(sessao.email)
            Result.success(Unit)
        } else {
            Result.failure(resultado.exceptionOrNull() ?: Exception("Erro desconhecido"))
        }
    }

    suspend fun cadastrar(email: String, senha: String): Result<Unit> {
        if (!AuthConfig.configurado()) {
            ativarModoOffline()
            return Result.success(Unit)
        }

        val resultado = apiService.cadastrar(email, senha)
        val sessao = resultado.getOrNull()
        return if (sessao != null) {
            if (sessao.accessToken.isNotBlank()) {
                salvarSessao(sessao)
                _estadoAuth.value = EstadoAuth.Autenticado(sessao.email)
            }
            Result.success(Unit)
        } else {
            Result.failure(resultado.exceptionOrNull() ?: Exception("Erro ao cadastrar"))
        }
    }

    fun ativarModoOffline() {
        prefs.edit().putBoolean(KEY_MODO_OFFLINE, true).apply()
        _estadoAuth.value = EstadoAuth.ModoOffline
    }

    fun logout() {
        prefs.edit().clear().apply()
        _estadoAuth.value = EstadoAuth.Desconectado
    }

    private fun encerrarSessaoExpirada() {
        // Apaga só as credenciais; os clientes continuam salvos no aparelho (Room).
        prefs.edit().clear().apply()
        _estadoAuth.value = EstadoAuth.Desconectado
    }

    private fun salvarSessao(sessao: Sessao) {
        val editor = prefs.edit()
            .putString(KEY_TOKEN, sessao.accessToken)
            .putString(KEY_EMAIL, sessao.email.ifBlank { prefs.getString(KEY_EMAIL, "") ?: "" })
            .putLong(KEY_EXPIRA_EM, sessao.expiraEmMs)
            .putBoolean(KEY_MODO_OFFLINE, false)
        // O Supabase rotaciona o refresh token a cada renovação: sempre guardar o mais novo.
        if (sessao.refreshToken.isNotBlank()) editor.putString(KEY_REFRESH, sessao.refreshToken)
        editor.apply()
    }

    companion object {
        private const val KEY_TOKEN = "jwt_access_token"
        private const val KEY_REFRESH = "jwt_refresh_token"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_MODO_OFFLINE = "is_offline_mode"
        private const val KEY_EXPIRA_EM = "jwt_expira_em_ms"

        /** Renova 60 s antes de expirar, para não enviar uma requisição com token vencendo. */
        private const val MARGEM_RENOVACAO_MS = 60_000L

        /** Respostas do Auth que significam "este refresh token não vale mais". */
        private val CODIGOS_SESSAO_RECUSADA = setOf(400, 401, 403)
    }
}
