import { createClient } from '@supabase/supabase-js';

// Variáveis de ambiente ou fallback de demonstração
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || 'https://SEU_PROJETO.supabase.co';
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...';

export const isSupabaseConfigured = () => {
  return (
    supabaseUrl.startsWith('https://') &&
    !supabaseUrl.includes('SEU_PROJETO') &&
    supabaseAnonKey.length > 20
  );
};

export const supabase = createClient(
  isSupabaseConfigured() ? supabaseUrl : 'https://placeholder.supabase.co',
  isSupabaseConfigured() ? supabaseAnonKey : 'placeholder-key',
  {
    auth: {
      persistSession: true,
      autoRefreshToken: true,
    },
    realtime: {
      params: {
        eventsPerSecond: 10,
      },
    },
  }
);
