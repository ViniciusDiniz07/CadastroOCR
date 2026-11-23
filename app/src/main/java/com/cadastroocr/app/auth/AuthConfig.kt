package com.cadastroocr.app.auth

/**
 * Configurações de conexão com o banco de dados online (Supabase).
 * 
 * Obtenha estas credenciais no painel do Supabase:
 * Project Settings -> API -> Project URL e anon / public key.
 *
 * A 'anon key' é pública e segura para uso no app porque todas as operações
 * são protegidas por Row Level Security (RLS) no PostgreSQL.
 */
object AuthConfig {
    // Insira aqui a URL do seu projeto Supabase (ex: "https://xyzcompany.supabase.co")
    const val SUPABASE_URL = "https://SEU_PROJETO.supabase.co"

    // Insira aqui a Anon Public Key do seu projeto Supabase
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

    fun configurado(): Boolean {
        return SUPABASE_URL.startsWith("https://") &&
                !SUPABASE_URL.contains("SEU_PROJETO") &&
                SUPABASE_ANON_KEY.length > 20
    }
}
