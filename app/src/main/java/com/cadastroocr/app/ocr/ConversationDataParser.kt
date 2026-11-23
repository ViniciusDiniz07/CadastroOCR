package com.cadastroocr.app.ocr

/**
 * Extrai dados úteis para o cadastro a partir de texto de conversas (ex.:
 * print do WhatsApp). Diferente do DocumentDataParser — que se baseia na
 * posição de rótulos fixos de um documento — aqui o texto é livre e
 * conversacional, então a extração é por PADRÕES (regex) e algumas poucas
 * palavras de contexto, nunca por posição de linha.
 *
 * Segue a mesma regra do OCR de documento: nunca inventa dado. Se não achar
 * um padrão com segurança, o campo correspondente fica vazio.
 */
object ConversationDataParser {

    private val REGEX_EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
    private val REGEX_CEP = Regex("""\b\d{5}-?\d{3}\b""")
    private val REGEX_CPF_FORMATADO = Regex("""\b\d{3}\.\d{3}\.\d{3}-\d{2}\b""")
    private val REGEX_TELEFONE = Regex("""\(?\d{2}\)?[\s.-]?9?\d{4}[\s.-]?\d{4}\b""")
    private val REGEX_DATA = Regex("""\b\d{2}/\d{2}/\d{4}\b""")

    private val PREFIXOS_LOGRADOURO = listOf(
        "RUA", "AVENIDA", "AV\\.", "TRAVESSA", "ALAMEDA", "ESTRADA", "RODOVIA", "PRAÇA", "PRACA"
    )

    fun interpretar(textoBruto: String): DadosExtraidos {
        val email = REGEX_EMAIL.find(textoBruto)?.value ?: ""
        val cep = extrairCep(textoBruto)
        val telefone = extrairTelefone(textoBruto, cep)
        val cpf = extrairCpf(textoBruto)
        val endereco = extrairEndereco(textoBruto)
        val nome = extrairNome(textoBruto)
        val dataNascimento = extrairDataNascimento(textoBruto)

        return DadosExtraidos(
            nomeCompleto = nome,
            cpf = cpf,
            dataNascimento = dataNascimento,
            email = email,
            telefone = telefone,
            cep = cep,
            rua = endereco.rua,
            numero = endereco.numero,
            bairro = endereco.bairro,
            cidade = endereco.cidade,
            textoBrutoReconhecido = textoBruto
        )
    }

    private fun extrairCep(texto: String): String = REGEX_CEP.find(texto)?.value ?: ""

    /** Evita confundir telefone com CEP: descarta um match que seja exatamente o CEP já achado. */
    private fun extrairTelefone(texto: String, cepJaEncontrado: String): String {
        val match = REGEX_TELEFONE.find(texto) ?: return ""
        val digitos = match.value.filter { it.isDigit() }
        val digitosCep = cepJaEncontrado.filter { it.isDigit() }
        if (digitos.length < 10 || digitos.length > 11) return ""
        if (digitos == digitosCep) return ""
        return match.value.trim()
    }

    /** Só aceita CPF formatado com pontos/traço — um número solto de 11 dígitos é ambíguo demais com telefone. */
    private fun extrairCpf(texto: String): String = REGEX_CPF_FORMATADO.find(texto)?.value ?: ""

    /**
     * Reconhece frases como "meu nome é João da Silva" ou "me chamo João".
     * Só aceita se vier de uma frase explícita — nunca assume que a primeira
     * palavra maiúscula do texto é um nome (geraria falsos positivos demais
     * numa conversa comum).
     */
    private fun extrairNome(texto: String): String {
        val padroes = listOf(
            Regex("""(?i)meu nome (?:completo )?é\s+([A-Za-zÀ-ÿ\s]{3,60})"""),
            Regex("""(?i)me chamo\s+([A-Za-zÀ-ÿ\s]{3,60})"""),
            Regex("""(?i)nome\s*:\s*([A-Za-zÀ-ÿ\s]{3,60})""")
        )
        for (padrao in padroes) {
            val m = padrao.find(texto) ?: continue
            val bruto = m.groupValues[1].trim().split(Regex("""[,.\n]"""))[0].trim()
            if (bruto.split(" ").size in 1..6) return tituloCase(bruto)
        }
        return ""
    }

    /**
     * Só aceita uma data como "data de nascimento" se ela aparecer perto de
     * uma palavra de contexto como "nasci"/"nascimento"/"aniversário" — uma
     * data solta numa conversa pode ser qualquer outra coisa (um compromisso,
     * uma cobrança, etc.), então não dá pra assumir.
     */
    private fun extrairDataNascimento(texto: String): String {
        val palavrasContexto = listOf("nasci", "nascimento", "aniversário", "aniversario", "nasc")
        val linhas = texto.lines()
        for (linha in linhas) {
            val normalizada = linha.lowercase()
            if (palavrasContexto.none { normalizada.contains(it) }) continue
            val match = REGEX_DATA.find(linha)
            if (match != null) return match.value
        }
        return ""
    }

