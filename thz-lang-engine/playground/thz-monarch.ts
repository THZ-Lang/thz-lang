/* THZ-LANG Monarch tokenizer para Monaco (G2 + v2.3) */

export const thzMonarch = {
  // Lista espelhada de src/keywords.ts e docs/GRAMATICA.md
  keywords: [
    'PROGRAMA','FIM_PROGRAMA','METADADOS_ARQUITETURA','FIM_METADADOS',
    'ESTRUTURA','FIM_ESTRUTURA','ENUMERACAO','FIM_ENUMERACAO',
    'REGRA_NEGOCIO','FIM_REGRA_NEGOCIO','OPERACAO','PROCEDIMENTO','VARIAVEL','VERSAO_LINGUAGEM',
    'CONTRATO_ENTRADA','FIM_CONTRATO_ENTRADA','CONTRATO_SAIDA','FIM_CONTRATO_SAIDA',
    'EXIGE','GARANTE','INVARIANTE','INICIO','FIM','SE','SENAO','FIM_SE','ENQUANTO','FIM_ENQUANTO',
    'VETORIZAR_PARA','FIM_PARA','PARA','USAR_BLOCO_MEMORIA','FIM_BLOCO_MEMORIA',
    'RETORNE','FALHAR_COM','EXIBA','LER','CRIAR','LAYOUT_COLUNAR','EM','PASSO_SIMD','PASSO','DE','ATE',
  ],
  typeKeywords: [
    'TEXTO','LOGICO','UUID','DATA','DATA_HORA','NATURAL8','NATURAL16','NATURAL32','NATURAL64',
    'INTEIRO8','INTEIRO16','INTEIRO32','INTEIRO64','DECIMAL','MONETARIO','FATIA','RESULTADO','LISTA'
  ],
  operators: ['<-','=','<>','<=','>=','<','>','+','-','*','/','%'],
  logical: ['E','OU','NAO'],
  literals: ['VERDADEIRO','FALSO','NULO'],

  tokenizer: {
    root: [
      [/#.*$/, 'comment'],
      [/"/, 'string', '@string'],
      [/\b\d[\d_]*(\.\d[\d_]*)?\b/, 'number'],
      [/[a-zA-Z_]\w*/, {
        cases: {
          '@keywords': 'keyword',
          '@typeKeywords': 'type',
          '@logical': 'keyword.logic',
          '@literals': 'keyword.literal',
          '@default': 'identifier',
        }
      }],
      [/[<>]=|<>|<-/, 'operator'],
      [/[=<>+\-*/%]/, 'operator'],
      [/[():,\.\[\]]/, 'delimiter'],
      [/\s+/, 'white'],
    ],
    string: [
      [/[^"\\]+/, 'string'],
      [/\\n/, 'string.escape'],
      [/"/, 'string', '@pop'],
    ],
  },
} as const;
