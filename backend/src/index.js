/**
 * Backend do Cadastro OCR — Cloudflare Worker.
 * A única peça que conhece a API key da Gemini. O app Android nunca fala
 * direto com a Gemini: ele chama este Worker, que chama a Gemini usando uma
 * key guardada em segredo (nunca no APK).
 *
 * Entrada  (POST, JSON): { "texto": "<texto bruto reconhecido pelo OCR>" }
 * Saída    (JSON):       { "dados": { nomeCompleto, cpf, ... } }
 *
 * Funciona tanto para texto de documento (RG/CIN/CNH) quanto de conversa
 * (WhatsApp) — é o próprio prompt que decide o que existe no texto.
 */

// Fácil de trocar quando o gemini-3.5-flash-lite for desligado — troque para o modelo mais atual recomendado.
const MODELO_GEMINI = "gemini-3.5-flash-lite";

const TAMANHO_MAXIMO_TEXTO = 8000; // limite defensivo de custo/abuso

const SCHEMA_RESPOSTA = {
  type: "object",
  properties: {
    nomeCompleto: { type: "string" },
    cpf: { type: "string" },
    dataNascimento: { type: "string" },
    nomeDaMae: { 
      type: "string", 
      description: "Nome da mãe do titular do documento. Geralmente é o primeiro nome listado sob a seção 'FILIAÇÃO' ou 'FILIATION'. Se não houver certeza ou não existir, retorne string vazia \"\"." 
    },
    nomeDoPai: { 
      type: "string", 
      description: "Nome do pai do titular do documento. Geralmente é o segundo nome listado sob a seção 'FILIAÇÃO' ou 'FILIATION'. Se não houver, retorne string vazia \"\"." 
    },
    rg: { 
      type: "string", 
      description: "Número do Registro Geral (RG) ou Identidade. Pode conter pontos e traços. Geralmente próximo a 'REGISTRO GERAL', 'RG', 'IDENTIDADE N°' ou 'N°'. Não confunda com CPF. Se não encontrar, retorne string vazia \"\"." 
    },
    dataExpedicaoRg: { type: "string" },
    email: { type: "string" },
    telefone: { type: "string" },
    cep: { type: "string" },
    rua: { type: "string" },
    numero: { type: "string" },
    bairro: { type: "string" },
    cidade: { type: "string" },
  },
  required: [
    "nomeCompleto", "cpf", "dataNascimento", "nomeDaMae", "nomeDoPai",
    "rg", "dataExpedicaoRg", "email", "telefone", "cep", "rua", "numero",
    "bairro", "cidade",
  ],
};

const PROMPT_SISTEMA = `Você é um extrator de dados cadastrais brasileiro de alta precisão.

Você vai receber um texto bruto reconhecido por OCR. Esse texto pode ter vindo
de duas fontes bem diferentes:
1. Um documento de identificação (RG, CIN, CNH ou similar) — nesse caso pode
   conter: nome completo, CPF, data de nascimento, RG, data de expedição/emissão,
   nome da mãe (sob 'FILIAÇÃO' ou 'FILIATION'), nome do pai (sob 'FILIAÇÃO' ou 'FILIATION').
2. Um print de conversa (ex.: WhatsApp) — nesse caso pode conter, escrito em
   linguagem natural e informal: e-mail, telefone, CEP, rua, número,
   complemento, bairro, cidade, e ocasionalmente CPF, nome ou data de nascimento.

Sua tarefa: ler o texto e devolver APENAS os campos que você tem certeza
razoável de ter identificado corretamente, no formato JSON pedido.

REGRAS OBRIGATÓRIAS:
- NUNCA invente, complete ou assuma um dado que não está claramente no texto.
- Se não tiver certeza sobre um campo, devolva ele como string vazia "".
- FILIAÇÃO: Em documentos brasileiros, a filiação lista os genitores. Quase sempre o primeiro nome é o da MÃE, e o segundo é o do PAI. Extraia os nomes corretamente baseando-se na seção de filiação, mesmo que as palavras "Mãe" ou "Pai" não estejam escritas explicitamente.
- RG: O número do RG (Registro Geral) costuma aparecer próximo a "REGISTRO GERAL", "RG", "IDENTIDADE N°", "N°" ou "DOCUMENTO DE IDENTIDADE". Descarte o CPF (que tem 11 dígitos) e extraia apenas o número correspondente ao RG.
- O texto pode vir com blocos fora de ordem (comum em documentos com layout
  de cartão, como CNH) — use o contexto para interpretar mesmo assim.
- CPF: formate como 000.000.000-00 quando encontrar.
- Datas: formate como DD/MM/AAAA quando encontrar.
- Nomes: use capitalização normal (ex.: "João da Silva"), mesmo que o texto
  original esteja todo em maiúsculas.
- Não inclua explicações, apenas os dados extraídos.`;