    private data class EnderecoEncontrado(
        val rua: String = "",
        val numero: String = "",
        val bairro: String = "",
        val cidade: String = ""
    )

    /**
     * Tenta separar rua/número/bairro/cidade de uma frase de endereço em
     * texto livre. Ex.: "Moro na Rua das Flores, 123, bairro Aterrado,
     * Volta Redonda" ou "Rua 8 número 212 vila rica volta redonda".
     *
     * Sempre trabalha em cima da LINHA que contém a rua (não o texto inteiro),
     * para não misturar com mensagens vizinhas na conversa.
     */
    private fun extrairEndereco(texto: String): EnderecoEncontrado {
        val prefixos = PREFIXOS_LOGRADOURO.joinToString("|")
        val regexPrefixo = Regex("""(?i)($prefixos)\s""")
        val linha = texto.lines().firstOrNull { regexPrefixo.containsMatchIn(it) } ?: return EnderecoEncontrado()

        // Padrão A: número indicado por uma palavra-chave ("número", "nº", "n.").
        // Precisa vir primeiro porque é o único jeito de separar corretamente
        // quando o próprio nome da rua é um número (ex.: "Rua 8" em loteamentos).
        val padraoComPalavraNumero = Regex(
            """(?i)($prefixos)\s+(.+?)\s+(?:n[ºo°.]?|numero|número)\.?\s*(\d{1,5})\b"""
        )
        // Padrão B: número emendado direto depois do nome da rua (nome só com letras).
        val padraoComNumeroDireto = Regex(
            """(?i)($prefixos)\s+([A-Za-zÀ-ÿ\s]{2,50}?)\s*,?\s*(\d{1,5})\b"""
        )
        // Padrão C: frase sem nenhum número.
        val padraoSemNumero = Regex("""(?i)($prefixos)\s+([A-Za-zÀ-ÿ\s]{2,50}?)(?:[,.\n]|$)""")

        val match = padraoComPalavraNumero.find(linha) ?: padraoComNumeroDireto.find(linha)

        val rua: String
        val numero: String
        val fimTrecho: Int

        if (match != null) {
            val prefixo = tituloCase(match.groupValues[1])
            val nomeRua = tituloCase(match.groupValues[2].trim())
            rua = if (nomeRua.isNotBlank()) "$prefixo $nomeRua" else ""
            numero = match.groupValues[3]
            fimTrecho = match.range.last + 1
        } else {
            val m = padraoSemNumero.find(linha) ?: return EnderecoEncontrado()
            val prefixo = tituloCase(m.groupValues[1])
            val nomeRua = tituloCase(m.groupValues[2].trim())
            rua = if (nomeRua.isNotBlank()) "$prefixo $nomeRua" else ""
            numero = ""
            fimTrecho = m.range.last + 1
        }

        val bairro = Regex("""(?i)bairro\s+([A-Za-zÀ-ÿ\s]{2,40}?)(?:[,.\n]|$)""")
            .find(linha)?.groupValues?.get(1)?.trim()?.let { tituloCase(it) } ?: ""

        // Cidade: só assume via "em <Cidade>"/"na cidade de <Cidade>", ou o último
        // trecho depois de uma vírgula de verdade. Sem vírgula nem palavra-chave,
        // não dá pra saber com segurança onde o bairro termina e a cidade começa —
        // então fica em branco, em vez de arriscar juntar tudo errado.
        val cidade = Regex("""(?i)(?:em|na cidade de)\s+([A-Za-zÀ-ÿ\s]{2,40}?)(?:[,.\n]|$)""")
            .find(linha)?.groupValues?.get(1)?.trim()?.let { tituloCase(it) }
            ?: run {
                val posFim = fimTrecho.coerceAtMost(linha.length)
                val trechos = linha.substring(posFim).split(",").map { it.trim() }
                if (trechos.size > 1) {
                    trechos.lastOrNull { it.isNotBlank() && !it.equals(bairro, ignoreCase = true) && it.length in 3..40 }
                        ?.let { tituloCase(it) } ?: ""
                } else ""
            }

        return EnderecoEncontrado(rua = rua, numero = numero, bairro = bairro, cidade = cidade)
    }

    private fun tituloCase(texto: String): String {
        if (texto.isBlank()) return ""
        val conectivos = setOf("de", "da", "do", "das", "dos")
        return texto.lowercase().trim().split(" ").filter { it.isNotBlank() }.joinToString(" ") { palavra ->
            if (palavra in conectivos) palavra else palavra.replaceFirstChar { it.uppercase() }
        }
    }
}
