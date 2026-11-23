package com.cadastroocr.app.ai

/**
 * Configuração da interpretação por IA remota.
 *
 * IMPORTANTE — preencha estes dois valores DEPOIS de fazer o deploy do
 * backend (ver /backend/README.md):
 *
 * 1. BACKEND_URL: a URL do seu Worker, algo como
 *    "https://cadastro-ocr-backend.SEU-USUARIO.workers.dev"
 *
 * 2. APP_SHARED_SECRET: uma senha própria do APP (NÃO é a API key da
 *    Gemini!) que você escolhe e configura tanto aqui quanto no backend
 *    (variável de ambiente APP_SHARED_SECRET). Ela serve só para dificultar
 *    que outra pessoa descubra sua URL e fique consumindo sua cota da
 *    Gemini de graça — não é uma proteção perfeita (ela fica dentro do
 *    APK), mas já impede abuso casual. Para uma proteção mais forte no
 *    futuro, veja Firebase App Check.
 *
 * A API key de verdade da Gemini NUNCA fica no app — ela mora só no
 * backend (ver /backend/functions/index.js).
 */
object AiConfig {
    const val BACKEND_URL = "https://cadastro-ocr-backend.wwwvinidiniz.workers.dev"
    const val APP_SHARED_SECRET = "Vini2014*"

    /** Enquanto BACKEND_URL não for configurada, a IA fica desativada e o app usa só o parser local. */
    fun configurado(): Boolean = !BACKEND_URL.contains("SEU-BACKEND-AQUI")
}
