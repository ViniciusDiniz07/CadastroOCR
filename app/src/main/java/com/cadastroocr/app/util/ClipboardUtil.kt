package com.cadastroocr.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Copia SOMENTE o valor de um campo específico (nunca todos os dados juntos),
 * conforme a Seção 8 do briefing: cópia exclusivamente individual por campo.
 */
object ClipboardUtil {
    fun copiarCampo(context: Context, rotulo: String, valor: String) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText(rotulo, valor))
    }
}
