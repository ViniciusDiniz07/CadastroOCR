import React, { useState, useRef } from 'react';
import { ExtractedOcrData } from '../types/client';
import { extractTextFromImage, interpretOcrText } from '../lib/aiOcrService';
import { X, Upload, Sparkles, FileText, CheckCircle2, AlertCircle, ChevronDown, ChevronUp, Image as ImageIcon } from 'lucide-react';

interface OcrImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onDataExtracted: (data: ExtractedOcrData) => void;
}

export const OcrImportModal: React.FC<OcrImportModalProps> = ({ isOpen, onClose, onDataExtracted }) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [processing, setProcessing] = useState(false);
  const [statusText, setStatusText] = useState('');
  const [extractedData, setExtractedData] = useState<ExtractedOcrData | null>(null);
  const [showRawText, setShowRawText] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleFileSelect = (file: File) => {
    if (!file.type.startsWith('image/')) {
      setError('Por favor, selecione um arquivo de imagem válido (JPG, PNG).');
      return;
    }
    setSelectedFile(file);
    setImagePreview(URL.createObjectURL(file));
    setError(null);
    setExtractedData(null);
  };

  const handleProcessOcr = async () => {
    if (!selectedFile) return;

    setProcessing(true);
    setError(null);
    setStatusText('Processando OCR na imagem...');

    try {
      // 1. Extração do texto bruto via OCR
      const rawText = await extractTextFromImage(selectedFile, (_p, status) => {
        setStatusText(status);
      });

      if (!rawText || rawText.trim().length === 0) {
        throw new Error('Nenhum texto pôde ser reconhecido na imagem. Tente uma foto mais nítida e iluminada.');
      }

      // 2. Interpretação dos dados via IA / Regras
      setStatusText('Estruturando dados com Inteligência Artificial...');
      const data = await interpretOcrText(rawText);
      setExtractedData(data);
    } catch (err: any) {
      setError(err.message || 'Erro ao processar imagem.');
    } finally {
      setProcessing(false);
      setStatusText('');
    }
  };

  const handleApply = () => {
    if (extractedData) {
      onDataExtracted(extractedData);
      onClose();
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-card" style={{ maxWidth: '850px' }}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Sparkles size={22} color="var(--accent-green)" />
            <h2 className="font-heading" style={{ fontSize: '20px' }}>
              Importar Cliente via OCR e IA
            </h2>
          </div>
          <button className="btn-icon" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        <div className="modal-body">
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
            Selecione ou arraste a foto de um documento (RG, CIN, CNH) ou o print de uma conversa (WhatsApp) para
            extrair e preencher os dados cadastrais automaticamente.
          </p>

          {/* Área de Seleção / Dropzone */}
          {!imagePreview ? (
            <div
              className="dropzone"
              onClick={() => fileInputRef.current?.click()}
              onDragOver={(e) => e.preventDefault()}
              onDrop={(e) => {
                e.preventDefault();
                if (e.dataTransfer.files?.[0]) {
                  handleFileSelect(e.dataTransfer.files[0]);
                }
              }}
            >
              <input
                type="file"
                ref={fileInputRef}
                style={{ display: 'none' }}
                accept="image/*"
                onChange={(e) => {
                  if (e.target.files?.[0]) {
                    handleFileSelect(e.target.files[0]);
                  }
                }}
              />
              <Upload size={40} style={{ color: 'var(--accent-green)', margin: '0 auto 12px' }} />
              <div style={{ fontWeight: 600, fontSize: '16px', marginBottom: '6px' }}>
                Clique para selecionar ou arraste a imagem aqui
              </div>
              <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                Formatos suportados: PNG, JPG, JPEG, WebP
              </div>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap', marginBottom: '16px' }}>
              <div style={{ flex: '1 1 280px', position: 'relative' }}>
                <img
                  src={imagePreview}
                  alt="Pré-visualização"
                  style={{
                    width: '100%',
                    maxHeight: '260px',
                    objectFit: 'contain',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-subtle)',
                    backgroundColor: '#000',
                  }}
                />
                <button
                  className="btn btn-secondary"
                  style={{ marginTop: '8px', width: '100%', fontSize: '13px' }}
                  onClick={() => {
                    setImagePreview(null);
                    setSelectedFile(null);
                    setExtractedData(null);
                  }}
                  disabled={processing}
                >
                  <ImageIcon size={14} /> Trocar Imagem
                </button>
              </div>

              <div style={{ flex: '1 1 340px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                {!extractedData && !processing && (
                  <div>
                    <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '8px' }}>
                      Imagem pronta para processamento
                    </h3>
                    <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
                      O sistema executará o OCR no navegador e utilizará a IA para estruturar os dados do cliente.
                    </p>
                    <button className="btn btn-primary" onClick={handleProcessOcr} style={{ width: '100%' }}>
                      <Sparkles size={16} /> Iniciar Reconhecimento e IA
                    </button>
                  </div>
                )}

                {processing && (
                  <div style={{ textAlign: 'center', padding: '24px 0' }}>
                    <div className="pulse-dot" style={{ width: '16px', height: '16px', margin: '0 auto 14px', backgroundColor: 'var(--accent-green)' }}></div>
                    <div style={{ fontWeight: 600, fontSize: '15px', color: 'var(--accent-green)', marginBottom: '6px' }}>
                      {statusText || 'Processando...'}
                    </div>
                    <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                      Aguarde enquanto os caracteres e dados são analisados.
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {error && (
            <div
              style={{
                background: 'var(--danger-red-bg)',
                border: '1px solid var(--danger-red)',
                color: 'var(--danger-red)',
                padding: '12px',
                borderRadius: 'var(--radius-md)',
                fontSize: '13px',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                marginTop: '16px',
              }}
            >
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          {/* Exibição dos Dados Extraídos */}
          {extractedData && (
            <div style={{ marginTop: '20px', borderTop: '1px solid var(--border-subtle)', paddingTop: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
                <CheckCircle2 size={18} color="var(--accent-green)" />
                <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Dados Extraídos com Sucesso
                </h3>
              </div>

              <div className="form-grid" style={{ marginBottom: '14px' }}>
                <div className="detail-field">
                  <div>
                    <div className="detail-label">Nome Completo</div>
                    <div className="detail-value">{extractedData.nomeCompleto || '—'}</div>
                  </div>
                </div>
                <div className="detail-field">
                  <div>
                    <div className="detail-label">CPF</div>
                    <div className="detail-value">{extractedData.cpf || '—'}</div>
                  </div>
                </div>
                <div className="detail-field">
                  <div>
                    <div className="detail-label">Data de Nascimento</div>
                    <div className="detail-value">{extractedData.dataNascimento || '—'}</div>
                  </div>
                </div>
                <div className="detail-field">
                  <div>
                    <div className="detail-label">RG</div>
                    <div className="detail-value">{extractedData.rg || '—'}</div>
                  </div>
                </div>
                <div className="detail-field">
                  <div>
                    <div className="detail-label">Telefone</div>
                    <div className="detail-value">{extractedData.telefone || '—'}</div>
                  </div>
                </div>
                <div className="detail-field">
                  <div>
                    <div className="detail-label">E-mail</div>
                    <div className="detail-value">{extractedData.email || '—'}</div>
                  </div>
                </div>
                <div className="detail-field">
                  <div>
                    <div className="detail-label">Cidade / Bairro</div>
                    <div className="detail-value">
                      {[extractedData.cidade, extractedData.bairro].filter(Boolean).join(' - ') || '—'}
                    </div>
                  </div>
                </div>
                <div className="detail-field">
                  <div>
                    <div className="detail-label">Endereço</div>
                    <div className="detail-value">
                      {[extractedData.rua, extractedData.numero].filter(Boolean).join(', ') || '—'}
                    </div>
                  </div>
                </div>
              </div>

              {/* Accordion de Texto Bruto do OCR */}
              {extractedData.textoBrutoReconhecido && (
                <div>
                  <button
                    type="button"
                    style={{
                      background: 'none',
                      border: 'none',
                      color: 'var(--text-secondary)',
                      fontSize: '13px',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      padding: '4px 0',
                    }}
                    onClick={() => setShowRawText(!showRawText)}
                  >
                    <FileText size={14} />
                    <span>Ver texto bruto reconhecido pelo OCR</span>
                    {showRawText ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                  </button>

                  {showRawText && (
                    <pre
                      style={{
                        background: 'var(--bg-dark)',
                        border: '1px solid var(--border-subtle)',
                        borderRadius: 'var(--radius-md)',
                        padding: '12px',
                        fontSize: '12px',
                        color: 'var(--text-secondary)',
                        maxHeight: '140px',
                        overflowY: 'auto',
                        whiteSpace: 'pre-wrap',
                        marginTop: '8px',
                      }}
                    >
                      {extractedData.textoBrutoReconhecido}
                    </pre>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose} disabled={processing}>
            Cancelar
          </button>
          {extractedData && (
            <button className="btn btn-primary" onClick={handleApply}>
              <CheckCircle2 size={16} /> Preencher Formulário de Cadastro
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
