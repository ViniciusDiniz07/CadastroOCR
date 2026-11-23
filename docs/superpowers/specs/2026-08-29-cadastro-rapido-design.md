# Cadastro rápido, máscaras e consulta de CEP

## Objetivo

Reduzir a digitação e os toques no cadastro de clientes sem alterar o
funcionamento atual de OCR, IA, edição de clientes ou armazenamento local.
O formulário continua completamente editável; automações apenas auxiliam o
preenchimento.

## Decisões de interação

O formulário passa a manter uma ordem explícita de foco:

`Nome → CPF → Nascimento → RG → Expedição → Mãe → Pai → E-mail → Telefone → CEP → Rua → Número → Complemento → Bairro → Cidade → UF → Plano → Valor → Pagamento → Vencimento`.

- Nome, RG e e-mail avançam somente quando o usuário aciona `Enter`/`OK`/`Próximo` no teclado. A validação do e-mail continua exibindo erro quando houver conteúdo inválido, mas não move o foco automaticamente.
- CPF avança após os 11 dígitos passarem na validação oficial.
- Nascimento e expedição aceitam uma sequência contínua de oito dígitos e a exibem como `DD/MM/AAAA`; também permitem edição parcial. Avançam só quando a data é real, completa e dentro de uma faixa aceitável.
- Telefone avança quando tiver 10 ou 11 dígitos válidos para o formato brasileiro.
- CEP avança e dispara a consulta quando tiver oito dígitos. O usuário pode sempre tocar em qualquer campo anterior para corrigir seu conteúdo.

Os campos com máscara mantêm os dígitos como valor canônico na edição e
calculam corretamente a seleção/cursor após inserir ou apagar caracteres. Isso
remove o salto atual do cursor no CPF. RG não receberá máscara automática,
evitando que pontuação seja introduzida no meio da digitação.

## Componentes e validação

Serão extraídas utilidades puras para CPF, CEP, telefone e data: normalização,
formatação e validação. A tela Compose usará componentes de campo reutilizáveis
com `FocusRequester`, ações IME e controle de cursor; a lógica de máscara não
será duplicada em callbacks individuais.

Os valores que chegam de OCR/IA serão normalizados para exibição, sem trocar
um valor reconhecido que já esteja completo. Datas reconhecidas em `DD/MM/AAAA`
permanecem visualmente e semanticamente iguais.

## Consulta de CEP

O aplicativo consultará o endpoint HTTPS JSON do ViaCEP somente após validar
localmente oito dígitos. A consulta rodará fora da thread de interface, terá
timeout curto e produzirá estados explícitos: aguardando, consultando,
encontrado, CEP inexistente e erro de rede/serviço. Falhas não bloqueiam o
formulário nem o salvamento manual.

Uma resposta encontrada fornece logradouro, bairro, localidade e UF. Número e
complemento nunca são preenchidos pela consulta.

Para cada um de rua, bairro, cidade e UF:

- se o campo estiver vazio, o valor do ViaCEP é aplicado;
- se for igual ao valor existente, nada muda;
- se divergir, o campo atual é preservado e um diálogo mostra ambos os valores
  para o usuário escolher;
- o mesmo mecanismo cobre dados inicialmente vindos de OCR/IA e valores que o
  usuário já tenha digitado, portanto a API nunca sobrescreve silenciosamente.

Uma nova consulta torna obsoleta a anterior, evitando que uma resposta atrasada
de um CEP antigo altere o formulário.

## Dados e compatibilidade

`Cliente` e `DadosExtraidos` receberão o campo `uf`; a ficha do cliente o
mostrará. O backend de IA passará a aceitar/devolver UF quando ela estiver
claramente presente no texto. Parsers locais permanecem permissivos e deixam o
campo vazio se não houver evidência.

O banco Room será elevado de versão com uma `Migration` que adiciona a coluna
`uf` com valor padrão vazio. A base existente não será apagada. Todos os demais
campos e rotas permanecem compatíveis.

## Verificação

Serão incluídos testes unitários para formatos e validações (incluindo cursor
na máscara de CPF), data, telefone, CEP e interpretação da resposta do ViaCEP.
Depois, o projeto será compilado e os cenários principais serão revisados:
cadastro manual, cadastro a partir de OCR, importação de conversa, CEP
encontrado, CEP inexistente, falha de rede, conflito de endereço e edição de
cliente existente.
