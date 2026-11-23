import React, { useState } from 'react';
import { supabase, isSupabaseConfigured } from '../lib/supabase';
import { Lock, Mail, Cloud, X, AlertCircle } from 'lucide-react';

interface LoginModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (email: string) => void;
}

export const LoginModal: React.FC<LoginModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Preencha todos os campos');
      return;
    }

    setLoading(true);
    setError(null);

    if (!isSupabaseConfigured()) {
      // Se o Supabase ainda não foi configurado, permite usar sessão local
      onSuccess(email);
      onClose();
      setLoading(false);
      return;
    }

    try {
      if (isRegister) {
        const { data, error: err } = await supabase.auth.signUp({
          email,
          password,
        });
        if (err) throw err;
        if (data.user?.email) {
          onSuccess(data.user.email);
          onClose();
        }
      } else {
        const { data, error: err } = await supabase.auth.signInWithPassword({
          email,
          password,
        });
        if (err) throw err;
        if (data.user?.email) {
          onSuccess(data.user.email);
          onClose();
        }
      }
    } catch (err: any) {
      setError(err.message || 'Falha na autenticação');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-card" style={{ maxWidth: '440px' }}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Cloud size={24} color="var(--accent-green)" />
            <h3 className="font-heading" style={{ fontSize: '18px' }}>
              {isRegister ? 'Criar Nova Conta' : 'Acessar Cadastro OCR'}
            </h3>
          </div>
          <button className="btn-icon" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {/* Alternador Entrar / Criar Conta */}
            <div
              style={{
                display: 'flex',
                background: 'var(--bg-dark)',
                padding: '4px',
                borderRadius: 'var(--radius-md)',
                marginBottom: '20px',
              }}
            >
              <button
                type="button"
                className={`btn ${!isRegister ? 'btn-primary' : 'btn-secondary'}`}
                style={{ flex: 1, padding: '8px' }}
                onClick={() => {
                  setIsRegister(false);
                  setError(null);
                }}
              >
                Entrar
              </button>
              <button
                type="button"
                className={`btn ${isRegister ? 'btn-primary' : 'btn-secondary'}`}
                style={{ flex: 1, padding: '8px' }}
                onClick={() => {
                  setIsRegister(true);
                  setError(null);
                }}
              >
                Criar Conta
              </button>
            </div>

            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">E-mail</label>
              <div style={{ position: 'relative' }}>
                <Mail size={16} style={{ position: 'absolute', left: '12px', top: '13px', color: 'var(--text-secondary)' }} />
                <input
                  type="email"
                  className="form-input"
                  style={{ paddingLeft: '38px', width: '100%' }}
                  placeholder="seu.email@exemplo.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '18px' }}>
              <label className="form-label">Senha</label>
              <div style={{ position: 'relative' }}>
                <Lock size={16} style={{ position: 'absolute', left: '12px', top: '13px', color: 'var(--text-secondary)' }} />
                <input
                  type="password"
                  className="form-input"
                  style={{ paddingLeft: '38px', width: '100%' }}
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
            </div>

            {error && (
              <div
                style={{
                  background: 'var(--danger-red-bg)',
                  border: '1px solid var(--danger-red)',
                  color: 'var(--danger-red)',
                  padding: '10px 14px',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '13px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  marginBottom: '16px',
                }}
              >
                <AlertCircle size={16} />
                <span>{error}</span>
              </div>
            )}

            {!isSupabaseConfigured() && (
              <div
                style={{
                  background: 'rgba(245, 166, 35, 0.1)',
                  border: '1px solid rgba(245, 166, 35, 0.3)',
                  color: 'var(--status-pending-text)',
                  padding: '10px',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '12px',
                  marginBottom: '16px',
                }}
              >
                Modo de visualização local ativo. O login salvará sua sessão no navegador.
              </div>
            )}

            <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
              {loading ? 'Aguarde...' : isRegister ? 'Cadastrar e Conectar' : 'Entrar no Sistema'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
