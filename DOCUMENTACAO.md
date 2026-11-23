# Cadastro OCR — Documentação

Aplicativo Android nativo para automatizar o cadastro de clientes a partir
de fotos de RG/CNH, usando OCR on-device.

---

## 1. Tecnologias utilizadas e por quê

| Tecnologia | Por quê |
|---|---|
| **Kotlin + Jetpack Compose** | Stack oficial do Google, produtividade alta, UI declarativa fácil de manter e de deixar fiel ao Figma. |
| **Android Studio / Gradle** | Ferramenta padrão para gerar APK nativo, sem depender de serviços externos. |
| **ML Kit Text Recognition** | OCR que roda **100% no aparelho** (offline, sem enviar a imagem para nenhum servidor) — essencial para privacidade e para funcionar sem internet. |
| **Room Database** | Camada sobre SQLite, banco 100% local, os clientes continuam salvos mesmo fechando o app ou reiniciando o celular. |
| **CameraX** | API moderna de câmera do Android, permite construir uma experiência de captura própria (com moldura) em vez de abrir o app de câmera do sistema. |

Essa combinação cobre 100% dos requisitos do briefing (offline, OCR local,
banco local, fácil de manter) sem precisar de nenhum framework
multiplataforma ou serviço em nuvem.

---

## 2. Estrutura do projeto

```
CadastroOCR/
├── app/src/main/java/com/cadastroocr/app/
│   ├── MainActivity.kt          → ponto de entrada, pede permissão de câmera
│   ├── CadastroOcrApp.kt        → cria o banco/repositório uma única vez
│   ├── data/                    → Cliente (entidade), ClienteDao, AppDatabase, ClienteRepository
│   ├── ocr/                     → OcrAnalyzer (ML Kit), DocumentDataParser (interpretação), DadosExtraidos, DocumentImageStore
│   ├── camera/                  → CameraScreen.kt (câmera real com CameraX)
│   ├── viewmodel/               → ClienteViewModel.kt (toda a lógica de estado do app)
│   ├── ui/theme/                → Cores, tipografia e tema (visual do Figma)
│   ├── ui/components/           → ClientCard, CopyableField, ConfirmDeleteDialog
│   ├── ui/screens/               → Todas as telas (lista, novo cliente, revisão, ficha, etc.)
│   └── ui/navigation/           → NavGraph.kt (liga todas as telas)
└── app/src/main/res/            → strings, cores, ícone do app
```

---

## 3. Como executar o projeto

1. Instale o **Android Studio** (versão Koala ou mais recente): https://developer.android.com/studio
2. Abra a pasta `CadastroOCR` com **File → Open**.
3. Aguarde o Android Studio baixar as dependências (primeira sincronização do Gradle demora alguns minutos).

   > **Nota:** o arquivo binário `gradle-wrapper.jar` não veio incluído neste pacote (este ambiente não tem acesso aos servidores do Gradle para gerar o binário). Isso **não é um problema**: assim que você abrir o projeto no Android Studio, ele detecta o `gradle-wrapper.properties` e baixa a versão correta do Gradle automaticamente ao clicar em **"Sync Project with Gradle Files"** (ícone do elefante no topo).
4. Conecte um celular Android (com Depuração USB ativada) ou crie um emulador em **Device Manager**.
5. Clique em **Run ▶** (Shift+F10).

---

## 4. Como gerar o APK

### APK de teste (debug)
No menu: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**
O arquivo fica em: `app/build/outputs/apk/debug/app-debug.apk`

### APK final (release)
1. **Build → Generate Signed Bundle / APK...**
2. Escolha **APK**.
3. Crie (ou selecione) um keystore — é a "chave" que identifica seu app; guarde-a bem, você vai precisar dela para toda atualização futura.
4. Escolha a variante **release**.
5. O arquivo final fica em: `app/build/outputs/apk/release/app-release.apk`

Ou, pelo terminal, depois de o wrapper ser baixado pelo Android Studio:
```
./gradlew assembleRelease
```

