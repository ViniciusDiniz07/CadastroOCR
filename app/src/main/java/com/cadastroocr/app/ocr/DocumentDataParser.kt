package com.cadastroocr.app.ocr

import com.cadastroocr.app.util.CpfValidator
import java.text.Normalizer

/**
 * Interpreta o texto bruto reconhecido pelo OCR e tenta identificar qual
 * informação corresponde a qual campo do cadastro (Seção 6 do briefing).
 *
 * Estratégia: RG/CIN e CNH têm rótulos previsíveis ("NOME", "CPF",
 * "FILIAÇÃO", etc.) mesmo quando o layout varia. Por isso o parser procura
 * por essas palavras-chave linha a linha e usa a(s) linha(s) seguinte(s)
 * como valor — em vez de despejar o texto inteiro em uma caixa só.
 *
 * Isso é heurístico por natureza (o OCR erra, documentos variam), por isso
 * TODOS os campos continuam editáveis na tela de revisão.
 */
object DocumentDataParser {

    private val REGEX_CPF = Regex("""\d{3}\.?\d{3}\.?\d{3}-?\d{2}""")
    private val REGEX_DATA = Regex("""\d{2}/\d{2}/\s?\d{4}""")
    private val REGEX_NUMERO_DOCUMENTO = Regex("""\d[\d.\-]{5,11}[\dxX]""")

    // Palavras que aparecem no documento mas nunca são, elas mesmas, o valor de um campo.
    // Inclui as versões em inglês porque RGs/CNHs novos trazem rótulos bilíngues
    // lado a lado (ex.: "NOME / NAME"), e sem isso o parser podia confundir a
    // tradução do rótulo com o valor do campo.
    private val PALAVRAS_RUIDO = setOf(
        "REPUBLICA", "REPÚBLICA", "FEDERATIVA", "BRASIL", "CARTEIRA", "IDENTIDADE",
        "NACIONAL", "HABILITACAO", "HABILITAÇÃO", "SECRETARIA", "MINISTERIO", "MINISTÉRIO",
        "VALIDA", "VÁLIDA", "TODO", "TERRITORIO", "TERRITÓRIO", "REGISTRO", "GERAL",
        "NAME", "MOTHER", "FATHER", "FILIATION", "DATE", "BIRTH", "NATURALITY",
        "SIGNATURE", "ISSUING", "IDENTITY CARD", "NATIONAL TERRITORY", "PLACE OF BIRTH",
        "DRIVER", "LICENSE", "CATEGORY", "VALID THROUGHOUT",
        "DETRAN", "ESTADO", "GOVERNO", "CIVIL", "DIRETORIA", "IDENTIFICACAO",
        "ORGAO", "EXPEDIDOR", "PRESIDENTE", "ISSUER", "CARDHOLDER", "ASSINATURA",
        "SOCIAL", "SEXO", "SEX", "NACIONALIDADE", "NATIONALITY", "VALIDADE",
        "EXPIRY", "LOCAL", "ISSUE", "PERSONAL NUMBER", "CPF"
    )

    fun interpretar(textoBruto: String): DadosExtraidos {
        val linhas = textoBruto.lines().map { it.trim() }.filter { it.isNotBlank() }
        val linhasNormalizadas = linhas.map { normalizar(it) }

        val cpf = extrairCpf(textoBruto)
        val nome = extrairAposRotulo(linhas, linhasNormalizadas, listOf("NOME"))
        val filiacao = ordenarMaePaiPorSobrenome(nome, extrairFiliacao(linhas, linhasNormalizadas))
        val endereco = extrairAposRotulo(
            linhas, linhasNormalizadas,
            listOf("ENDERECO", "RESIDENCIA", "LOGRADOURO")
        )
        val dataNascimento = extrairData(linhas, linhasNormalizadas, listOf("NASCIMENTO", "NASC"))
        // Alguns documentos chamam de "expedição", outros de "emissão" — aceita os dois.
        val dataExpedicao = extrairData(
            linhas, linhasNormalizadas,
            listOf("EXPEDICAO", "EXPEDIÇÃO", "EMISSAO", "EMISSÃO", "DATA EXP")
        )
        val rg = extrairNumeroRg(linhas, linhasNormalizadas, cpf)

        return DadosExtraidos(
            nomeCompleto = tituloCase(nome),
            cpf = if (cpf.isNotBlank()) CpfValidator.formatar(cpf) else "",
            dataNascimento = dataNascimento,
            nomeDaMae = tituloCase(filiacao.first),
            nomeDoPai = tituloCase(filiacao.second),
            rg = rg,
            dataExpedicaoRg = dataExpedicao,
            endereco = tituloCase(endereco),
            textoBrutoReconhecido = textoBruto
        )
    }

