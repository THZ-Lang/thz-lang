import { TokenType } from './types.js';

/**
 * Fonte unica e canonica das palavras reservadas da THZ-LANG.
 *
 * POLITICA DE ESTABILIDADE (v2.2+):
 *  - Palavras reservadas NUNCA sao removidas ou renomeadas.
 *  - Novas palavras so entram em versoes minor/major, nunca em patch.
 *  - Palavras reservadas sao ESTRITAS: nao podem ser usadas como
 *    identificadores; o parser reporta [Erro Sintatico][Linha L:C].
 */

export const VERSAO_LINGUAGEM_ATUAL = '2.3.0';

export enum CategoriaPalavra {
  DECLARACAO = 'DECLARACAO',
  FIM_BLOCO = 'FIM_BLOCO',
  CONTRATO = 'CONTRATO',
  CONTROLE = 'CONTROLE',
  MEMORIA = 'MEMORIA',
  MODIFICADOR = 'MODIFICADOR',
  LITERAL = 'LITERAL',
  CONECTIVO_LOGICO = 'CONECTIVO_LOGICO'
}

export interface EntradaPalavraReservada {
  token: TokenType;
  categoria: CategoriaPalavra;
}

const TABELA: Readonly<Record<string, EntradaPalavraReservada>> = Object.freeze({
  // --- Declaracao estrutural ---
  PROGRAMA: { token: TokenType.PROGRAMA, categoria: CategoriaPalavra.DECLARACAO },
  METADADOS_ARQUITETURA: { token: TokenType.METADADOS_ARQUITETURA, categoria: CategoriaPalavra.DECLARACAO },
  ESTRUTURA: { token: TokenType.ESTRUTURA, categoria: CategoriaPalavra.DECLARACAO },
  ENUMERACAO: { token: TokenType.ENUMERACAO, categoria: CategoriaPalavra.DECLARACAO },
  REGRA_NEGOCIO: { token: TokenType.REGRA_NEGOCIO, categoria: CategoriaPalavra.DECLARACAO },
  PROCEDIMENTO: { token: TokenType.PROCEDIMENTO, categoria: CategoriaPalavra.DECLARACAO },
  OPERACAO: { token: TokenType.OPERACAO, categoria: CategoriaPalavra.DECLARACAO },
  VARIAVEL: { token: TokenType.VARIAVEL, categoria: CategoriaPalavra.DECLARACAO },
  VERSAO_LINGUAGEM: { token: TokenType.VERSAO_LINGUAGEM, categoria: CategoriaPalavra.DECLARACAO },

  // --- Fechamento de blocos ---
  FIM_PROGRAMA: { token: TokenType.FIM_PROGRAMA, categoria: CategoriaPalavra.FIM_BLOCO },
  FIM_METADADOS: { token: TokenType.FIM_METADADOS, categoria: CategoriaPalavra.FIM_BLOCO },
  FIM_ESTRUTURA: { token: TokenType.FIM_ESTRUTURA, categoria: CategoriaPalavra.FIM_BLOCO },
  FIM_ENUMERACAO: { token: TokenType.FIM_ENUMERACAO, categoria: CategoriaPalavra.FIM_BLOCO },
  FIM_REGRA_NEGOCIO: { token: TokenType.FIM_REGRA_NEGOCIO, categoria: CategoriaPalavra.FIM_BLOCO },
  FIM_PARA: { token: TokenType.FIM_PARA, categoria: CategoriaPalavra.FIM_BLOCO },
  FIM_BLOCO_MEMORIA: { token: TokenType.FIM_BLOCO_MEMORIA, categoria: CategoriaPalavra.FIM_BLOCO },
  FIM_SE: { token: TokenType.FIM_SE, categoria: CategoriaPalavra.FIM_BLOCO },
  FIM_ENQUANTO: { token: TokenType.FIM_ENQUANTO, categoria: CategoriaPalavra.FIM_BLOCO },
  FIM: { token: TokenType.FIM, categoria: CategoriaPalavra.FIM_BLOCO },

  // --- Contratos formais (Design by Contract) ---
  EXIGE: { token: TokenType.EXIGE, categoria: CategoriaPalavra.CONTRATO },
  GARANTE: { token: TokenType.GARANTE, categoria: CategoriaPalavra.CONTRATO },
  INVARIANTE: { token: TokenType.INVARIANTE, categoria: CategoriaPalavra.CONTRATO },
  CONTRATO_ENTRADA: { token: TokenType.CONTRATO_ENTRADA, categoria: CategoriaPalavra.CONTRATO },
  FIM_CONTRATO_ENTRADA: { token: TokenType.FIM_CONTRATO_ENTRADA, categoria: CategoriaPalavra.CONTRATO },
  CONTRATO_SAIDA: { token: TokenType.CONTRATO_SAIDA, categoria: CategoriaPalavra.CONTRATO },
  FIM_CONTRATO_SAIDA: { token: TokenType.FIM_CONTRATO_SAIDA, categoria: CategoriaPalavra.CONTRATO },

  // --- Controle de fluxo ---
  INICIO: { token: TokenType.INICIO, categoria: CategoriaPalavra.CONTROLE },
  SE: { token: TokenType.SE, categoria: CategoriaPalavra.CONTROLE },
  SENAO: { token: TokenType.SENAO, categoria: CategoriaPalavra.CONTROLE },
  ENQUANTO: { token: TokenType.ENQUANTO, categoria: CategoriaPalavra.CONTROLE },
  RETORNE: { token: TokenType.RETORNE, categoria: CategoriaPalavra.CONTROLE },
  FALHAR_COM: { token: TokenType.FALHAR_COM, categoria: CategoriaPalavra.CONTROLE },
  EXIBA: { token: TokenType.EXIBA, categoria: CategoriaPalavra.CONTROLE },
  LER: { token: TokenType.LER, categoria: CategoriaPalavra.CONTROLE },
  VETORIZAR_PARA: { token: TokenType.VETORIZAR_PARA, categoria: CategoriaPalavra.CONTROLE },

  // --- Memoria ---
  USAR_BLOCO_MEMORIA: { token: TokenType.USAR_BLOCO_MEMORIA, categoria: CategoriaPalavra.MEMORIA },

  // --- Modificadores e qualificadores sintaticos ---
  LAYOUT_COLUNAR: { token: TokenType.LAYOUT_COLUNAR, categoria: CategoriaPalavra.MODIFICADOR },
  EM: { token: TokenType.EM, categoria: CategoriaPalavra.MODIFICADOR },
  PASSO_SIMD: { token: TokenType.PASSO_SIMD, categoria: CategoriaPalavra.MODIFICADOR },
  PARA: { token: TokenType.PARA, categoria: CategoriaPalavra.CONTROLE },
  PASSO: { token: TokenType.PASSO, categoria: CategoriaPalavra.MODIFICADOR },
  DE: { token: TokenType.DE, categoria: CategoriaPalavra.MODIFICADOR },
  ATE: { token: TokenType.ATE, categoria: CategoriaPalavra.MODIFICADOR },

  // --- Construcao ---
  CRIAR: { token: TokenType.CRIAR, categoria: CategoriaPalavra.DECLARACAO },

  // --- Literais verbais ---
  VERDADEIRO: { token: TokenType.VERDADEIRO, categoria: CategoriaPalavra.LITERAL },
  FALSO: { token: TokenType.FALSO, categoria: CategoriaPalavra.LITERAL },
  NULO: { token: TokenType.NULO, categoria: CategoriaPalavra.LITERAL },

  // --- Conectivos logicos (operadores verbais) ---
  E: { token: TokenType.OPERADOR_LOGICO, categoria: CategoriaPalavra.CONECTIVO_LOGICO },
  OU: { token: TokenType.OPERADOR_LOGICO, categoria: CategoriaPalavra.CONECTIVO_LOGICO },
  NAO: { token: TokenType.OPERADOR_LOGICO, categoria: CategoriaPalavra.CONECTIVO_LOGICO }
});

export const PALAVRAS_RESERVADAS = TABELA;

export function ehPalavraReservada(palavra: string): boolean {
  return Object.prototype.hasOwnProperty.call(TABELA, palavra);
}

export function tokenDe(palavra: string): TokenType | undefined {
  return TABELA[palavra]?.token;
}

export function categoriaDe(palavra: string): CategoriaPalavra | undefined {
  return TABELA[palavra]?.categoria;
}

export function palavrasPorCategoria(categoria: CategoriaPalavra): string[] {
  return Object.keys(TABELA).filter((p) => TABELA[p].categoria === categoria);
}
