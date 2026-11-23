# Cadastro OCR — Painel Web para Computador (PC)

Aplicação Web responsiva desenvolvida para computadores, permitindo gerenciar, pesquisar, cadastrar, editar e sincronizar clientes em tempo real com os celulares Android.

---

## Recursos da Versão Web

- ⚡ **Sincronização em Tempo Real**: Qualquer cliente cadastrado ou alterado no Celular 1 ou Celular 2 aparece instantaneamente no PC através de WebSockets (Supabase Realtime).
- 🔒 **Autenticação Segura**: Login por e-mail e senha com JWT e Row Level Security (RLS) no PostgreSQL.
- 📋 **Ficha Completa com Cópia por Campo**: Visualização organizada de todos os 19 campos cadastrais com botões de cópia individual em 1 clique e feedback visual imediato.
- 🔍 **Pesquisa Universal Instantânea**: Filtro em tempo real por Nome, CPF, Telefone, E-mail, Cidade ou Plano.
- 🤖 **Importador OCR & IA no Navegador**: Arraste uma foto de RG/CNH ou print do WhatsApp para extrair os dados com OCR e IA automaticamente.
- 🎨 **Visual Fiel ao Aplicativo Mobile**: Identidade visual escura e moderna (`#0E1A16`, `#16241F`, `#2ECC71`).

---

## Como Executar

### 1. Instalar as dependências

```bash
cd web
npm install
```

### 2. Configurar as credenciais do Supabase (Opcional para demonstração)

Crie um arquivo `.env` na pasta `web/` com suas credenciais do Supabase:

```env
VITE_SUPABASE_URL=https://SEU_PROJETO.supabase.co
VITE_SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
VITE_BACKEND_URL=https://cadastro-ocr-backend.SEU-USUARIO.workers.dev
VITE_APP_SHARED_SECRET=sua-senha-do-worker
```

> **Nota:** A aplicação possui modo offline/local embutido — você pode testar todas as telas e cadastros mesmo antes de configurar as credenciais online.

### 3. Iniciar o servidor de desenvolvimento

```bash
npm run dev
```

Abra seu navegador em: `http://localhost:3000`

---

## Como Fazer Deploy Gratuito (Produção)

A versão web é uma SPA estática otimizada que pode ser hospedada gratuitamente em qualquer serviço:
- **Cloudflare Pages**: `npm run build` -> pasta de saída `dist/`
- **Vercel**: Importe o repositório e selecione a pasta `web`
- **Netlify**: Arraste a pasta `dist` gerada