    /**
     * Procura uma data (dd/mm/aaaa) perto do rótulo — na própria linha, nas
     * duas seguintes, ou (comum em CNH, onde o layout do cartão faz o texto
     * ser lido fora de ordem) nas três linhas anteriores ao rótulo.
     */
    private fun extrairData(
        linhasOriginais: List<String>,
        linhasNormalizadas: List<String>,
        rotulos: List<String>
    ): String {
        for (i in linhasNormalizadas.indices) {
            val linha = linhasNormalizadas[i]
            if (rotulos.none { linha.contains(it) }) continue

            for (j in i until minOf(i + 3, linhasOriginais.size)) {
                val match = REGEX_DATA.find(linhasOriginais[j])
                if (match != null) return match.value
            }
            for (j in (i - 1) downTo maxOf(i - 3, 0)) {
                val match = REGEX_DATA.find(linhasOriginais[j])
                if (match != null) return match.value
            }
        }
        return ""
    }

    /**
     * Procura o número do RG perto de rótulos como "REGISTRO GERAL" ou "RG".
     * Descarta qualquer número igual ao CPF (11 dígitos) para não confundir os dois.
     */
    private fun extrairNumeroRg(
        linhasOriginais: List<String>,
        linhasNormalizadas: List<String>,
        cpfDigits: String
    ): String {
        val rotulos = listOf("REGISTRO GERAL", "IDENTIDADE N", "DOC IDENTIDADE", "RG")
        for (i in linhasNormalizadas.indices) {
            val linha = linhasNormalizadas[i]
            if (rotulos.none { linha.contains(it) }) continue
            for (j in i until minOf(i + 3, linhasOriginais.size)) {
                val match = REGEX_NUMERO_DOCUMENTO.find(linhasOriginais[j]) ?: continue
                val digitos = match.value.filter { it.isDigit() }
                if (digitos.length in 6..10 && digitos != cpfDigits) return match.value.trim()
            }
        }
        return ""
    }

    private fun extrairCpf(texto: String): String {
        val match = REGEX_CPF.find(texto) ?: return ""
        val digitos = match.value.filter { it.isDigit() }
        return if (digitos.length == 11) digitos else ""
    }

    /**
     * No Brasil, o nome completo costuma seguir o padrão
     * [Nome] [Sobrenome da mãe] [Sobrenome do pai]. Se um dos dois nomes
     * encontrados na filiação compartilha o último sobrenome do cliente
     * (o sobrenome paterno) e o outro não, isso confirma quem é quem —
     * mesmo que a ordem em que apareceram no documento seja diferente.
     * Quando não dá pra confirmar, mantém a ordem original (primeiro = mãe).
     */
    private fun ordenarMaePaiPorSobrenome(nomeCompleto: String, candidatos: Pair<String, String>): Pair<String, String> {
        val sobrenomePaterno = normalizar(nomeCompleto).trim().split(" ").lastOrNull()
        if (sobrenomePaterno.isNullOrBlank() || candidatos.first.isBlank() || candidatos.second.isBlank()) {
            return candidatos
        }
        val aTemSobrenomePaterno = normalizar(candidatos.first).split(" ").contains(sobrenomePaterno)
        val bTemSobrenomePaterno = normalizar(candidatos.second).split(" ").contains(sobrenomePaterno)
        return when {
            aTemSobrenomePaterno && !bTemSobrenomePaterno -> candidatos.second to candidatos.first // 'a' é o pai
            bTemSobrenomePaterno && !aTemSobrenomePaterno -> candidatos // já está certo ('b' é o pai)
            else -> candidatos // ambíguo — mantém como veio
        }
    }

