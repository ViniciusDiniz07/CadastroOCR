import { createWorker } from 'tesseract.js';
import { ExtractedOcrData } from '../types/client';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8787';
const APP_SHARED_SECRET = import.meta.env.VITE_APP_SHARED_SECRET || '';

/**
 * Realiza OCR na imagem utilizando Tesseract.js diretamente no navegador (offline-capable).
 */
export const extractTextFromImage = async (
  imageFile: File,
  onProgress?: (progress: number, status: string) => void
): Promise<string> => {
  const worker = await createWorker('por');
  
  if (onProgress) {
    onProgress(0.3, 'Reconhecendo caracteres na imagem...');
  }

  const ret = await worker.recognize(imageFile);
  await worker.terminate();

  return ret.data.text;
};

/**
 * Envia o texto extraído pelo OCR para o Worker da Gemini (IA) para estruturar em JSON.
 * Se o Worker não estiver acessível, utiliza regras de extração local (Regex).
 */
export const interpretOcrText = async (rawText: string): Promise<ExtractedOcrData> => {
  if (BACKEND_URL.startsWith('http') && APP_SHARED_SECRET) {
    try {
      const res = await fetch(BACKEND_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-app-secret': APP_SHARED_SECRET,
        },
        body: JSON.stringify({ texto: rawText }),
      });

      if (res.ok) {
        const json = await res.json();
        const data = json.dados || json;
        return {
          nomeCompleto: data.nomeCompleto || '',
          cpf: data.cpf || '',
          dataNascimento: data.dataNascimento || '',
          nomeDaMae: data.nomeDaMae || '',
          nomeDoPai: data.nomeDoPai || '',
          rg: data.rg || '',
          dataExpedicaoRg: data.dataExpedicaoRg || '',
          email: data.email || '',
          telefone: data.telefone || '',
          rua: data.rua || '',
          numero: data.numero || '',
          cep: data.cep || '',
          bairro: data.bairro || '',
          cidade: data.cidade || '',
          textoBrutoReconhecido: rawText,
        };
      }
    } catch (e) {
      console.warn('Falha na IA remota, utilizando parser de regras local:', e);
    }
  }

  // Fallback para parser de regras local (Regex)
  return parseLocallyWithRegex(rawText);
};

const parseLocallyWithRegex = (text: string): ExtractedOcrData => {
  const clean = text.replace(/\r/g, '');

  // CPF
  const cpfMatch = clean.match(/\b\d{3}\.?\d{3}\.?\d{3}-?\d{2}\b/);
  const cpf = cpfMatch ? cpfMatch[0] : '';

  // E-mail
  const emailMatch = clean.match(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/);
  const email = emailMatch ? emailMatch[0] : '';

  // Telefone / Celular
  const phoneMatch = clean.match(/(?:\(?\d{2}\)?\s*)?(?:9\s*)?\d{4,5}[-\s]?\d{4}/);
  const telefone = phoneMatch ? phoneMatch[0].trim() : '';

  // CEP
  const cepMatch = clean.match(/\b\d{5}-?\d{3}\b/);
  const cep = cepMatch ? cepMatch[0] : '';

  // Data de nascimento
  const dateMatch = clean.match(/\b(\d{2}\/\d{2}\/\d{4})\b/);
  const dataNascimento = dateMatch ? dateMatch[1] : '';

  // Nome (heurística simples sobre primeiras linhas)
  const lines = clean.split('\n').map((l) => l.trim()).filter((l) => l.length > 3);
  let nomeCompleto = '';
  for (const line of lines) {
    if (/^[A-ZÁÉÍÓÚÂÊÔÃÕÇ\s]{4,}$/i.test(line) && !line.includes('DOCUMENTO') && !line.includes('BRASIL') && !line.includes('REGISTRO')) {
      nomeCompleto = line;
      break;
    }
  }

  return {
    nomeCompleto,
    cpf,
    dataNascimento,
    nomeDaMae: '',
    nomeDoPai: '',
    rg: '',
    dataExpedicaoRg: '',
    email,
    telefone,
    rua: '',
    numero: '',
    cep,
    bairro: '',
    cidade: '',
    textoBrutoReconhecido: text,
  };
};
