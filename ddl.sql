-- ====================================================================================
-- SCRIPT DE CRIAÇÃO DO BANCO DE DADOS (DDL)
-- Banco: PostgreSQL (Supabase)
-- Padrão de Nomenclatura: pt-BR
-- ====================================================================================

-- Extensão necessária para geração de UUIDs automáticos
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ====================================================================================
-- 0. USUÁRIOS E PERFIS
-- (Link direto com o Supabase Auth)
-- ====================================================================================
CREATE TYPE tipo_perfil AS ENUM ('pai_admin', 'filho_membro', 'cuidador');

CREATE TABLE usuarios (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE, -- ID vinculado ao Supabase Auth
    nome VARCHAR(255) NOT NULL,
    perfil tipo_perfil NOT NULL,
    saldo_moedas INT DEFAULT 0, -- Usado na Gamificação das crianças
    avatar_url TEXT,
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- ====================================================================================
-- 1. FINANÇAS FAMILIARES
-- ====================================================================================
CREATE TYPE tipo_transacao AS ENUM ('receita', 'despesa_fixa', 'despesa_variavel', 'assinatura');
CREATE TYPE status_pagamento AS ENUM ('pendente', 'pago', 'atrasado');

CREATE TABLE categorias_financeiras (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(100) NOT NULL,
    icone VARCHAR(50),
    cor VARCHAR(20)
);

CREATE TABLE transacoes_financeiras (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID REFERENCES usuarios(id),
    categoria_id UUID REFERENCES categorias_financeiras(id),
    tipo tipo_transacao NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,
    status status_pagamento DEFAULT 'pendente',
    observacoes TEXT,
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE cofrinhos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(255) NOT NULL,
    meta_valor DECIMAL(10,2) NOT NULL,
    saldo_atual DECIMAL(10,2) DEFAULT 0.00,
    data_limite DATE,
    icone VARCHAR(50)
);

-- ====================================================================================
-- 2. PLANEJAMENTO DE EVENTOS & COTAÇÕES
-- ====================================================================================
CREATE TABLE eventos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(255) NOT NULL,
    data_evento TIMESTAMP WITH TIME ZONE NOT NULL,
    descricao TEXT,
    orcamento_estimado DECIMAL(10,2),
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TYPE status_rsvp AS ENUM ('pendente', 'confirmado', 'recusado');

CREATE TABLE convidados (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    evento_id UUID REFERENCES eventos(id) ON DELETE CASCADE,
    nome VARCHAR(255) NOT NULL,
    telefone VARCHAR(20),
    status status_rsvp DEFAULT 'pendente',
    restricoes_alimentares TEXT
);

CREATE TYPE status_cotacao AS ENUM ('em_analise', 'aprovada', 'rejeitada');

CREATE TABLE cotacoes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    evento_id UUID REFERENCES eventos(id) ON DELETE CASCADE, -- Cotações podem ser vinculadas a um evento
    item_ou_servico VARCHAR(255) NOT NULL,
    fornecedor VARCHAR(255) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    status status_cotacao DEFAULT 'em_analise',
    link_anexo TEXT, -- Link para o PDF/Foto no Supabase Storage
    observacoes TEXT
);

-- ====================================================================================
-- 3. COFRE DE SENHAS
-- ====================================================================================
CREATE TYPE nivel_cofre AS ENUM ('master', 'compartilhado');

CREATE TABLE cofre_credenciais (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    titulo VARCHAR(255) NOT NULL,
    nivel nivel_cofre NOT NULL,
    login_usuario VARCHAR(255),
    senha_criptografada TEXT NOT NULL, -- Dados sensíveis devem ser E2E no app
    url_acesso TEXT,
    notas_seguras TEXT,
    criado_por UUID REFERENCES usuarios(id)
);

-- ====================================================================================
-- 4. ROTINAS E GAMIFICAÇÃO
-- ====================================================================================
CREATE TYPE recorrencia_tarefa AS ENUM ('unica', 'diaria', 'semanal', 'mensal');

CREATE TABLE tarefas_rotinas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    titulo VARCHAR(255) NOT NULL,
    responsavel_id UUID REFERENCES usuarios(id),
    recorrencia recorrencia_tarefa NOT NULL,
    moedas_recompensa INT DEFAULT 0,
    data_limite TIMESTAMP WITH TIME ZONE,
    esta_concluida BOOLEAN DEFAULT FALSE,
    atualizado_em TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE loja_privilegios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    titulo VARCHAR(255) NOT NULL,
    custo_moedas INT NOT NULL,
    icone VARCHAR(50),
    estoque_disponivel INT -- Null para infinito
);

-- ====================================================================================
-- 5. INVENTÁRIO, MANUTENÇÕES E CONTATOS
-- ====================================================================================
CREATE TABLE agenda_contatos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(255) NOT NULL,
    especialidade VARCHAR(100), -- Ex: Encanador, Pediatra
    telefone VARCHAR(20) NOT NULL,
    eh_emergencia BOOLEAN DEFAULT FALSE,
    observacoes TEXT
);

CREATE TABLE inventario_itens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(255) NOT NULL,
    localizacao VARCHAR(255) NOT NULL,
    quantidade INT DEFAULT 1,
    data_compra DATE,
    data_fim_garantia DATE,
    caminho_nota_fiscal TEXT, -- Storage
    caminho_manual TEXT -- Storage
);

CREATE TABLE historico_manutencoes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    item_id UUID REFERENCES inventario_itens(id) ON DELETE SET NULL,
    prestador_id UUID REFERENCES agenda_contatos(id) ON DELETE SET NULL,
    descricao TEXT NOT NULL,
    data_servico DATE NOT NULL,
    custo DECIMAL(10,2),
    data_proxima_manutencao DATE -- Para agendamentos preventivos
);

-- ====================================================================================
-- 6. SAÚDE INTEGRAL
-- ====================================================================================
CREATE TABLE saude_checkins (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID REFERENCES usuarios(id) ON DELETE CASCADE,
    nivel_humor INT CHECK (nivel_humor BETWEEN 1 AND 5), -- 1: Muito Triste, 5: Muito Feliz
    notas_diario TEXT,
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE saude_fichas_medicas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo_sanguineo VARCHAR(5),
    alergias TEXT,
    medicamentos_uso_continuo TEXT,
    numero_plano_saude VARCHAR(50)
);

-- ====================================================================================
-- 7. COMPRAS INTELIGENTES (OCR / IA)
-- ====================================================================================
CREATE TABLE compras_recibos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    fornecedor VARCHAR(255),
    data_compra DATE,
    valor_total DECIMAL(10,2),
    caminho_imagem_recibo TEXT,
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE compras_itens_extraidos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recibo_id UUID REFERENCES compras_recibos(id) ON DELETE CASCADE,
    nome_produto VARCHAR(255) NOT NULL,
    quantidade DECIMAL(10,3) DEFAULT 1.000,
    valor_unitario DECIMAL(10,2) NOT NULL,
    categoria_sugerida VARCHAR(100)
);

-- ====================================================================================
-- 8. MURAL DE CONQUISTAS E ÁLBUM
-- ====================================================================================
CREATE TABLE mural_conquistas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL, -- Se for uma conquista individual
    data_conquista DATE NOT NULL,
    caminho_foto TEXT -- Vinculo com o álbum
);