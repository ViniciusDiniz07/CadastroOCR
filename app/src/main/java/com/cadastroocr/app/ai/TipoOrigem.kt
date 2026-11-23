package com.cadastroocr.app.ai

/**
 * De onde veio o texto reconhecido pelo OCR. A IA remota usa o MESMO prompt
 * e schema pros dois casos (é o próprio texto que diz o que tem disponível),
 * mas esse tipo ainda importa para escolher o parser de REGRAS certo quando
 * o app está offline ou a IA falha (ver ClienteViewModel.interpretarTexto).
 */
enum class TipoOrigem { DOCUMENTO, CONVERSA }
