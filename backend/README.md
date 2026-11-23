# Backend do Cadastro OCR (Cloudflare Workers)

Um único Worker que recebe o texto do OCR, chama a Gemini com a API key
protegida, e devolve os campos já estruturados. É a peça que existe
justamente para a API key **nunca** ficar dentro do APK.

**Por que Cloudflare Workers:** plano gratuito com 100 mil requisições por
dia, **para sempre**, sem cartão de crédito pra começar, sem prazo de teste,
sem "plano pago" pra ativar antes. Pro volume desse app, você nunca deve
nem chegar perto do limite gratuito.

## Pré-requisitos

- Conta Cloudflare (gratuita, só e-mail e senha): https://dash.cloudflare.com/sign-up
- Node.js instalado no seu computador (versão 18+): https://nodejs.org
- Uma API key da Gemini: https://aistudio.google.com/apikey (gratuita para criar; o uso é cobrado por token, mas é extremamente barato — frações de centavo por cadastro com o `gemini-2.5-flash-lite`)

## Passo a passo

### 1. Criar a conta Cloudflare

Só isso — email, senha, confirmar o e-mail. Não pede cartão de crédito pra usar o plano Free do Workers.

### 2. Instalar as dependências e logar

Dentro da pasta `backend/` (a mesma onde está este README):

```
cd backend
npm install
npx wrangler login
```
Isso abre o navegador pra você autorizar o Wrangler (CLI do Cloudflare) na sua conta.

### 3. Guardar os segredos (a API key nunca vai pro código)

```
npx wrangler secret put GEMINI_API_KEY
```
Cole a key que você gerou no Google AI Studio quando pedir.

```
npx wrangler secret put APP_SHARED_SECRET
```
Aqui você **inventa uma senha própria** (qualquer string longa e aleatória,
ex.: gere uma em https://www.uuidgenerator.net). Essa é a mesma senha que
você vai colocar em `AiConfig.APP_SHARED_SECRET` no app Android — ela não
tem relação com a Gemini, serve só pra impedir que outra pessoa descubra a
URL do seu Worker e fique usando sua cota de graça.

### 4. Deploy

```
npm run deploy
```

Ao final, o terminal mostra a URL do Worker, algo como:
```
https://cadastro-ocr-backend.SEU-USUARIO.workers.dev
```

### 5. Configurar o app Android

Copie essa URL e cole em:
`app/src/main/java/com/cadastroocr/app/ai/AiConfig.kt`

```kotlin
const val BACKEND_URL = "https://cadastro-ocr-backend.SEU-USUARIO.workers.dev"
const val APP_SHARED_SECRET = "a mesma senha do passo 3"
```

Recompile o app. Pronto — a partir daí, sempre que o celular tiver internet,
o app usa a IA pra interpretar documentos e conversas; sem internet, ele cai
automaticamente pro parser de regras local (nada quebra).

## Custo esperado

O Worker em si: **R$ 0,00**, dentro do limite de 100 mil requisições/dia
(o app precisaria de um volume de cadastros bem fora da realidade pra chegar
perto disso). O único custo de verdade é o da própria Gemini: com o
`gemini-2.5-flash-lite` (~$0,10 por milhão de tokens de entrada), cada
cadastro processado custa uma fração de centavo. Isso é cobrado direto pela
Google (Google AI Studio / Cloud Console), não pelo Cloudflare — vale
configurar um alerta de orçamento lá também: [console.cloud.google.com](https://console.cloud.google.com) → Faturamento → Orçamentos e alertas.

## Quando o modelo for desativado

O Google avisou que o `gemini-2.5-flash-lite` deve ser desligado não antes
de 16/out/2026, com `gemini-3.1-flash-lite` como substituto recomendado.
Quando isso acontecer, troque a constante `MODELO_GEMINI` em `src/index.js`
e rode `npm run deploy` de novo — não precisa mexer no app Android.

## Segurança — o que já está e o que pode melhorar

**Já implementado:**
- API key da Gemini fica só no Worker (Secrets do Cloudflare), nunca no APK
- Comunicação sempre por HTTPS
- Senha própria do app (`APP_SHARED_SECRET`) exigida em toda chamada
- Limite de tamanho de texto por requisição (evita abuso e custo alto)
- Nenhum dado é logado em texto puro com informação sensível

**Melhoria futura recomendada:** o `APP_SHARED_SECRET` fixo já é uma
proteção razoável pro volume atual do projeto. Se um dia isso crescer muito,
dá pra evoluir pra uma verificação mais forte (ex.: assinar cada requisição
com uma chave que roda no próprio app), mas não é necessário agora.
