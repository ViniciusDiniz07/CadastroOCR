import React, { useState, useEffect } from 'react';
import { Cliente, ExtractedOcrData } from '../types/client';
import { formatCpf, isValidCpf, isValidEmail, formatPhone, formatCep } from '../lib/validators';
import { X, Save, User, FileText, Phone, MapPin, CreditCard, AlertCircle } from 'lucide-react';

interface ClientFormModalProps {
  clienteInicial: Cliente | null;
  dadosOcrPrePreenchidos?: ExtractedOcrData | null;
  isOpen: boolean;
  onClose: () => void;
  onSave: (cliente: Partial<Cliente>) => Promise<void>;
}

export const ClientFormModal: React.FC<ClientFormModalProps> = ({
  clienteInicial,
  dadosOcrPrePreenchidos,
  isOpen,
  onClose,
  onSave,
}) => {
  const [formData, setFormData] = useState<Partial<Cliente>>({
    nome_completo: '',
    cpf: '',
    data_nascimento: '',
    nome_da_mae: '',
    nome_do_pai: '',
    rg: '',
    data_expedicao_rg: '',
    email: '',
    telefone: '',
    rua: '',
    numero: '',
    complemento: '',
    cep: '',
    bairro: '',
    cidade: '',
    plano: '',
    valor_plano: '',
    forma_pagamento: '',
    dia_vencimento: '',
  });

  const [attemptedSave, setAttemptedSave] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (clienteInicial) {
      setFormData(clienteInicial);
    } else if (dadosOcrPrePreenchidos) {
      setFormData({
        nome_completo: dadosOcrPrePreenchidos.nomeCompleto || '',
        cpf: formatCpf(dadosOcrPrePreenchidos.cpf || ''),
        data_nascimento: dadosOcrPrePreenchidos.dataNascimento || '',
        nome_da_mae: dadosOcrPrePreenchidos.nomeDaMae || '',
        nome_do_pai: dadosOcrPrePreenchidos.nomeDoPai || '',
        rg: dadosOcrPrePreenchidos.rg || '',
        data_expedicao_rg: dadosOcrPrePreenchidos.dataExpedicaoRg || '',
        email: dadosOcrPrePreenchidos.email || '',
        telefone: formatPhone(dadosOcrPrePreenchidos.telefone || ''),
        rua: dadosOcrPrePreenchidos.rua || '',
        numero: dadosOcrPrePreenchidos.numero || '',
        complemento: dadosOcrPrePreenchidos.complemento || '',
        cep: formatCep(dadosOcrPrePreenchidos.cep || ''),
        bairro: dadosOcrPrePreenchidos.bairro || '',
        cidade: dadosOcrPrePreenchidos.cidade || '',
        plano: '',
        valor_plano: '',
        forma_pagamento: '',
        dia_vencimento: '',
      });
    } else {
      setFormData({
        nome_completo: '',
        cpf: '',
        data_nascimento: '',
        nome_da_mae: '',
        nome_do_pai: '',
        rg: '',
        data_expedicao_rg: '',
        email: '',
        telefone: '',
        rua: '',
        numero: '',
        complemento: '',
        cep: '',
        bairro: '',
        cidade: '',
        plano: '',
        valor_plano: '',
        forma_pagamento: '',
        dia_vencimento: '',
      });
    }
    setAttemptedSave(false);
    setErrorMessage(null);
  }, [clienteInicial, dadosOcrPrePreenchidos, isOpen]);

  if (!isOpen) return null;

  const handleChange = (field: keyof Cliente, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const isNameInvalid = !formData.nome_completo || formData.nome_completo.trim() === '';
  const isCpfInvalid = Boolean(formData.cpf && formData.cpf.trim() !== '' && !isValidCpf(formData.cpf));
  const isEmailInvalid = Boolean(formData.email && !isValidEmail(formData.email));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setAttemptedSave(true);

    if (isNameInvalid) {
      setErrorMessage('O Nome Completo é obrigatório');
      return;
    }

    if (isCpfInvalid) {
      setErrorMessage('CPF inválido. Verifique os dígitos digitados.');
      return;
    }

    if (isEmailInvalid) {
      setErrorMessage('E-mail em formato inválido.');
      return;
    }

    setSaving(true);
    setErrorMessage(null);

    try {
      await onSave(formData);
      onClose();
    } catch (err: any) {
      setErrorMessage(err.message || 'Erro ao salvar cliente');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-card" style={{ maxWidth: '900px' }}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <h2 className="font-heading" style={{ fontSize: '20px' }}>
              {clienteInicial ? 'Editar Cliente' : 'Novo Cadastro de Cliente'}
            </h2>
          </div>
          <button className="btn-icon" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'contents' }}>
          <div className="modal-body">
            {errorMessage && (
              <div
                style={{
                  background: 'var(--danger-red-bg)',
                  border: '1px solid var(--danger-red)',
                  color: 'var(--danger-red)',
                  padding: '12px 16px',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '13px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  marginBottom: '20px',
                }}
              >
                <AlertCircle size={16} />
                <span>{errorMessage}</span>
              </div>
            )}

            {/* Seção 1: Dados Pessoais */}
            <div className="form-section">
              <div className="form-section-title">
                <User size={16} />
                Dados Pessoais
              </div>
              <div className="form-grid">
                <div className="form-group col-span-2">
                  <label className="form-label">Nome Completo *</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.nome_completo || ''}
                    onChange={(e) => handleChange('nome_completo', e.target.value)}
                    placeholder="Ex: Maria da Silva"
                    required
                  />
                  {attemptedSave && isNameInvalid && (
                    <span style={{ color: 'var(--danger-red)', fontSize: '12px' }}>Informe o nome completo</span>
                  )}
                </div>

                <div className="form-group">
                  <label className="form-label">CPF</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.cpf || ''}
                    onChange={(e) => handleChange('cpf', formatCpf(e.target.value))}
                    placeholder="000.000.000-00"
                    maxLength={14}
                  />
                  {isCpfInvalid && (
                    <span style={{ color: 'var(--danger-red)', fontSize: '12px' }}>CPF inválido</span>
                  )}
                </div>

                <div className="form-group">
                  <label className="form-label">Data de Nascimento</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.data_nascimento || ''}
                    onChange={(e) => handleChange('data_nascimento', e.target.value)}
                    placeholder="DD/MM/AAAA"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Nome da Mãe</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.nome_da_mae || ''}
                    onChange={(e) => handleChange('nome_da_mae', e.target.value)}
                    placeholder="Nome completo da mãe"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Nome do Pai</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.nome_do_pai || ''}
                    onChange={(e) => handleChange('nome_do_pai', e.target.value)}
                    placeholder="Nome completo do pai"
                  />
                </div>
              </div>
            </div>

            {/* Seção 2: Documento */}
            <div className="form-section">
              <div className="form-section-title">
                <FileText size={16} />
                Documento
              </div>
              <div className="form-grid">
                <div className="form-group">
                  <label className="form-label">RG / Identidade</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.rg || ''}
                    onChange={(e) => handleChange('rg', e.target.value)}
                    placeholder="Ex: 12.345.678-9"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Data de Expedição</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.data_expedicao_rg || ''}
                    onChange={(e) => handleChange('data_expedicao_rg', e.target.value)}
                    placeholder="DD/MM/AAAA"
                  />
                </div>
              </div>
            </div>

            {/* Seção 3: Contato */}
            <div className="form-section">
              <div className="form-section-title">
                <Phone size={16} />
                Contato
              </div>
              <div className="form-grid">
                <div className="form-group">
                  <label className="form-label">E-mail</label>
                  <input
                    type="email"
                    className="form-input"
                    value={formData.email || ''}
                    onChange={(e) => handleChange('email', e.target.value)}
                    placeholder="email@exemplo.com"
                  />
                  {isEmailInvalid && (
                    <span style={{ color: 'var(--danger-red)', fontSize: '12px' }}>E-mail em formato inválido</span>
                  )}
                </div>

                <div className="form-group">
                  <label className="form-label">Telefone / WhatsApp</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.telefone || ''}
                    onChange={(e) => handleChange('telefone', formatPhone(e.target.value))}
                    placeholder="(00) 00000-0000"
                    maxLength={15}
                  />
                </div>
              </div>
            </div>

            {/* Seção 4: Endereço */}
            <div className="form-section">
              <div className="form-section-title">
                <MapPin size={16} />
                Endereço
              </div>
              <div className="form-grid">
                <div className="form-group col-span-2">
                  <label className="form-label">Rua / Logradouro</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.rua || ''}
                    onChange={(e) => handleChange('rua', e.target.value)}
                    placeholder="Nome da rua ou avenida"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Número</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.numero || ''}
                    onChange={(e) => handleChange('numero', e.target.value)}
                    placeholder="123"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Complemento</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.complemento || ''}
                    onChange={(e) => handleChange('complemento', e.target.value)}
                    placeholder="Apto 101, Bloco B"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">CEP</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.cep || ''}
                    onChange={(e) => handleChange('cep', formatCep(e.target.value))}
                    placeholder="00000-000"
                    maxLength={9}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Bairro</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.bairro || ''}
                    onChange={(e) => handleChange('bairro', e.target.value)}
                    placeholder="Nome do bairro"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Cidade</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.cidade || ''}
                    onChange={(e) => handleChange('cidade', e.target.value)}
                    placeholder="Nome da cidade"
                  />
                </div>
              </div>
            </div>

            {/* Seção 5: Plano e Pagamento */}
            <div className="form-section">
              <div className="form-section-title">
                <CreditCard size={16} />
                Plano e Pagamento
              </div>
              <div className="form-grid">
                <div className="form-group">
                  <label className="form-label">Plano</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.plano || ''}
                    onChange={(e) => handleChange('plano', e.target.value)}
                    placeholder="Ex: Fibra 500 Mega"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Valor (R$)</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.valor_plano || ''}
                    onChange={(e) => handleChange('valor_plano', e.target.value)}
                    placeholder="Ex: 99,90"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Forma de Pagamento</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.forma_pagamento || ''}
                    onChange={(e) => handleChange('forma_pagamento', e.target.value)}
                    placeholder="Ex: Cartão / Boleto / PIX"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Dia de Vencimento</label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.dia_vencimento || ''}
                    onChange={(e) => handleChange('dia_vencimento', e.target.value)}
                    placeholder="Ex: 10"
                  />
                </div>
              </div>
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose} disabled={saving}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              <Save size={16} />
              {saving ? 'Salvando...' : clienteInicial ? 'Salvar Alterações' : 'Cadastrar Cliente'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
