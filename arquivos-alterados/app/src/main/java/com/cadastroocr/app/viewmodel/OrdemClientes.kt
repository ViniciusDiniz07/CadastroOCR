package com.cadastroocr.app.viewmodel

import com.cadastroocr.app.data.Cliente
import java.text.Collator
import java.util.Locale

/** Formas de ordenar a listagem. Só mudam a apresentação na tela; nada é gravado no banco. */
enum class OrdemClientes(val rotulo: String) {
    NOME_AZ("Nome (A–Z)"),
    NOME_ZA("Nome (Z–A)"),
    MAIS_RECENTES("Mais recentes adicionados"),
    MAIS_ANTIGOS("Mais antigos adicionados"),
    ULTIMOS_ATUALIZADOS("Últimos atualizados")
}

// PRIMARY: ignora maiúsculas/minúsculas e acentos ("Álvaro" fica junto de "Alvaro").
private val collatorPtBr: Collator = Collator.getInstance(Locale("pt", "BR")).apply {
    strength = Collator.PRIMARY
}

fun ordenarClientes(lista: List<Cliente>, ordem: OrdemClientes): List<Cliente> {
    val porNome = Comparator<Cliente> { a, b ->
        val c = collatorPtBr.compare(a.nomeCompleto.trim(), b.nomeCompleto.trim())
        if (c != 0) c else a.criadoEm.compareTo(b.criadoEm)
    }
    return when (ordem) {
        OrdemClientes.NOME_AZ -> lista.sortedWith(porNome)
        OrdemClientes.NOME_ZA -> lista.sortedWith(porNome.reversed())
        OrdemClientes.MAIS_RECENTES -> lista.sortedByDescending { it.criadoEm }
        OrdemClientes.MAIS_ANTIGOS -> lista.sortedBy { it.criadoEm }
        OrdemClientes.ULTIMOS_ATUALIZADOS -> lista.sortedByDescending { it.atualizadoEm }
    }
}
