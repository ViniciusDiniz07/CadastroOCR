package com.cadastroocr.app.ocr

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A imagem do documento é usada SOMENTE para o processamento do OCR.
 * Ela fica em cache/documentos/ (área privada do app, nunca em galeria
 * pública) e deve ser apagada logo após o texto ser extraído — ver
 * limparImagemTemporaria(). Nada aqui é enviado para servidores externos:
 * o reconhecimento roda 100% no aparelho via ML Kit on-device (Seção 15).
 */
object DocumentImageStore {

    private fun pastaTemp(context: Context): File {
        val dir = File(context.cacheDir, "documentos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun novoArquivoTemporario(context: Context): File {
        val nomeArquivo = "doc_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        return File(pastaTemp(context), nomeArquivo)
    }

    fun uriParaArquivo(context: Context, arquivo: File) =
        FileProvider.getUriForFile(context, "com.cadastroocr.app.fileprovider", arquivo)

    /** Chamado assim que o OCR termina de ler a imagem — a foto não precisa mais existir. */
    fun limparImagemTemporaria(arquivo: File?) {
        arquivo?.let { if (it.exists()) it.delete() }
    }

    /** Limpa qualquer resíduo de fotos antigas (ex.: app fechado no meio do fluxo). */
    fun limparTudo(context: Context) {
        pastaTemp(context).listFiles()?.forEach { it.delete() }
    }
}
