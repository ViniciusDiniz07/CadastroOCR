package com.cadastroocr.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cadastroocr.app.data.Cliente
import com.cadastroocr.app.ai.TipoOrigem
import com.cadastroocr.app.ocr.DadosExtraidos
import com.cadastroocr.app.ui.theme.AccentGreen
import com.cadastroocr.app.ui.theme.TextSecondary
import com.cadastroocr.app.util.CpfValidator
import com.cadastroocr.app.util.EmailValidator
import com.cadastroocr.app.viewmodel.ClienteViewModel
import kotlinx.coroutines.launch

/**
 * Tela única de formulário, usada em 3 contextos:
 *  - Revisão pós-OCR (dadosOcr preenchidos pelo parser, tudo editável)
 *  - Cadastro manual (tudo vazio)
 *  - Edição de cliente existente (clienteExistente preenchido)
 *
 * Regra da Seção 13: nenhum campo opcional bloqueia o salvamento — apenas
 * o nome completo é exigido, para evitar clientes "fantasmas" na lista.
 *
 * Os campos estão organizados em seções (Dados Pessoais, Documento, Contato,
 * Endereço, Plano e Pagamento) para não virar uma lista longa e confusa.
 */
@Composable
fun ReviewScreen(
    viewModel: ClienteViewModel,
    clienteExistente: Cliente?,
    dadosOcr: DadosExtraidos?,
    avisoOcrParcial: Boolean,
    onVoltar: () -> Unit,
    onSalvo: () -> Unit
) {
    // Dados pessoais
    var nome by remember { mutableStateOf(clienteExistente?.nomeCompleto ?: dadosOcr?.nomeCompleto ?: "") }
    var cpf by remember { mutableStateOf(clienteExistente?.cpf ?: dadosOcr?.cpf ?: "") }
    var dataNascimento by remember { mutableStateOf(clienteExistente?.dataNascimento ?: dadosOcr?.dataNascimento ?: "") }
    var mae by remember { mutableStateOf(clienteExistente?.nomeDaMae ?: dadosOcr?.nomeDaMae ?: "") }
    var pai by remember { mutableStateOf(clienteExistente?.nomeDoPai ?: dadosOcr?.nomeDoPai ?: "") }

    // Documento
    var rg by remember { mutableStateOf(clienteExistente?.rg ?: dadosOcr?.rg ?: "") }
    var dataExpedicaoRg by remember { mutableStateOf(clienteExistente?.dataExpedicaoRg ?: dadosOcr?.dataExpedicaoRg ?: "") }

    // Contato
    var email by remember { mutableStateOf(clienteExistente?.email ?: "") }
    var telefone by remember { mutableStateOf(clienteExistente?.telefone ?: "") }

    // Endereço
    var rua by remember {
        mutableStateOf(
            clienteExistente?.rua
                ?: dadosOcr?.rua?.takeIf { it.isNotBlank() }
                ?: dadosOcr?.endereco
                ?: ""
        )
    }
    var numero by remember { mutableStateOf(clienteExistente?.numero ?: "") }
    var complemento by remember { mutableStateOf(clienteExistente?.complemento ?: "") }
    var cep by remember { mutableStateOf(clienteExistente?.cep ?: "") }
    var bairro by remember { mutableStateOf(clienteExistente?.bairro ?: "") }
    var cidade by remember { mutableStateOf(clienteExistente?.cidade ?: "") }

    // Plano e pagamento
    var plano by remember { mutableStateOf(clienteExistente?.plano ?: "") }
    var valorPlano by remember { mutableStateOf(clienteExistente?.valorPlano ?: "") }
    var formaPagamento by remember { mutableStateOf(clienteExistente?.formaPagamento ?: "") }
    var diaVencimento by remember { mutableStateOf(clienteExistente?.diaVencimento ?: "") }

    var tentouSalvar by remember { mutableStateOf(false) }

    val cpfInvalido = cpf.isNotBlank() && !CpfValidator.isValido(cpf)
    val emailInvalido = !EmailValidator.isValido(email)
    val nomeInvalido = nome.isBlank()

    // --- Importação de conversa (WhatsApp) ---
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    var processandoConversa by remember { mutableStateOf(false) }
    var textoBrutoConversa by remember { mutableStateOf("") }
    var conflitosPendentes by remember { mutableStateOf(listOf<ConflitoCampo>()) }

    /**
     * Para cada campo que a conversa encontrou: se o campo já estava vazio,
     * preenche direto. Se já tinha um valor DIFERENTE, gera um conflito para
     * o usuário decidir (nunca substitui um dado existente sem perguntar —
     * regra explícita do briefing da importação de conversa).
     */
    fun aplicarDadosDaConversa(dados: DadosExtraidos) {
        val novosConflitos = mutableListOf<ConflitoCampo>()
        fun processarCampo(rotulo: String, atual: String, novo: String, setar: (String) -> Unit) {
            if (novo.isBlank()) return
            if (atual.isBlank()) { setar(novo); return }
            if (atual.trim().equals(novo.trim(), ignoreCase = true)) return
            novosConflitos.add(ConflitoCampo(rotulo, atual, novo) { setar(novo) })
        }
        processarCampo("Nome completo", nome, dados.nomeCompleto) { nome = it }
        processarCampo("CPF", cpf, dados.cpf) { cpf = CpfValidator.formatar(it) }
        processarCampo("Data de nascimento", dataNascimento, dados.dataNascimento) { dataNascimento = it }
        processarCampo("E-mail", email, dados.email) { email = it }
        processarCampo("N° para contato", telefone, dados.telefone) { telefone = it }
        processarCampo("CEP", cep, dados.cep) { cep = it }
        processarCampo("Rua", rua, dados.rua) { rua = it }
        processarCampo("N°", numero, dados.numero) { numero = it }
        processarCampo("Bairro", bairro, dados.bairro) { bairro = it }
        processarCampo("Cidade", cidade, dados.cidade) { cidade = it }
        conflitosPendentes = conflitosPendentes + novosConflitos
        textoBrutoConversa = dados.textoBrutoReconhecido
    }

    val seletorGaleriaConversa = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            processandoConversa = true
            escopo.launch {
                try {
                    val texto = viewModel.ocrTextoDeImagem(contexto, uri)
                    aplicarDadosDaConversa(viewModel.interpretarTexto(texto, TipoOrigem.CONVERSA, contexto))
                } finally {
                    processandoConversa = false
                }
            }
        }
    }

    val cameraConversaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            processandoConversa = true
            escopo.launch {
                try {
                    val texto = viewModel.ocrTextoDeImagem(bitmap)
                    aplicarDadosDaConversa(viewModel.interpretarTexto(texto, TipoOrigem.CONVERSA, contexto))
                } finally {
                    processandoConversa = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (clienteExistente != null) "Editar Cliente" else "Revisão dos Dados") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (dadosOcr != null) {
                Text(
                    "Verifique os dados extraídos antes de salvar o cliente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (avisoOcrParcial) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Não conseguimos identificar todos os dados. Confira e preencha os campos manualmente.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (dadosOcr.textoBrutoReconhecido.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    TextoOcrDebug("Ver texto bruto do OCR", dadosOcr.textoBrutoReconhecido)
                }
                Spacer(Modifier.height(16.dp))
            }

            SecaoFormulario("Importar dados extras")
            Text(
                "Tire um print de uma conversa (ex.: WhatsApp) para preencher automaticamente e-mail, telefone e endereço.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { cameraConversaLauncher.launch(null) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tirar foto")
                }
                OutlinedButton(
                    onClick = { seletorGaleriaConversa.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Da galeria")
                }
            }
            if (processandoConversa) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Processando print...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            if (textoBrutoConversa.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                TextoOcrDebug("Ver texto bruto da conversa", textoBrutoConversa)
            }

            SecaoFormulario("Dados pessoais")
            CampoFormulario(
                rotulo = "Nome completo",
                valor = nome,
                onValorAlterado = { nome = it },
                erro = if (tentouSalvar && nomeInvalido) "Informe o nome completo" else null
            )
            Spacer(Modifier.height(12.dp))
            CampoFormulario(
                rotulo = "CPF",
                valor = cpf,
                onValorAlterado = { cpf = CpfValidator.formatar(it) },
                teclado = KeyboardType.Number,
                erro = if (cpfInvalido) "CPF inválido — confira os números" else null
            )
            Spacer(Modifier.height(12.dp))
            CampoData(
                rotulo = "Data de nascimento",
                valor = dataNascimento,
                onValorAlterado = { dataNascimento = it }
            )
            Spacer(Modifier.height(12.dp))
            CampoFormulario(rotulo = "Nome da mãe", valor = mae, onValorAlterado = { mae = it })
            Spacer(Modifier.height(12.dp))
            CampoFormulario(rotulo = "Nome do pai", valor = pai, onValorAlterado = { pai = it })

            SecaoFormulario("Documento")
            CampoFormulario(rotulo = "RG", valor = rg, onValorAlterado = { rg = it }, teclado = KeyboardType.Number)
            Spacer(Modifier.height(12.dp))
            CampoData(
                rotulo = "Data de expedição",
                valor = dataExpedicaoRg,
                onValorAlterado = { dataExpedicaoRg = it }
            )

            SecaoFormulario("Contato")
            CampoFormulario(
                rotulo = "E-mail",
                valor = email,
                onValorAlterado = { email = it },
                teclado = KeyboardType.Email,
                erro = if (emailInvalido) "E-mail em formato inválido" else null
            )
            Spacer(Modifier.height(12.dp))
            CampoFormulario(
                rotulo = "N° para contato",
                valor = telefone,
                onValorAlterado = { telefone = it },
                teclado = KeyboardType.Phone
            )

            SecaoFormulario("Endereço")
            CampoFormulario(rotulo = "Rua", valor = rua, onValorAlterado = { rua = it })
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CampoFormulario(rotulo = "N°", valor = numero, onValorAlterado = { numero = it }, teclado = KeyboardType.Number)
                }
                Box(modifier = Modifier.weight(2f)) {
                    CampoFormulario(rotulo = "Complemento", valor = complemento, onValorAlterado = { complemento = it })
                }
            }
            Spacer(Modifier.height(12.dp))
            CampoFormulario(rotulo = "CEP", valor = cep, onValorAlterado = { cep = it }, teclado = KeyboardType.Number)
            Spacer(Modifier.height(12.dp))
            CampoFormulario(rotulo = "Bairro", valor = bairro, onValorAlterado = { bairro = it })
            Spacer(Modifier.height(12.dp))
            CampoFormulario(rotulo = "Cidade", valor = cidade, onValorAlterado = { cidade = it })

            SecaoFormulario("Plano e pagamento")
            CampoFormulario(rotulo = "Plano", valor = plano, onValorAlterado = { plano = it })
            Spacer(Modifier.height(12.dp))
            CampoFormulario(rotulo = "Valor", valor = valorPlano, onValorAlterado = { valorPlano = it }, teclado = KeyboardType.Number)
            Spacer(Modifier.height(12.dp))
            CampoFormulario(rotulo = "Forma de pagamento", valor = formaPagamento, onValorAlterado = { formaPagamento = it })
            Spacer(Modifier.height(12.dp))
            CampoFormulario(
                rotulo = "Vencimento (dia)",
                valor = diaVencimento,
                onValorAlterado = { diaVencimento = it },
                teclado = KeyboardType.Number
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    tentouSalvar = true
                    if (!nomeInvalido) {
                        val cliente = (clienteExistente ?: Cliente()).copy(
                            nomeCompleto = nome.trim(),
                            cpf = cpf.trim(),
                            dataNascimento = dataNascimento.trim(),
                            nomeDaMae = mae.trim(),
                            nomeDoPai = pai.trim(),
                            rg = rg.trim(),
                            dataExpedicaoRg = dataExpedicaoRg.trim(),
                            email = email.trim(),
                            telefone = telefone.trim(),
                            rua = rua.trim(),
                            numero = numero.trim(),
                            complemento = complemento.trim(),
                            cep = cep.trim(),
                            bairro = bairro.trim(),
                            cidade = cidade.trim(),
                            plano = plano.trim(),
                            valorPlano = valorPlano.trim(),
                            formaPagamento = formaPagamento.trim(),
                            diaVencimento = diaVencimento.trim()
                        )
                        viewModel.salvar(cliente, onSalvo)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = androidx.compose.ui.graphics.Color.Black),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (clienteExistente != null) "Salvar alterações" else "Salvar cliente")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    DialogoConflito(
        conflitos = conflitosPendentes,
        onConflitosAlterados = { conflitosPendentes = it }
    )
}

