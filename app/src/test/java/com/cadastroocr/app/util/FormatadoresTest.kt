package com.cadastroocr.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FormatadoresTest {

    @Test
    fun cpfComQuartoDigitoPreservaCursorAposMascara() {
        assertEquals("123.4" to 5, CpfValidator.formatarComCursor("1234", 4))
    }

    @Test
    fun cepComOitoDigitosGanhaMascara() {
        assertEquals("27200-000", CepValidator.formatar("27200000"))
    }

    @Test
    fun telefoneCelularGanhaMascara() {
        assertEquals("(24) 99999-9999", TelefoneValidator.formatar("24999999999"))
    }

    @Test
    fun dataCompactaGanhaSeparadores() {
        assertEquals("01/01/2000", DataValidator.formatar("01012000"))
    }

    @Test
    fun dataInexistenteNaoEValida() {
        assertFalse(DataValidator.isValida("31/02/2000"))
    }
}
