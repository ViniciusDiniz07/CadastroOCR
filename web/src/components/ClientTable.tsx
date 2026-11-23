import React, { useState } from 'react';
import { Cliente } from '../types/client';
import { Eye, Edit2, Trash2, Copy, Check } from 'lucide-react';

interface ClientTableProps {
  clientes: Cliente[];
  onViewDetails: (cliente: Cliente) => void;
  onEdit: (cliente: Cliente) => void;
  onDelete: (cliente: Cliente) => void;
}

export const ClientTable: React.FC<ClientTableProps> = ({
  clientes,
  onViewDetails,
  onEdit,
  onDelete,
}) => {
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const handleCopy = (key: string, text: string) => {
    if (!text) return;
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 1500);
  };

  const getInitials = (name: string) => {
    const parts = name.trim().split(' ').filter(Boolean);
    if (parts.length === 0) return '?';
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  };

  if (clientes.length === 0) {
    return (
      <div
        className="table-container"
        style={{
          padding: '60px 20px',
          textAlign: 'center',
          color: 'var(--text-secondary)',
        }}
      >
        <div style={{ fontSize: '16px', fontWeight: 500, marginBottom: '6px' }}>
          Nenhum cliente cadastrado ou encontrado
        </div>
        <div style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
          Cadastre um cliente manualmente ou utilize o importador OCR para começar.
        </div>
      </div>
    );
  }

  return (
    <div className="table-container">
      <table className="client-table">
        <thead>
          <tr>
            <th>Cliente</th>
            <th>CPF</th>
            <th>Telefone</th>
            <th>E-mail</th>
            <th>Cidade</th>
            <th>Plano / Valor</th>
            <th>Pagamento</th>
            <th>Venc.</th>
            <th style={{ textAlign: 'right' }}>Ações</th>
          </tr>
        </thead>
        <tbody>
          {clientes.map((c) => (
            <tr key={c.id}>
              {/* Nome */}
              <td>
                <div style={{ display: 'flex', alignItems: 'center' }}>
                  <div className="client-avatar">{getInitials(c.nome_completo || '')}</div>
                  <div>
                    <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                      {c.nome_completo || 'Sem nome'}
                    </div>
                    {c.data_nascimento && (
                      <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                        Nasc: {c.data_nascimento}
                      </div>
                    )}
                  </div>
                </div>
              </td>

              {/* CPF com Cópia Rápida */}
              <td>
                {c.cpf ? (
                  <button
                    className={`copy-badge ${copiedKey === `cpf-${c.id}` ? 'copied' : ''}`}
                    onClick={() => handleCopy(`cpf-${c.id}`, c.cpf)}
                    title="Clique para copiar o CPF"
                  >
                    {copiedKey === `cpf-${c.id}` ? <Check size={12} /> : <Copy size={12} />}
                    <span>{c.cpf}</span>
                  </button>
                ) : (
                  <span style={{ color: 'var(--text-muted)' }}>—</span>
                )}
              </td>

              {/* Telefone com Cópia Rápida */}
              <td>
                {c.telefone ? (
                  <button
                    className={`copy-badge ${copiedKey === `tel-${c.id}` ? 'copied' : ''}`}
                    onClick={() => handleCopy(`tel-${c.id}`, c.telefone)}
                    title="Clique para copiar o telefone"
                  >
                    {copiedKey === `tel-${c.id}` ? <Check size={12} /> : <Copy size={12} />}
                    <span>{c.telefone}</span>
                  </button>
                ) : (
                  <span style={{ color: 'var(--text-muted)' }}>—</span>
                )}
              </td>

              {/* E-mail */}
              <td>
                <span style={{ fontSize: '13px', color: c.email ? 'var(--text-secondary)' : 'var(--text-muted)' }}>
                  {c.email || '—'}
                </span>
              </td>

              {/* Cidade / Bairro */}
              <td>
                <span style={{ fontSize: '13px' }}>
                  {c.cidade ? (c.bairro ? `${c.cidade} (${c.bairro})` : c.cidade) : '—'}
                </span>
              </td>

              {/* Plano / Valor */}
              <td>
                {c.plano || c.valor_plano ? (
                  <div>
                    <div style={{ fontWeight: 500, color: 'var(--text-primary)' }}>{c.plano || '—'}</div>
                    {c.valor_plano && (
                      <div style={{ fontSize: '12px', color: 'var(--accent-green)' }}>
                        R$ {c.valor_plano}
                      </div>
                    )}
                  </div>
                ) : (
                  <span style={{ color: 'var(--text-muted)' }}>—</span>
                )}
              </td>

              {/* Forma de Pagamento */}
              <td>
                <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                  {c.forma_pagamento || '—'}
                </span>
              </td>

              {/* Vencimento */}
              <td>
                <span style={{ fontSize: '13px', fontWeight: 500 }}>
                  {c.dia_vencimento ? `Dia ${c.dia_vencimento}` : '—'}
                </span>
              </td>

              {/* Ações */}
              <td style={{ textAlign: 'right' }}>
                <div style={{ display: 'inline-flex', gap: '6px' }}>
                  <button
                    className="btn-icon"
                    onClick={() => onViewDetails(c)}
                    title="Ver Ficha Completa"
                  >
                    <Eye size={16} />
                  </button>
                  <button
                    className="btn-icon"
                    onClick={() => onEdit(c)}
                    title="Editar Cliente"
                  >
                    <Edit2 size={16} />
                  </button>
                  <button
                    className="btn-icon"
                    onClick={() => {
                      if (window.confirm(`Deseja excluir ${c.nome_completo}?`)) {
                        onDelete(c);
                      }
                    }}
                    title="Excluir Cliente"
                    style={{ color: 'var(--danger-red)' }}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
