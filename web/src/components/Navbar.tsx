import React from 'react';
import { Cloud, RefreshCw, LogOut, ScanText } from 'lucide-react';

interface NavbarProps {
  userEmail: string | null;
  isOnline: boolean;
  isSyncing: boolean;
  onManualSync: () => void;
  onOpenLogin: () => void;
  onLogout: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  userEmail,
  isOnline,
  isSyncing,
  onManualSync,
  onOpenLogin,
  onLogout,
}) => {
  return (
    <header className="navbar">
      <div className="navbar-inner">
        <div className="logo-group">
          <div className="logo-icon">
            <ScanText size={22} />
          </div>
          <div>
            <div className="logo-title">Cadastro OCR</div>
            <div className="logo-subtitle">Sincronização Multi-Dispositivo (PC + Android)</div>
          </div>
        </div>

        <div className="nav-actions">
          {/* Status da Conexão / Sincronização */}
          <div className={`status-pill ${isOnline ? 'online' : 'offline'}`}>
            <span className="pulse-dot"></span>
            {isSyncing ? 'Sincronizando...' : isOnline ? 'Nuvem Conectada' : 'Modo Offline'}
          </div>

          {/* Botão de Sync Manual */}
          <button
            className="btn btn-secondary btn-icon"
            onClick={onManualSync}
            title="Sincronizar agora"
            disabled={isSyncing}
          >
            <RefreshCw size={16} className={isSyncing ? 'animate-spin' : ''} />
          </button>

          {/* Identificação do Usuário */}
          {userEmail ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div
                style={{
                  background: 'var(--surface-elevated)',
                  border: '1px solid var(--border-subtle)',
                  padding: '6px 14px',
                  borderRadius: 'var(--radius-full)',
                  fontSize: '13px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                }}
              >
                <Cloud size={14} color="var(--accent-green)" />
                <span>{userEmail}</span>
              </div>
              <button className="btn btn-danger" onClick={onLogout} style={{ padding: '6px 12px', fontSize: '13px' }}>
                <LogOut size={14} />
                Sair
              </button>
            </div>
          ) : (
            <button className="btn btn-primary" onClick={onOpenLogin}>
              Fazer Login
            </button>
          )}
        </div>
      </div>
    </header>
  );
};
