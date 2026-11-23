package com.cadastroocr.app.ocr

import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Camada de OCR real (Etapa 6). Usa o ML Kit Text Recognition, que roda
 * totalmente no aparelho (não envia a imagem para nenhum servidor).
 * Retorna o texto bruto reconhecido; a interpretação de qual dado é qual
 * campo fica no DocumentDataParser.
 */
class OcrAnalyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun reconhecerTexto(arquivo: File): String {
        // downscale defensivo: fotos de câmeras modernas podem ter resolução
        // muito alta; reduzir evita OutOfMemory sem prejudicar o OCR.
        val bitmap = decodificarComEscalaSegura(arquivo)
        val image = InputImage.fromBitmap(bitmap, 0)
        val resultado = recognizer.process(image).await()
        return resultado.text
    }

    suspend fun reconhecerTexto(context: android.content.Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val resultado = recognizer.process(image).await()
        return resultado.text
    }

    /**
     * Usado pela importação de conversa: a foto vem direto da câmera do
     * sistema como um Bitmap em memória (via TakePicturePreview), sem
     * precisar salvar um arquivo temporário no disco.
     */
    suspend fun reconhecerTexto(bitmap: android.graphics.Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val resultado = recognizer.process(image).await()
        return resultado.text
    }

    private fun decodificarComEscalaSegura(arquivo: File): android.graphics.Bitmap {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(arquivo.absolutePath, opts)

        var amostragem = 1
        val alturaAlvo = 2000
        while (opts.outHeight / amostragem > alturaAlvo) amostragem *= 2

        val opts2 = BitmapFactory.Options().apply { inSampleSize = amostragem }
        return BitmapFactory.decodeFile(arquivo.absolutePath, opts2)
    }
}
