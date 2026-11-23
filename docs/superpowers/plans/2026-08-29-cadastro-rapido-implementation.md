# Cadastro rápido e CEP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tornar o cadastro Android mais rápido com máscaras estáveis, navegação de foco, consulta segura de CEP e preservação dos dados de OCR/IA.

**Architecture:** Utilitários Kotlin puros normalizam e validam formatos. Um serviço ViaCEP isolado devolve resultados tipados e a tela Compose coordena foco, estados de consulta e conflitos sem tornar a UI dependente da rede. UF é incluída nos modelos e migrada no Room sem apagar clientes existentes.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Coroutines, `HttpURLConnection`, Room, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-08-29-cadastro-rapido-design.md`

## Global Constraints

- Preserve OCR local, IA, importação de conversa, edição e banco local existentes.
- Consulte apenas o ViaCEP por HTTPS; falhas não bloqueiam preenchimento ou salvamento manual.
- Não sobrescreva rua, bairro, cidade ou UF divergentes sem decisão explícita.
- CPF, CEP, telefone e datas mantêm edição natural; RG não recebe máscara automática.
- A migração Room mantém todos os registros existentes.

---

### Task 1: Formatos e validações puras

**Files:**
- Create: `app/src/main/java/com/cadastroocr/app/util/CepValidator.kt`
- Create: `app/src/main/java/com/cadastroocr/app/util/TelefoneValidator.kt`
- Create: `app/src/main/java/com/cadastroocr/app/util/DataValidator.kt`
- Modify: `app/src/main/java/com/cadastroocr/app/util/CpfValidator.kt`
- Test: `app/src/test/java/com/cadastroocr/app/util/FormatadoresTest.kt`

**Interfaces:** `CepValidator.formatar/isValido`, `TelefoneValidator.formatar/isValido`, `DataValidator.formatar/isValida`, and `CpfValidator.formatarComCursor(valor: String, cursorBruto: Int): Pair<String, Int>`.

- [ ] **Step 1: Write failing tests**

```kotlin
@Test fun formatosEsperados() {
    assertEquals("123.4" to 5, CpfValidator.formatarComCursor("1234", 4))
    assertEquals("27200-000", CepValidator.formatar("27200000"))
    assertEquals("(24) 99999-9999", TelefoneValidator.formatar("24999999999"))
    assertEquals("01/01/2000", DataValidator.formatar("01012000"))
    assertFalse(DataValidator.isValida("31/02/2000"))
}
```

- [ ] **Step 2: Verify failure**

Run: `./gradlew testDebugUnitTest --tests com.cadastroocr.app.util.FormatadoresTest`

Expected: compilation failure because the types do not exist.

- [ ] **Step 3: Implement minimal helpers**

```kotlin
object CepValidator {
    fun apenasDigitos(valor: String) = valor.filter(Char::isDigit).take(8)
    fun formatar(valor: String) = apenasDigitos(valor).let { if (it.length <= 5) it else it.take(5) + "-" + it.drop(5) }
    fun isValido(valor: String) = apenasDigitos(valor).length == 8
}
```

Use `LocalDate.of(year, month, day)` for actual calendar validity. Calculate CPF selection from the count of digits before the selection, then use the length of the formatted prefix.

- [ ] **Step 4: Verify pass**

Run: `./gradlew testDebugUnitTest --tests com.cadastroocr.app.util.FormatadoresTest`

Expected: PASS.

- [ ] **Step 5: Commit**

Run: `git add app/src/main/java/com/cadastroocr/app/util app/src/test/java/com/cadastroocr/app/util/FormatadoresTest.kt`

Run: `git commit -m "feat: add registration format validators"`

### Task 2: Serviço ViaCEP isolado

**Files:**
- Create: `app/src/main/java/com/cadastroocr/app/cep/EnderecoCep.kt`
- Create: `app/src/main/java/com/cadastroocr/app/cep/ViaCepService.kt`
- Test: `app/src/test/java/com/cadastroocr/app/cep/ViaCepServiceTest.kt`

**Interfaces:** `EnderecoCep(cep, rua, bairro, cidade, uf)`, `ResultadoCep.Encontrado/NaoEncontrado/Falha`, `suspend ViaCepService.buscar(cep): ResultadoCep`, and `internal ViaCepService.parsear(json): ResultadoCep`.

- [ ] **Step 1: Write failing parser tests**

```kotlin
@Test fun converteRespostaEncontrada() {
    val json = """{"cep":"01001-000","logradouro":"Praça da Sé","bairro":"Sé","localidade":"São Paulo","uf":"SP"}"""
    assertEquals(EnderecoCep("01001-000", "Praça da Sé", "Sé", "São Paulo", "SP"), (ViaCepService.parsear(json) as ResultadoCep.Encontrado).endereco)
}
@Test fun converteCepInexistente() { assertEquals(ResultadoCep.NaoEncontrado, ViaCepService.parsear("""{"erro":true}""")) }
```

- [ ] **Step 2: Verify failure**

Run: `./gradlew testDebugUnitTest --tests com.cadastroocr.app.cep.ViaCepServiceTest`

Expected: compilation failure because the service does not exist.

- [ ] **Step 3: Implement service**

```kotlin
suspend fun buscar(cep: String): ResultadoCep = withContext(Dispatchers.IO) {
    // GET https://viacep.com.br/ws/{eightDigits}/json/; 5s connect/read timeouts.
}
```

Reject non-eight-digit input. Parse `erro: true` as `NaoEncontrado`; return `Falha(exception)` for I/O, HTTP and JSON failures. Do not log address data.

- [ ] **Step 4: Verify pass**

Run: `./gradlew testDebugUnitTest --tests com.cadastroocr.app.cep.ViaCepServiceTest`

Expected: PASS.

- [ ] **Step 5: Commit**

Run: `git add app/src/main/java/com/cadastroocr/app/cep app/src/test/java/com/cadastroocr/app/cep/ViaCepServiceTest.kt`

Run: `git commit -m "feat: add safe ViaCEP lookup service"`

### Task 3: UF e migração sem perda

**Files:**
- Modify: `app/src/main/java/com/cadastroocr/app/data/Cliente.kt`
- Modify: `app/src/main/java/com/cadastroocr/app/data/AppDatabase.kt`
- Modify: `app/src/main/java/com/cadastroocr/app/ocr/DadosExtraidos.kt`
- Modify: `app/src/main/java/com/cadastroocr/app/ai/GeminiInterpreter.kt`
- Modify: `backend/src/index.js`
- Modify: `app/src/main/java/com/cadastroocr/app/ui/screens/ClientDetailScreen.kt`
- Test: `app/src/androidTest/java/com/cadastroocr/app/data/AppDatabaseMigrationTest.kt`

**Interfaces:** add `uf: String = ""` to `Cliente` and `DadosExtraidos`; add `MIGRATION_2_3` running `ALTER TABLE clientes ADD COLUMN uf TEXT NOT NULL DEFAULT ''`; raise database version to 3 and call `.addMigrations(MIGRATION_2_3)`.

- [ ] **Step 1: Write failing migration test**

```kotlin
@Test fun migration2To3MantemClienteEAdicionaUf() {
    // Create v2 database, insert a client, migrate to v3.
    // Assert existing nomeCompleto and new empty uf.
}
```

- [ ] **Step 2: Verify failure**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.cadastroocr.app.data.AppDatabaseMigrationTest`

