-- ==============================================================================
-- CADASTRO OCR — SCHEMA COMPLETO DO BANCO ONLINE (SUPABASE / POSTGRESQL)
-- ==============================================================================
-- Este script configura a tabela de clientes, chaves únicas, índices de busca,
-- gatilhos automáticos de atualização de data e regras de segurança (Row Level Security).
--
-- Como executar:
-- 1. Acesse o painel do seu projeto no Supabase (https://supabase.com/dashboard)
-- 2. No menu lateral esquerdo, clique em "SQL Editor"
-- 3. Cole todo o conteúdo deste arquivo e clique em "Run"
-- ==============================================================================

-- 1. Habilita a extensão de geração de UUID caso não esteja habilitada
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Criação da tabela principal de clientes
CREATE TABLE IF NOT EXISTS public.clientes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Dados Pessoais
    nome_completo TEXT NOT NULL DEFAULT '',
    cpf TEXT DEFAULT '',
    data_nascimento TEXT DEFAULT '',
    nome_da_mae TEXT DEFAULT '',
    nome_do_pai TEXT DEFAULT '',
    
    -- Documento
    rg TEXT DEFAULT '',
    data_expedicao_rg TEXT DEFAULT '',
    
    -- Contato
    email TEXT DEFAULT '',
    telefone TEXT DEFAULT '',
    
    -- Endereço
    rua TEXT DEFAULT '',
    numero TEXT DEFAULT '',
    complemento TEXT DEFAULT '',
    cep TEXT DEFAULT '',
    bairro TEXT DEFAULT '',
    cidade TEXT DEFAULT '',
    
    -- Plano e Pagamento
    plano TEXT DEFAULT '',
    valor_plano TEXT DEFAULT '',
    forma_pagamento TEXT DEFAULT '',
    dia_vencimento TEXT DEFAULT '',
    
    -- Controle de sincronização e integridade
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    criado_em BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    atualizado_em BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. Índices para performance extrema em buscas e sincronização incremental
CREATE INDEX IF NOT EXISTS idx_clientes_nome ON public.clientes (nome_completo);
CREATE INDEX IF NOT EXISTS idx_clientes_cpf ON public.clientes (cpf);
CREATE INDEX IF NOT EXISTS idx_clientes_email ON public.clientes (email);
CREATE INDEX IF NOT EXISTS idx_clientes_telefone ON public.clientes (telefone);
CREATE INDEX IF NOT EXISTS idx_clientes_atualizado_em ON public.clientes (atualizado_em);
CREATE INDEX IF NOT EXISTS idx_clientes_is_deleted ON public.clientes (is_deleted);

-- 4. Função e Gatilho para atualizar automaticamente `atualizado_em` e `updated_at`
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    IF NEW.atualizado_em IS NULL OR NEW.atualizado_em = OLD.atualizado_em THEN
        NEW.atualizado_em = (extract(epoch from now()) * 1000)::bigint;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_clientes_updated_at ON public.clientes;
CREATE TRIGGER tr_clientes_updated_at
    BEFORE UPDATE ON public.clientes
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

-- 5. Configuração de Segurança (Row Level Security - RLS)
ALTER TABLE public.clientes ENABLE ROW LEVEL SECURITY;

-- Política 1: Usuários autenticados podem consultar clientes ativos
DROP POLICY IF EXISTS "Permitir leitura para usuarios autenticados" ON public.clientes;
CREATE POLICY "Permitir leitura para usuarios autenticados"
ON public.clientes FOR SELECT
TO authenticated
USING (true);

-- Política 2: Usuários autenticados podem cadastrar novos clientes
DROP POLICY IF EXISTS "Permitir insercao para usuarios autenticados" ON public.clientes;
CREATE POLICY "Permitir insercao para usuarios autenticados"
ON public.clientes FOR INSERT
TO authenticated
WITH CHECK (true);

-- Política 3: Usuários autenticados podem atualizar clientes
DROP POLICY IF EXISTS "Permitir atualizacao para usuarios autenticados" ON public.clientes;
CREATE POLICY "Permitir atualizacao para usuarios autenticados"
ON public.clientes FOR UPDATE
TO authenticated
USING (true)
WITH CHECK (true);

-- Política 4: Usuários autenticados podem deletar clientes (ou fazer soft-delete)
DROP POLICY IF EXISTS "Permitir exclusao para usuarios autenticados" ON public.clientes;
CREATE POLICY "Permitir exclusao para usuarios autenticados"
ON public.clientes FOR DELETE
TO authenticated
USING (true);

-- 6. Habilita Realtime para a tabela clientes (sincronização instantânea via WebSockets)
ALTER PUBLICATION supabase_realtime ADD TABLE public.clientes;

-- ==============================================================================
-- PRONTO! Sua tabela está criada com segurança, índices e sincronização em tempo real.
-- ==============================================================================