### Como instalar o APK no celular
1. Copie o `.apk` para o celular (cabo USB, Google Drive, etc.).
2. No celular, permita "Instalar de fontes desconhecidas" quando solicitado.
3. Toque no arquivo `.apk` para instalar.

---

## 5. Como funciona o OCR (resumo técnico)

**Arquitetura atual: híbrida (OCR local + IA remota, com fallback automático).**

1. `CameraScreen.kt` / seletor de galeria capturam a imagem (documento ou print de conversa).
2. `OcrAnalyzer.kt` manda a imagem para o ML Kit, que devolve o texto bruto reconhecido — sempre no aparelho, sem internet.
3. Esse texto vai para `ClienteViewModel.interpretarTexto()`, que decide:
   - **Com internet**: chama o backend próprio (`GeminiInterpreter.kt` → Cloudflare Worker → Gemini) pedindo os dados já estruturados em JSON. Isso substitui a antiga abordagem de dezenas de regras/regex específicas por tipo de documento — a IA generaliza pra layouts novos sem precisar de código novo.
   - **Sem internet, ou se a IA falhar**: cai automaticamente para o parser de regras local (`DocumentDataParser.kt` para documento, `ConversationDataParser.kt` para conversa) — o app nunca fica sem conseguir preencher nada.
4. O resultado (`DadosExtraidos`) chega igual pra tela de revisão, não importa se veio da IA ou do parser local.
5. A foto original é sempre apagada logo depois da extração do texto — nunca é salva permanentemente, e nunca é enviada para o backend (só o **texto** é enviado, nunca a imagem/documento em si).

### Configurando a IA (opcional, mas recomendado)

O app funciona mesmo sem configurar a IA (usa só o parser local). Para ativar:
1. Siga o guia em `backend/README.md` para publicar o backend.
2. Preencha `app/src/main/java/com/cadastroocr/app/ai/AiConfig.kt` com a URL do backend e a senha do app.
3. Recompile.

### Por que texto, não a imagem inteira

O app manda pro backend só o **texto já reconhecido pelo OCR**, nunca a foto do documento. Isso é mais barato (texto consome muito menos do que imagem) e mais privado (a foto do RG/CNH nunca sai do aparelho — só os dados textuais).

---

## 6. Onde alterar cada coisa

| Quero mudar... | Arquivo |
|---|---|
| Campos do cliente (adicionar/remover) | `data/Cliente.kt`, `data/ClienteDao.kt` (se afetar busca) e `ui/screens/ReviewScreen.kt` |
| Cores do app | `ui/theme/Color.kt` |
| Nome do aplicativo | `res/values/strings.xml` (`app_name`) e `applicationId`/`namespace` em `app/build.gradle.kts` |
| Lógica de interpretação do OCR (novas palavras-chave, novos documentos) | `ocr/DocumentDataParser.kt` |
| Validações (CPF, e-mail) | `util/CpfValidator.kt`, `util/EmailValidator.kt` |
| Banco de dados (nova tabela, migração) | `data/AppDatabase.kt` |
| Telas / layout | `ui/screens/*.kt` |
| Navegação entre telas | `ui/navigation/NavGraph.kt` |
| Ícone do app | `res/drawable/ic_launcher_foreground.xml` e `ic_launcher_background.xml` |

---

## 7. Escopo desta primeira versão

Incluído: OCR real + cadastro + banco local + busca + edição + exclusão +
cópia individual por campo — exatamente o escopo definido no briefing.

Não incluído de propósito (para manter a v1 enxuta, conforme pedido):
exportação para Excel/CSV, compartilhamento, botão de "copiar tudo",
WhatsApp/e-mail automático, sincronização em nuvem, sistema de usuários.

---

## 8. Privacidade

- Nenhum dado é enviado para servidores externos — o OCR roda localmente via ML Kit.
- A imagem do documento fica apenas em cache privado do app (`cacheDir/documentos/`) e é apagada assim que o texto é extraído.
- Nenhum CPF, endereço ou dado pessoal é gravado em log.