Expected: compilation failure until the migration exists. If no emulator is attached, retain the test and record the device limitation.

- [ ] **Step 3: Implement model propagation and migration**

```kotlin
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE clientes ADD COLUMN uf TEXT NOT NULL DEFAULT ''")
    }
}
```

Add `uf` to the Worker schema, required list, prompt and Android JSON parsing; preserve an empty value when OCR/IA did not identify it. Show UF in `ClientDetailScreen`.

- [ ] **Step 4: Verify unit suite**

Run: `./gradlew testDebugUnitTest`

Expected: PASS.

- [ ] **Step 5: Commit**

Run: `git add app/src/main/java/com/cadastroocr/app/data app/src/main/java/com/cadastroocr/app/ocr/DadosExtraidos.kt app/src/main/java/com/cadastroocr/app/ai/GeminiInterpreter.kt backend/src/index.js app/src/main/java/com/cadastroocr/app/ui/screens/ClientDetailScreen.kt app/src/androidTest/java/com/cadastroocr/app/data/AppDatabaseMigrationTest.kt`

Run: `git commit -m "feat: persist state in customer addresses"`

### Task 4: Formulário Compose, foco e conflitos de CEP

**Files:**
- Modify: `app/src/main/java/com/cadastroocr/app/ui/screens/ReviewScreen.kt`
- Modify: `app/src/main/java/com/cadastroocr/app/viewmodel/ClienteViewModel.kt`
- Test: `app/src/androidTest/java/com/cadastroocr/app/ui/screens/ReviewScreenTest.kt`

