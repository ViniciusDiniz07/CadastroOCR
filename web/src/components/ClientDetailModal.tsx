import React, { useState } from 'react';
import { Cliente } from '../types/client';
import { X, Copy, Check, Edit2, Trash2, User, FileText, Phone, MapPin, CreditCard } from 'lucide-react';

interface ClientDetailModalProps {
  cliente: Cliente | null;
  isOpen: boolean;
  onClose: () => void;
  onEdit: (cliente: Cliente) => void;
  onDelete: (cliente: Cliente) => void;
}

export const ClientDetailModal: React.FC<ClientDetailModalProps> = ({
  cliente,
  isOpen,
  onClose,
  onEdit,
  onDelete,
}) => {
  const [copiedField, setCopiedField] = useState<string | null>(null);

  if (!isOpen || !cliente) return null;

  const handleCopy = (label: string, value: string) => {
    if (!value) return;
    navigator.clipboard.writeText(value);
    setCopiedField(label);
    setTimeout(() => setCopiedField(null), 1800);
  };

  const renderCopyableField = (label: string, value: string | undefined) => {
    const isCopied = copiedField === label;
    const displayValue = value && value.trim() ? value : '—';
    const canCopy = Boolean(value && value.trim());

    return (
      <div className="detail-field">
        <div>
          <div className="detail-label">{label}</div>
          <div className="detail-value">{displayValue}</div>
        </div>
        {canCopy && (
          <button
            className={`copy-badge ${isCopied ? 'copied' : ''}`}
            onClick={() => handleCopy(label, value!)}
            title={`Copiar ${label}`}
          >
            {isCopied ? (
              <>
                <Check size={13} />
                <span>Copiado!</span>
              </>
            ) : (
              <>
                <Copy size={13} />
                <span>Copiar</span>
              </>
            )}
          </button>
        )}
      </div>
    );
  };

  return (
    <div className="modal-overlay">
      <div className="modal-card" style={{ maxWidth: '850px' }}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div
              style={{
                width: '44px',
                height: '44px',
                borderRadius: 'var(--radius-full)',
                backgroundColor: 'var(--accent-green-bg)',
                color: 'var(--accent-green)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 'bold',
                fontSize: '18px',
              }}
            >
              {cliente.nome_completo ? cliente.nome_completo.charAt(0).toUpperCase() : '?'}
            </div>
            <div>
              <h2 className="font-heading" style={{ fontSize: '20px' }}>
                {cliente.nome_completo || 'Cliente sem nome'}
              </h2>
              <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                CPF: {cliente.cpf || 'Não informado'}
              </div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <button
              className="btn btn-secondary"
              onClick={() => {
                onClose();
                onEdit(cliente);
              }}
            >
              <Edit2 size={16} />
              Editar
            </button>
            <button
              className="btn btn-danger"
              onClick={() => {
                if (window.confirm(`Deseja realmente excluir ${cliente.nome_completo}?`)) {
                  onDelete(cliente);
                  onClose();
                }
              }}
            >
              <Trash2 size={16} />
              Excluir
            </button>
            <button className="btn-icon" onClick={onClose}>
              <X size={20} />
            </button>
          </div>
        </div>

        <div className="modal-body">
          {/* Seção 1: Dados Pessoais */}
          <div className="form-section">
            <div className="form-section-title">
              <User size={16} />
              Dados Pessoais
            </div>
            <div className="form-grid">
              {renderCopyableField('Nome Completo', cliente.nome_completo)}
              {renderCopyableField('CPF', cliente.cpf)}
              {renderCopyableField('Data de Nascimento', cliente.data_nascimento)}
              {renderCopyableField('Nome da Mãe', cliente.nome_da_mae)}
              {renderCopyableField('Nome do Pai', cliente.nome_do_pai)}
            </div>
          </div>

          {/* Seção 2: Documento */}
          <div className="form-section">
            <div className="form-section-title">
              <FileText size={16} />
              Documento de Identificação
            </div>
            <div className="form-grid">
              {renderCopyableField('RG', cliente.rg)}
              {renderCopyableField('Data de Expedição', cliente.data_expedicao_rg)}
            </div>
          </div>

          {/* Seção 3: Contato */}
          <div className="form-section">
            <div className="form-section-title">
              <Phone size={16} />
              Informações de Contato
            </div>
            <div className="form-grid">
              {renderCopyableField('E-mail', cliente.email)}
              {renderCopyableField('Telefone / Contato', cliente.telefone)}
            </div>
          </div>

          {/* Seção 4: Endereço */}
          <div className="form-section">
            <div className="form-section-title">
              <MapPin size={16} />
              Endereço Completo
            </div>
            <div className="form-grid">
              {renderCopyableField('Rua', cliente.rua)}
              {renderCopyableField('Número', cliente.numero)}
              {renderCopyableField('Complemento', cliente.complemento)}
              {renderCopyableField('CEP', cliente.cep)}
              {renderCopyableField('Bairro', cliente.bairro)}
              {renderCopyableField('Cidade', cliente.cidade)}
            </div>
          </div>

          {/* Seção 5: Plano e Pagamento */}
          <div className="form-section">
            <div className="form-section-title">
              <CreditCard size={16} />
              Plano e Pagamento
            </div>
            <div className="form-grid">
              {renderCopyableField('Plano', cliente.plano)}
              {renderCopyableField('Valor (R$)', cliente.valor_plano)}
              {renderCopyableField('Forma de Pagamento', cliente.forma_pagamento)}
              {renderCopyableField('Dia de Vencimento', cliente.dia_vencimento)}
            </div>
          </div>
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
};