    /**
     * Procura uma linha que contenha um dos rótulos e o valor correspondente.
     * O valor pode estar em 3 lugares, verificados nesta ordem:
     *  1. Na mesma linha, depois do rótulo (ex.: "NOME: JOAO DA SILVA")
     *  2. Na(s) linha(s) seguinte(s) — comum no RG
     *  3. Na(s) linha(s) anterior(es) — comum na CNH, onde o rótulo bilíngue
     *     ("Nome / Name") aparece DEPOIS do valor por causa da forma como o
     *     ML Kit lê os blocos de texto rotacionados do cartão
     */
    private fun extrairAposRotulo(
        linhasOriginais: List<String>,
        linhasNormalizadas: List<String>,
        rotulos: List<String>
    ): String {
        for (i in linhasNormalizadas.indices) {
            val linha = linhasNormalizadas[i]
            val rotuloEncontrado = rotulos.firstOrNull { linha.startsWith(it) || linha == it }
                ?: continue

            // Caso 1: valor na mesma linha, depois do rótulo (com ':' ou espaço)
            val restante = limparValor(
                linhasOriginais[i].substring(minOf(rotuloEncontrado.length, linhasOriginais[i].length))
            )
            if (restante.length >= 3 && !ehRuido(restante)) return restante

            // Caso 2: valor na(s) próxima(s) linha(s)
            for (j in (i + 1) until minOf(i + 3, linhasOriginais.size)) {
                val candidato = limparValor(linhasOriginais[j])
                if (candidato.length >= 3 && !ehRuido(candidato) && !ehOutroRotulo(linhasNormalizadas[j])) {
                    return candidato
                }
            }

            // Caso 3: valor na(s) linha(s) anterior(es) ao rótulo
            for (j in (i - 1) downTo maxOf(i - 2, 0)) {
                val candidato = limparValor(linhasOriginais[j])
                if (candidato.length >= 3 && !ehRuido(candidato) && !ehOutroRotulo(linhasNormalizadas[j])) {
                    return candidato
                }
            }
        }
        return ""
    }

    /**
     * Remove caracteres que o OCR às vezes gruda no início/fim de uma linha
     * (barras, pipes, dois-pontos, traços soltos, aspas) e que não fazem
     * parte do dado em si — ex.: "/ MARCOS SILVA" deve virar "MARCOS SILVA".
     */
    private fun limparValor(texto: String): String {
        return texto.trim()
            .trim('/', '\\', '|', ':', '-', '_', '*', '.', ',', '"', '\'', '´', '`')
            .trim()
    }

    /**
     * FILIAÇÃO pode aparecer de duas formas:
     *  - RG: rótulo primeiro, depois mãe e pai nas linhas seguintes
     *  - CNH: mãe e pai primeiro, rótulo bilíngue ("Filiação/ Filiation") depois
     * Tenta as duas ordens. Também remove "palavras soltas" duplicadas que
     * o OCR às vezes captura de marca d'água/ruído do cartão (ex.: um
     * sobrenome sozinho repetido logo abaixo do nome completo).
     */
    private fun extrairFiliacao(
        linhasOriginais: List<String>,
        linhasNormalizadas: List<String>
    ): Pair<String, String> {
        val idxFiliacao = linhasNormalizadas.indexOfFirst {
            it.contains("FILIACAO") || it.contains("FILIATION")
        }
        if (idxFiliacao == -1) {
            val mae = extrairAposRotulo(linhasOriginais, linhasNormalizadas, listOf("MAE", "NOME DA MAE"))
            val pai = extrairAposRotulo(linhasOriginais, linhasNormalizadas, listOf("PAI", "NOME DO PAI"))
            return mae to pai
        }

        // Tenta primeiro as linhas ANTES do rótulo (formato CNH)
        val antes = coletarCandidatos(linhasOriginais, linhasNormalizadas, idxFiliacao - 1, -1)
        val limposAntes = removerPalavrasSoltasDuplicadas(antes)
        if (limposAntes.size >= 2) return limposAntes[0] to limposAntes[1]
        if (limposAntes.size == 1) return limposAntes[0] to ""

        // Senão, tenta as linhas DEPOIS do rótulo (formato RG)
        val depois = coletarCandidatos(linhasOriginais, linhasNormalizadas, idxFiliacao + 1, +1)
        val limposDepois = removerPalavrasSoltasDuplicadas(depois)
        val mae = limposDepois.getOrNull(0) ?: ""
        val pai = limposDepois.getOrNull(1) ?: ""
        return mae to pai
    }