@Composable
private fun SecaoFormulario(titulo: String) {
    Spacer(Modifier.height(20.dp))
    Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun CampoFormulario(
    rotulo: String,
    valor: String,
    onValorAlterado: (String) -> Unit,
    teclado: KeyboardType = KeyboardType.Text,
    erro: String? = null
) {
    Column {
        OutlinedTextField(
            value = valor,
            onValueChange = onValorAlterado,
            label = { Text(rotulo) },
            isError = erro != null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = teclado),
            modifier = Modifier.fillMaxWidth()
        )
        if (erro != null) {
            Text(
                erro,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

/**
 * Campo de data dividido em 3 caixinhas (Dia / Mês / Ano) em vez de um texto
 * corrido "dd/mm/aaaa" — mais fácil de ler e de corrigir rapidamente quando o
 * OCR confunde qual data vai em qual campo (algo comum em CNH, cujo layout de
 * cartão faz o texto ser lido fora de ordem). O valor combinado continua
 * sendo salvo como "dd/mm/aaaa", igual antes.
 */
@Composable
private fun CampoData(
    rotulo: String,
    valor: String,
    onValorAlterado: (String) -> Unit
) {
    val partesIniciais = remember { valor.split("/") }
    var dia by remember { mutableStateOf(partesIniciais.getOrNull(0)?.trim() ?: "") }
    var mes by remember { mutableStateOf(partesIniciais.getOrNull(1)?.trim() ?: "") }
    var ano by remember { mutableStateOf(partesIniciais.getOrNull(2)?.trim() ?: "") }

    fun notificarAlteracao() {
        val combinado = if (dia.isBlank() && mes.isBlank() && ano.isBlank()) "" else "$dia/$mes/$ano"
        onValorAlterado(combinado)
    }

    Column {
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = dia,
                onValueChange = { novo -> if (novo.length <= 2 && novo.all { it.isDigit() }) { dia = novo; notificarAlteracao() } },
                placeholder = { Text("DD") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = mes,
                onValueChange = { novo -> if (novo.length <= 2 && novo.all { it.isDigit() }) { mes = novo; notificarAlteracao() } },
                placeholder = { Text("MM") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = ano,
                onValueChange = { novo -> if (novo.length <= 4 && novo.all { it.isDigit() }) { ano = novo; notificarAlteracao() } },
                placeholder = { Text("AAAA") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1.4f)
            )
        }
    }
}

/**
 * Mostra um texto bruto de OCR (documento ou conversa), escondido por padrão.
 * Não é uma funcionalidade do briefing original — serve para conferir
 * exatamente o que o OCR está lendo e mandar print quando algum campo sair
 * errado, o que torna os ajustes nos parsers muito mais precisos.
 */
@Composable
private fun TextoOcrDebug(titulo: String, texto: String) {
    var expandido by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            TextButton(onClick = { expandido = !expandido }, contentPadding = PaddingValues(0.dp)) {
                Text(if (expandido) "$titulo ▲" else "$titulo ▼")
            }
            if (expandido) {
                Spacer(Modifier.height(6.dp))
                Text(
                    texto,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = TextSecondary
                )
            }
        }
    }
}

/** Um campo que já tinha valor e a conversa encontrou um valor diferente — precisa da decisão do usuário. */
private data class ConflitoCampo(
    val rotulo: String,
    val valorAtual: String,
    val valorNovo: String,
    val aplicar: () -> Unit
)

/** Mostra um conflito de cada vez (o primeiro da fila) até a lista esvaziar. */
@Composable
private fun DialogoConflito(
    conflitos: List<ConflitoCampo>,
    onConflitosAlterados: (List<ConflitoCampo>) -> Unit
) {
    val conflito = conflitos.firstOrNull() ?: return
    AlertDialog(
        onDismissRequest = { onConflitosAlterados(conflitos.drop(1)) },
        title = { Text("${conflito.rotulo} já preenchido") },
        text = {
            Column {
                Text("Atual: ${conflito.valorAtual}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text("Encontrado na conversa: ${conflito.valorNovo}", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                conflito.aplicar()
                onConflitosAlterados(conflitos.drop(1))
            }) { Text("Substituir") }
        },
        dismissButton = {
            TextButton(onClick = { onConflitosAlterados(conflitos.drop(1)) }) { Text("Manter atual") }
        }
    )
}
