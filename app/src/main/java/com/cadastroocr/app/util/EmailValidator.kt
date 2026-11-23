package com.cadastroocr.app.util

import android.util.Patterns

object EmailValidator {
    /** Formato básico de e-mail. Campo vazio é considerado válido (e-mail é opcional). */
    fun isValido(valor: String): Boolean {
        if (valor.isBlank()) return true
        return Patterns.EMAIL_ADDRESS.matcher(valor.trim()).matches()
    }
}