**Interfaces:** add `EstadoConsultaCep.Ocioso/Consultando/Encontrado/NaoEncontrado/Falhou`; expose `buscarEnderecoPorCep(cep, aoConcluir)` through the ViewModel; form fields receive `FocusRequester`, `ImeAction`, `KeyboardActions` and `TextFieldValue` where masks must keep selection.

- [ ] **Step 1: Write failing interaction tests**

```kotlin
@Test fun cpfValidoAvancaParaNascimento() { /* valid CPF => next field focused */ }
@Test fun emailInvalidoNaoAvancaNoNext() { /* malformed email => field remains focused */ }
@Test fun conflitoDoCepExigeEscolha() { /* divergent street => dialog exposes both values */ }
```

- [ ] **Step 2: Verify failure**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.cadastroocr.app.ui.screens.ReviewScreenTest`

Expected: FAIL because tags/focus behavior/CEP workflow do not exist.

- [ ] **Step 3: Implement focus and asynchronous CEP flow**

```kotlin
keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
keyboardActions = KeyboardActions(onNext = {
    if (EmailValidator.isValido(email)) telefoneFocus.requestFocus()
})
```

Create requesters in this exact order: nome, CPF, nascimento, RG, expedição, mãe, pai, e-mail, telefone, CEP, rua, número, complemento, bairro, cidade, UF, plano, valor, pagamento, vencimento. Name/RG/email move only on the keyboard action. CPF/date/phone/CEP auto-focus next only when changing from invalid to valid. Use `TextFieldValue` and the utility cursor mapping for CPF; RG remains raw text. Format telephone, CEP and compact dates through their pure utilities.

Cancel an old CEP lookup before creating a new one, display `Consultando CEP...`, and ignore responses whose normalized CEP no longer equals the current field. Show `✓ Endereço encontrado`, `CEP não encontrado. Verifique o número informado.`, or `Não foi possível consultar o CEP. Verifique sua conexão ou preencha o endereço manualmente.`. Fill only empty rua/bairro/cidade/UF; create a generalized conflict dialog for divergent existing values. Keep number and complement unchanged.

- [ ] **Step 4: Verify build and unit tests**

Run: `./gradlew testDebugUnitTest assembleDebug`

Expected: PASS and debug APK produced.

- [ ] **Step 5: Commit**

Run: `git add app/src/main/java/com/cadastroocr/app/ui/screens/ReviewScreen.kt app/src/main/java/com/cadastroocr/app/viewmodel/ClienteViewModel.kt app/src/androidTest/java/com/cadastroocr/app/ui/screens/ReviewScreenTest.kt`

Run: `git commit -m "feat: streamline registration form and CEP fill"`

### Task 5: Regressions and documentation

**Files:**
- Modify: `DOCUMENTACAO.md`

**Interfaces:** document ViaCEP as an optional network enhancement, manual fallback, UF and conflict behavior.

- [ ] **Step 1: Document the new behavior**

```markdown
O preenchimento por CEP usa ViaCEP somente quando há rede. Em caso de falha,
o endereço continua editável e o cadastro pode ser salvo manualmente.
```

- [ ] **Step 2: Run all automated checks**

Run: `./gradlew testDebugUnitTest assembleDebug`

Expected: PASS.

- [ ] **Step 3: Manual smoke test**

Check manual form, OCR form, conversation import, valid/invalid CPF, compact date, valid/invalid CEP, ViaCEP network failure, conflict selection, editing an existing record and UF in the client detail.

- [ ] **Step 4: Commit**

Run: `git add DOCUMENTACAO.md`

Run: `git commit -m "docs: describe accelerated registration flow"`