    /**
     * Coleta até 4 linhas candidatas a partir de `inicio`, andando na direção
     * `passo` (+1 = para frente, -1 = para trás). Pula ruído isolado sem
     * interromper a coleta, mas para ao encontrar outro rótulo conhecido.
     * O resultado sempre volta na ordem original do documento (topo → baixo).
     */
    private fun coletarCandidatos(
        linhasOriginais: List<String>,
        linhasNormalizadas: List<String>,
        inicio: Int,
        passo: Int
    ): List<String> {
        val brutos = mutableListOf<String>()
        var j = inicio
        while (j in linhasOriginais.indices && brutos.size < 4) {
            if (ehOutroRotulo(linhasNormalizadas[j])) break
            val candidato = limparValor(linhasOriginais[j])
            if (candidato.length >= 2 && !ehRuido(candidato)) {
                if (passo > 0) brutos.add(candidato) else brutos.add(0, candidato)
            }
            j += passo
        }
        return brutos
    }

    /**
     * Remove itens de uma única palavra quando essa palavra já aparece em
     * outro candidato da lista — geralmente é ruído do cartão (ex.: um
     * sobrenome solto repetido) em vez de um nome de verdade.
     */
    private fun removerPalavrasSoltasDuplicadas(candidatos: List<String>): List<String> {
        return candidatos.filterIndexed { index, candidato ->
            val palavras = candidato.trim().split(" ")
            if (palavras.size > 1) return@filterIndexed true
            val apareceEmOutro = candidatos.filterIndexed { i, _ -> i != index }
                .any { it.split(" ").contains(palavras[0]) }
            !apareceEmOutro
        }
    }

    private fun ehRuido(texto: String): Boolean {
        val normalizado = normalizar(texto)
        if (normalizado.any { it.isDigit() }) return true // datas/números não são nomes
        if (PALAVRAS_RUIDO.any { normalizado.contains(it) }) return true

        // Um nome de verdade é majoritariamente letras e espaços. Se sobrar
        // muito símbolo (ex.: OCR grudou "/", "|", "()" etc.), é ruído.
        val letras = texto.count { it.isLetter() }
        val relevante = texto.count { !it.isWhitespace() }
        if (relevante == 0 || letras.toFloat() / relevante < 0.7f) return true

        // Uma "palavra" isolada de 1 caractere no início costuma ser resíduo do OCR.
        val primeiraPalavra = texto.trim().split(" ").firstOrNull() ?: ""
        if (primeiraPalavra.length == 1) return true

        return false
    }

    private fun ehOutroRotulo(linhaNormalizada: String): Boolean {
        val rotulosConhecidos = listOf(
            "NOME", "CPF", "FILIACAO", "NASCIMENTO", "NATURALIDADE", "DOC", "REGISTRO",
            "VALIDADE", "CATEGORIA", "ENDERECO", "RESIDENCIA", "RG", "ORGAO", "EXPEDIDOR"
        )
        return rotulosConhecidos.any { linhaNormalizada.startsWith(it) }
    }

    private fun normalizar(texto: String): String {
        val semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
        return semAcento.uppercase().trim()
    }

    private fun tituloCase(texto: String): String {
        if (texto.isBlank()) return ""
        val conectivos = setOf("de", "da", "do", "das", "dos")
        return texto.lowercase().split(" ").joinToString(" ") { palavra ->
            if (palavra in conectivos) {
                palavra
            } else {
                palavra.replaceFirstChar { it.uppercase() }
            }
        }
    }
}
