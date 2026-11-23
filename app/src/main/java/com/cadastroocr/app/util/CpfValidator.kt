package com.cadastroocr.app.util

object CpfValidator {

    /** Remove tudo que não for dígito. */
    fun apenasDigitos(valor: String): String = valor.filter { it.isDigit() }

    /** Formata 11 dígitos como 000.000.000-00. Se tiver menos dígitos, formata parcialmente. */
    fun formatar(valor: String): String {
        val d = apenasDigitos(valor).take(11)
        val sb = StringBuilder()
        for (i in d.indices) {
            sb.append(d[i])
            when (i) {
                2, 5 -> if (i != d.lastIndex) sb.append('.')
                8 -> if (i != d.lastIndex) sb.append('-')
            }
        }
        return sb.toString()
    }

    /** Valida os dígitos verificadores oficiais do CPF (algoritmo módulo 11). */
    fun isValido(valor: String): Boolean {
        val cpf = apenasDigitos(valor)
        if (cpf.length != 11) return false
        if (cpf.all { it == cpf[0] }) return false // sequências tipo 111.111.111-11

        fun calcularDigito(base: String): Int {
            var soma = 0
            var peso = base.length + 1
            for (c in base) {
                soma += (c - '0') * peso
                peso--
            }
            val resto = soma % 11
            return if (resto < 2) 0 else 11 - resto
        }

        val d1 = calcularDigito(cpf.substring(0, 9))
        val d2 = calcularDigito(cpf.substring(0, 9) + d1)
        return cpf[9] - '0' == d1 && cpf[10] - '0' == d2
    }
}
