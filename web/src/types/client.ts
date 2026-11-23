export interface Cliente {
  id: string; // UUID universal
  
  // Dados pessoais
  nome_completo: string;
  cpf: string;
  data_nascimento: string;
  nome_da_mae: string;
  nome_do_pai: string;

  // Documento
  rg: string;
  data_expedicao_rg: string;

  // Contato
  email: string;
  telefone: string;

  // Endereço
  rua: string;
  numero: string;
  complemento: string;
  cep: string;
  bairro: string;
  cidade: string;

  // Plano e pagamento
  plano: string;
  valor_plano: string;
  forma_pagamento: string;
  dia_vencimento: string;

  // Metadados
  is_deleted?: boolean;
  criado_em?: number;
  atualizado_em?: number;
  created_at?: string;
  updated_at?: string;
}

export interface ExtractedOcrData {
  nomeCompleto: string;
  cpf: string;
  dataNascimento: string;
  nomeDaMae: string;
  nomeDoPai: string;
  rg: string;
  dataExpedicaoRg: string;
  email: string;
  telefone: string;
  rua: string;
  numero: string;
  complemento?: string;
  cep: string;
  bairro: string;
  cidade: string;
  textoBrutoReconhecido?: string;
}
