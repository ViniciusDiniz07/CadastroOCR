package com.cadastroocr.app.ai

import com.cadastroocr.app.ocr.DadosExtraidos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Chama o backend próprio (NUNCA a API da Gemini direto do celular — ver
 * AiConfig.kt) para interpretar o texto do OCR com IA. O backend (Cloudflare
 * Worker, ver /backend) devolve um JSON com os mesmos campos de DadosExtraidos.
 *
 * Este cliente não sabe (e não precisa saber) se o texto veio de um
 * documento ou de uma conversa — quem decide isso é o próprio prompt do
 * backend, olhando o texto.
 *
 * Nunca lança para o chamador em caso de erro de rede/timeout: retorna null,
 * e quem chamou decide usar o parser de regras como fallback
 * (ver ClienteViewModel.interpretarTexto).
 */
object GeminiInterpreter {

    private const val TIMEOUT_MS = 15_000

    suspend fun interpretar(texto: String): DadosExtraidos? = withContext(Dispatchers.IO) {
        if (!AiConfig.configurado() || texto.isBlank()) return@withContext null

        try {
            val url = URL(AiConfig.BACKEND_URL)
            val conexao = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("x-app-secret", AiConfig.APP_SHARED_SECRET)
            }

            val corpo = JSONObject().put("texto", texto).toString()
            OutputStreamWriter(conexao.outputStream, Charsets.UTF_8).use { it.write(corpo) }

            val codigo = conexao.responseCode
            if (codigo != HttpURLConnection.HTTP_OK) {
                conexao.disconnect()
                return@withContext null
            }

            val respostaTexto = conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conexao.disconnect()
            parsearResposta(respostaTexto, texto)
        } catch (e: Exception) {
            // Sem internet, timeout, backend fora do ar, etc. — o chamador cai pro parser local.
            null
        }
    }

    private fun parsearResposta(json: String, textoBruto: String): DadosExtraidos? {
        return try {
            val obj = JSONObject(json)
            val dados = obj.optJSONObject("dados") ?: obj
            fun campo(nome: String) = dados.optString(nome, "").trim()

            DadosExtraidos(
                nomeCompleto = campo("nomeCompleto"),
                cpf = campo("cpf"),
                dataNascimento = campo("dataNascimento"),
                nomeDaMae = campo("nomeDaMae"),
                nomeDoPai = campo("nomeDoPai"),
                rg = campo("rg"),
                dataExpedicaoRg = campo("dataExpedicaoRg"),
                email = campo("email"),
                telefone = campo("telefone"),
                cep = campo("cep"),
                rua = campo("rua"),
                numero = campo("numero"),
                bairro = campo("bairro"),
                cidade = campo("cidade"),
                textoBrutoReconhecido = textoBruto
            )
        } catch (e: Exception) {
            null
        }
    }
}
