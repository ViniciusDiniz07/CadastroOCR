package com.cadastroocr.app.ocr

/** Nível de sucesso do OCR, usado para decidir qual mensagem mostrar (Seção 21). */
enum class NivelReconhecimento { SUCESSO, PARCIAL, INSUFICIENTE }

data class DadosExtraidos(
    val nomeCompleto: String = "",
    val cpf: String = "",
    val dataNascimento: String = "",
    val nomeDaMae: String = "",
    val nomeDoPai: String = "",
    val rg: String = "",
    val dataExpedicaoRg: String = "",
    val endereco: String = "",
    // Campos que também podem vir de um print de conversa (ConversationDataParser),
    // além do documento — por isso ficam separados do "endereco" genérico acima.
    val email: String = "",
    val telefone: String = "",
    val cep: String = "",
    val rua: String = "",
    val numero: String = "",
    val bairro: String = "",
    val cidade: String = "",
    val textoBrutoReconhecido: String = ""
) {
    /** Quantos dos campos "principais" foram encontrados (usado para o nível de confiança). */
    private fun camposPreenchidos(): Int =
        listOf(nomeCompleto, cpf, nomeDaMae, nomeDoPai).count { it.isNotBlank() }

    fun nivel(): NivelReconhecimento = when {
        textoBrutoReconhecido.isBlank() -> NivelReconhecimento.INSUFICIENTE
        camposPreenchidos() >= 3 -> NivelReconhecimento.SUCESSO
        camposPreenchidos() in 1..2 -> NivelReconhecimento.PARCIAL
        else -> NivelReconhecimento.INSUFICIENTE
    }
}
