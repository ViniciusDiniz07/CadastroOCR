package com.cadastroocr.app.auth

import android.content.Context
import android.content.SharedPreferences
import com.cadastroocr.app.sync.SupabaseApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class EstadoAuth {
    data object Desconectado : EstadoAuth()
    data class Autenticado(val email: String) : EstadoAuth()
    data object ModoOffline : EstadoAuth()
}

class AuthManager(context: Context, private val apiService: SupabaseApiService) {

    private val prefs: SharedPreferences = context.getSharedPreferences("cadastro_ocr_auth", Context.MODE_PRIVATE)

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

    suspend fun login(email: String, senha: String): Result<Unit> {
        if (!AuthConfig.configurado()) {
            // Se ainda não configurou credenciais na nuvem, permite entrar em modo local/offline
            ativarModoOffline()
            return Result.success(Unit)
        }

        val resultado = apiService.login(email, senha)
        return if (resultado.isSuccess) {
            val (token, refresh, userEmail) = resultado.getOrThrow()
            salvarSessao(token, refresh, userEmail)
            _estadoAuth.value = EstadoAuth.Autenticado(userEmail)
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
        return if (resultado.isSuccess) {
            val (token, refresh, userEmail) = resultado.getOrThrow()
            if (token.isNotBlank()) {
                salvarSessao(token, refresh, userEmail)
                _estadoAuth.value = EstadoAuth.Autenticado(userEmail)
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

    private fun salvarSessao(token: String, refresh: String, email: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_REFRESH, refresh)
            .putString(KEY_EMAIL, email)
            .putBoolean(KEY_MODO_OFFLINE, false)
            .apply()
    }

    companion object {
        private const val KEY_TOKEN = "jwt_access_token"
        private const val KEY_REFRESH = "jwt_refresh_token"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_MODO_OFFLINE = "is_offline_mode"
    }
}