export default {
  async fetch(request, env) {
    console.log("--- NOVA REQUISIÇÃO RECEBIDA ---");
    console.log("Headers:", JSON.stringify(Object.fromEntries(request.headers.entries())));

    if (request.method !== "POST") {
      console.log("Erro: Método não permitido");
      return jsonResponse({ erro: "Método não permitido" }, 405);
    }

    // Protege contra uso não autorizado do Worker
    const segredoRecebido = request.headers.get("x-app-secret");
    if (!segredoRecebido || segredoRecebido !== env.APP_SHARED_SECRET) {
      console.log("Erro: Não autorizado. Segredo incorreto.");
      return jsonResponse({ erro: "Não autorizado" }, 401);
    }

    let corpo;
    try {
      corpo = await request.json();
    } catch {
      console.log("Erro: JSON inválido");
      return jsonResponse({ erro: "JSON inválido" }, 400);
    }

    const texto = corpo?.texto;
    if (typeof texto !== "string" || texto.trim().length === 0) {
      console.log("Erro: Campo 'texto' vazio ou inválido");
      return jsonResponse({ erro: "Campo 'texto' é obrigatório" }, 400);
    }
    if (texto.length > TAMANHO_MAXIMO_TEXTO) {
      console.log("Erro: Texto muito longo");
      return jsonResponse({ erro: "Texto muito longo" }, 400);
    }

    console.log("Texto bruto do OCR enviado para a Gemini:\n", texto);

    try {
      const urlGemini = `https://generativelanguage.googleapis.com/v1beta/models/${MODELO_GEMINI}:generateContent`;
      console.log("Chamando a Gemini API no endpoint:", urlGemini);

      const respostaGemini = await fetch(
        urlGemini,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": env.GEMINI_API_KEY,
          },
          body: JSON.stringify({
            systemInstruction: { parts: [{ text: PROMPT_SISTEMA }] },
            contents: [{ role: "user", parts: [{ text: texto }] }],
            generationConfig: {
              temperature: 0,
              responseMimeType: "application/json",
              responseSchema: SCHEMA_RESPOSTA,
            },
          }),
        }
      );

      if (!respostaGemini.ok) {
        const erroCorpo = await respostaGemini.text();
        console.error("Erro da API Gemini:", respostaGemini.status, erroCorpo);
        return jsonResponse({ erro: "Falha ao consultar o modelo de IA" }, 502);
      }

      const json = await respostaGemini.json();
      console.log("Resposta JSON bruta da Gemini:", JSON.stringify(json));

      const textoResposta = json?.candidates?.[0]?.content?.parts?.[0]?.text;
      if (!textoResposta) {
        console.error("Erro: Resposta da IA vazia");
        return jsonResponse({ erro: "Resposta da IA vazia" }, 502);
      }

      console.log("JSON de dados extraídos da Gemini:", textoResposta);
      const dados = JSON.parse(textoResposta);
      return jsonResponse({ dados }, 200);
    } catch (erro) {
      console.error("Erro ao interpretar texto:", erro);
      return jsonResponse({ erro: "Erro interno" }, 500);
    }
  },
};

function jsonResponse(objeto, status) {
  return new Response(JSON.stringify(objeto), {
    status,
    headers: { 
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*"
    },
  });
}
