# Cadastro OCR — Guia de Sincronização Multi-Dispositivo (Android + Web)

Este documento explica como funciona a arquitetura de sincronização em tempo real entre celulares Android e computadores (Web), como configurar o banco online e as políticas de segurança.

---

## 1. Visão Geral da Arquitetura

```
               ☁️ BANCO CENTRAL ONLINE (Supabase / PostgreSQL)
                              │
               ┌──────────────┼──────────────┐
               ▼              ▼              ▼
           📱 CELULAR 1   📱 CELULAR 2    💻 PC (WEB)
             (Room DB)      (Room DB)      (Navegador)
```

- **Android (Celular 1 e Celular 2)**:
  - Utiliza o banco local **Room (SQLite)** como fonte local da verdade (*Local-First*).
  - Funciona 100% offline (criar, editar, pesquisar, excluir, OCR).
  - Possui um motor em segundo plano (`SyncManager`) que monitora a conectividade de rede. Ao detectar internet, envia alterações pendentes (*Push*) e recebe novos cadastros da nuvem (*Pull*).
- **Computador (Web / PC)**:
  - Aplicação Web responsiva em React + Vite + TypeScript.
  - Conecta-se diretamente ao Supabase via WebSockets (*Realtime*).
  - Ao alterar ou cadastrar um cliente no PC, todos os celulares conectados recebem a atualização instantaneamente.

---

## 2. Passo a Passo para Configurar o Banco Online (Supabase)

O Supabase oferece plano gratuito para sempre (500 MB de banco, 50.000 usuários ativos e requisições ilimitadas a **R$ 0,00 de custo**).

### Passo 1: Criar o Projeto no Supabase
1. Acesse: https://supabase.com e faça login (com GitHub ou e-mail).
2. Clique em **"New Project"**.
3. Dê um nome ao projeto (ex.: `cadastro-ocr`) e defina uma senha forte para o banco.
4. Selecione a região **"South America (São Paulo)"** para menor latência no Brasil.
5. Clique em **"Create new project"** e aguarde ~1 minuto.

### Passo 2: Executar o Script SQL
1. No painel do seu projeto no Supabase, clique no menu lateral em **"SQL Editor"**.
2. Clique em **"New query"**.
3. Abra o arquivo `backend/supabase/schema.sql` deste projeto, copie todo o conteúdo e cole no editor do Supabase.
4. Clique em **"Run"** (ou Ctrl+Enter).
5. Pronto! A tabela `clientes`, as políticas de segurança RLS e a sincronização Realtime estão configuradas.

### Passo 3: Obter a URL e a Chave Pública (Anon Key)
1. No menu lateral esquerdo do Supabase, clique no ícone de engrenagem **"Project Settings"**.
2. Clique em **"API"**.
3. Copie:
   - **Project URL** (ex.: `https://abcdefghij.supabase.co`)
   - **Project API keys -> `anon` / `public`** (chave longa começando com `eyJ...`)

---

## 3. Configurar o Aplicativo Android

Abra o arquivo:
`app/src/main/java/com/cadastroocr/app/auth/AuthConfig.kt`

Substitua com suas credenciais:

```kotlin
object AuthConfig {
    const val SUPABASE_URL = "https://SEU_PROJETO.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

Recompile o APK. Pronto! O aplicativo Android agora sincronizará automaticamente.

---

## 4. Configurar e Rodar a Versão Web para PC

1. Abra a pasta `web/`:
   ```bash
   cd web
   ```
2. Crie um arquivo `.env` com suas chaves:
   ```env
   VITE_SUPABASE_URL=https://SEU_PROJETO.supabase.co
   VITE_SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   VITE_BACKEND_URL=https://cadastro-ocr-backend.SEU-USUARIO.workers.dev
   VITE_APP_SHARED_SECRET=sua-senha-do-worker
   ```
3. Instale as dependências e inicie:
   ```bash
   npm install
   npm run dev
   ```
4. O navegador abrirá em `http://localhost:3000`.

---

## 5. Estratégia de Resolução de Conflitos

- **Identificador Único Global (`uuid`)**: Cada cliente criado recebe um UUID v4 no momento do cadastro, eliminando o risco de colisão de IDs entre dispositivos.
- **Atualização Granular a Nível de Campos (*Field-Level Patch*)**:
  - Se o Celular 1 alterar o *telefone* de um cliente e o PC alterar o *endereço* do mesmo cliente ao mesmo tempo, ambas as alterações são gravadas no banco e preservadas.
- **Colisão no Mesmo Campo (*Last-Write-Wins*)**:
  - Caso dois dispositivos alterem exatamente o mesmo campo simultaneamente, o registro com o timestamp `atualizadoEm` mais recente prevalece.
- **Proteção de Edições Locais Pendentes no Android**:
  - Registros marcados como `PENDING_UPDATE` no Room local nunca são sobrescritos por dados desatualizados da nuvem antes de suas alterações serem enviadas.

---

## 6. Segurança e Privacidade

- **Criptografia em Trânsito**: 100% da comunicação é feita sob HTTPS / TLS 1.3.
- **Row Level Security (RLS)**: O banco PostgreSQL rejeita qualquer tentativa de acesso não autenticado. Apenas requisições com token JWT válido emitido pelo Supabase Auth podem consultar ou alterar clientes.
- **Segredos Protegidos**: Chaves administrativas (`service_role_key`) e a chave da Gemini nunca são incluídas no código do APK nem no JavaScript do frontend.
- **Zero Vazamento em Logs**: Dados sensíveis (CPF, RG, Filiação, Endereço) não são impressos no logcat do Android nem no console do navegador.
