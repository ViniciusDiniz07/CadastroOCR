import React, { useState, useEffect, useMemo } from 'react';
import { Cliente, ExtractedOcrData } from './types/client';
import { supabase, isSupabaseConfigured } from './lib/supabase';
import { Navbar } from './components/Navbar';
import { ClientTable } from './components/ClientTable';
import { ClientDetailModal } from './components/ClientDetailModal';
import { ClientFormModal } from './components/ClientFormModal';
import { OcrImportModal } from './components/OcrImportModal';
import { LoginModal } from './components/LoginModal';
import { Search, Plus, Sparkles, Users } from 'lucide-react';

const LOCAL_STORAGE_KEY = 'cadastro_ocr_web_clientes';

export const App: React.FC = () => {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [userEmail, setUserEmail] = useState<string | null>(null);
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [isSyncing, setIsSyncing] = useState(false);

  // Modais
  const [selectedClientForDetail, setSelectedClientForDetail] = useState<Cliente | null>(null);
  const [selectedClientForEdit, setSelectedClientForEdit] = useState<Cliente | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isOcrModalOpen, setIsOcrModalOpen] = useState(false);
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [prefilledOcrData, setPrefilledOcrData] = useState<ExtractedOcrData | null>(null);

  // 1. Carrega sessão de autenticação inicial
  useEffect(() => {
    const checkAuth = async () => {
      if (isSupabaseConfigured()) {
        const { data } = await supabase.auth.getSession();
        if (data.session?.user?.email) {
          setUserEmail(data.session.user.email);
        }
      } else {
        const savedEmail = localStorage.getItem('cadastro_ocr_saved_user');
        if (savedEmail) setUserEmail(savedEmail);
      }
    };
    checkAuth();

    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // 2. Busca inicial e Sincronização Realtime com Supabase
  const loadClientes = async () => {
    setIsSyncing(true);

    if (isSupabaseConfigured()) {
      try {
        const { data, error } = await supabase
          .from('clientes')
          .select('*')
          .eq('is_deleted', false)
          .order('nome_completo', { ascending: true });

        if (!error && data) {
          setClientes(data as Cliente[]);
          localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(data));
        }
      } catch (err) {
        console.error('Erro ao buscar dados do Supabase:', err);
      }
    } else {
      const localData = localStorage.getItem(LOCAL_STORAGE_KEY);
      if (localData) {
        try {
          setClientes(JSON.parse(localData));
        } catch {
          setClientes([]);
        }
      }
    }

    setIsSyncing(false);
  };

  useEffect(() => {
    loadClientes();

    // Ativa inscrição Realtime (WebSockets) para escutar alterações de outros dispositivos
    if (isSupabaseConfigured()) {
      const channel = supabase
        .channel('public:clientes')
        .on(
          'postgres_changes',
          { event: '*', schema: 'public', table: 'clientes' },
          (payload) => {
            console.log('Alteração recebida em tempo real da nuvem:', payload);
            if (payload.eventType === 'INSERT') {
              const novo = payload.new as Cliente;
              if (!novo.is_deleted) {
                setClientes((prev) => {
                  if (prev.some((c) => c.id === novo.id)) return prev;
                  return [...prev, novo].sort((a, b) => a.nome_completo.localeCompare(b.nome_completo));
                });
              }
            } else if (payload.eventType === 'UPDATE') {
              const atualizado = payload.new as Cliente;
              if (atualizado.is_deleted) {
                setClientes((prev) => prev.filter((c) => c.id !== atualizado.id));
              } else {
                setClientes((prev) =>
                  prev.map((c) => (c.id === atualizado.id ? atualizado : c))
                );
              }
            } else if (payload.eventType === 'DELETE') {
              const idDeletado = payload.old.id;
              setClientes((prev) => prev.filter((c) => c.id !== idDeletado));
            }
          }
        )
        .subscribe();

      return () => {
        supabase.removeChannel(channel);
      };
    }
  }, []);

  // 3. Salvar Cliente (Criar ou Atualizar)
  const handleSaveClient = async (formData: Partial<Cliente>) => {
    setIsSyncing(true);
    const clienteId = formData.id || crypto.randomUUID();
    const timestamp = Date.now();

    const clienteCompleto: Cliente = {
      ...(formData as Cliente),
      id: clienteId,
      is_deleted: false,
      criado_em: formData.criado_em || timestamp,
      atualizado_em: timestamp,
    };

    if (isSupabaseConfigured()) {
      const { error } = await supabase.from('clientes').upsert({
        ...clienteCompleto,
        updated_at: new Date().toISOString(),
      });
      if (error) {
        setIsSyncing(false);
        throw new Error(error.message);
      }
    }

    // Atualiza estado local
    setClientes((prev) => {
      const exists = prev.some((c) => c.id === clienteId);
      const updated = exists
        ? prev.map((c) => (c.id === clienteId ? clienteCompleto : c))
        : [...prev, clienteCompleto];
      const sorted = updated.sort((a, b) => a.nome_completo.localeCompare(b.nome_completo));
      localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(sorted));
      return sorted;
    });

    setIsSyncing(false);
  };

  // 4. Excluir Cliente
  const handleDeleteClient = async (cliente: Cliente) => {
    setIsSyncing(true);

    if (isSupabaseConfigured()) {
      await supabase
        .from('clientes')
        .update({ is_deleted: true, atualizado_em: Date.now() })
        .eq('id', cliente.id);
    }

    setClientes((prev) => {
      const filtered = prev.filter((c) => c.id !== cliente.id);
      localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(filtered));
      return filtered;
    });

    setIsSyncing(false);
  };

  // 5. Filtro de Busca Universal
  const filteredClients = useMemo(() => {
    if (!searchTerm.trim()) return clientes;
    const term = searchTerm.toLowerCase().trim();

    return clientes.filter((c) => {
      return (
        c.nome_completo.toLowerCase().includes(term) ||
        c.cpf.replace(/\D/g, '').includes(term.replace(/\D/g, '')) ||
        c.email.toLowerCase().includes(term) ||
        c.telefone.includes(term) ||
        c.cidade.toLowerCase().includes(term) ||
        c.plano.toLowerCase().includes(term)
      );
    });
  }, [clientes, searchTerm]);

  return (
    <div className="app-container">
      <Navbar
        userEmail={userEmail}
        isOnline={isOnline}
        isSyncing={isSyncing}
        onManualSync={loadClientes}
        onOpenLogin={() => setIsLoginModalOpen(true)}
        onLogout={async () => {
          if (isSupabaseConfigured()) {
            await supabase.auth.signOut();
          }
          localStorage.removeItem('cadastro_ocr_saved_user');
          setUserEmail(null);
        }}
      />

      <main className="main-content">
        {/* Cabeçalho do Painel */}
        <div className="dashboard-header">
          <div>
            <h1 className="font-heading" style={{ fontSize: '24px', fontWeight: 700, marginBottom: '4px' }}>
              Painel de Clientes
            </h1>
            <div style={{ fontSize: '14px', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Users size={15} />
              <span>
                {clientes.length} {clientes.length === 1 ? 'cliente cadastrado' : 'clientes cadastrados'} no sistema
              </span>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              className="btn btn-secondary"
              onClick={() => setIsOcrModalOpen(true)}
            >
              <Sparkles size={16} color="var(--accent-green)" />
              Importar com OCR / IA
            </button>

            <button
              className="btn btn-primary"
              onClick={() => {
                setSelectedClientForEdit(null);
                setPrefilledOcrData(null);
                setIsFormOpen(true);
              }}
            >
              <Plus size={16} />
              Novo Cliente
            </button>
          </div>
        </div>

        {/* Barra de Pesquisa */}
        <div style={{ marginBottom: '20px' }}>
          <div className="search-container">
            <Search className="search-icon" size={18} />
            <input
              type="text"
              className="search-input"
              placeholder="Pesquisar por nome, CPF, e-mail, telefone, cidade ou plano..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        {/* Tabela de Dados */}
        <ClientTable
          clientes={filteredClients}
          onViewDetails={(c) => setSelectedClientForDetail(c)}
          onEdit={(c) => {
            setSelectedClientForEdit(c);
            setPrefilledOcrData(null);
            setIsFormOpen(true);
          }}
          onDelete={handleDeleteClient}
        />
      </main>

      {/* Modais da Aplicação */}
      <ClientDetailModal
        cliente={selectedClientForDetail}
        isOpen={Boolean(selectedClientForDetail)}
        onClose={() => setSelectedClientForDetail(null)}
        onEdit={(c) => {
          setSelectedClientForEdit(c);
          setPrefilledOcrData(null);
          setIsFormOpen(true);
        }}
        onDelete={handleDeleteClient}
      />

      <ClientFormModal
        clienteInicial={selectedClientForEdit}
        dadosOcrPrePreenchidos={prefilledOcrData}
        isOpen={isFormOpen}
        onClose={() => {
          setIsFormOpen(false);
          setSelectedClientForEdit(null);
          setPrefilledOcrData(null);
        }}
        onSave={handleSaveClient}
      />

      <OcrImportModal
        isOpen={isOcrModalOpen}
        onClose={() => setIsOcrModalOpen(false)}
        onDataExtracted={(data) => {
          setPrefilledOcrData(data);
          setSelectedClientForEdit(null);
          setIsFormOpen(true);
        }}
      />

      <LoginModal
        isOpen={isLoginModalOpen}
        onClose={() => setIsLoginModalOpen(false)}
        onSuccess={(email) => {
          setUserEmail(email);
          localStorage.setItem('cadastro_ocr_saved_user', email);
          loadClientes();
        }}
      />
    </div>
  );
};
