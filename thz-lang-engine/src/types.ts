export enum TokenType {
  PROGRAMA = 'PROGRAMA',
  FIM_PROGRAMA = 'FIM_PROGRAMA',
  METADADOS_ARQUITETURA = 'METADADOS_ARQUITETURA',
  FIM_METADADOS = 'FIM_METADADOS',
  ESTRUTURA = 'ESTRUTURA',
  FIM_ESTRUTURA = 'FIM_ESTRUTURA',
  ENUMERACAO = 'ENUMERACAO',
  FIM_ENUMERACAO = 'FIM_ENUMERACAO',
  REGRA_NEGOCIO = 'REGRA_NEGOCIO',
  FIM_REGRA_NEGOCIO = 'FIM_REGRA_NEGOCIO',
  PROCEDIMENTO = 'PROCEDIMENTO',
  INICIO = 'INICIO',
  FIM = 'FIM',
  EXIGE = 'EXIGE',
  GARANTE = 'GARANTE',
  INVARIANTE = 'INVARIANTE',
  FALHAR_COM = 'FALHAR_COM',
  CONTRATO_ENTRADA = 'CONTRATO_ENTRADA',
  FIM_CONTRATO_ENTRADA = 'FIM_CONTRATO_ENTRADA',
  CONTRATO_SAIDA = 'CONTRATO_SAIDA',
  FIM_CONTRATO_SAIDA = 'FIM_CONTRATO_SAIDA',
  VARIAVEL = 'VARIAVEL',
  RETORNE = 'RETORNE',
  EXIBA = 'EXIBA',
  OPERACAO = 'OPERACAO',
  SE = 'SE',
  SENAO = 'SENAO',
  ENQUANTO = 'ENQUANTO',
  FIM_SE = 'FIM_SE',
  FIM_ENQUANTO = 'FIM_ENQUANTO',
  VERDADEIRO = 'VERDADEIRO',
  FALSO = 'FALSO',
  NULO = 'NULO',
  VETORIZAR_PARA = 'VETORIZAR_PARA',
  EM = 'EM',
  PASSO_SIMD = 'PASSO_SIMD',
  PARA = 'PARA',
  PASSO = 'PASSO',
  DE = 'DE',
  ATE = 'ATE',
  CRIAR = 'CRIAR',
  LER = 'LER',
  FIM_PARA = 'FIM_PARA',
  USAR_BLOCO_MEMORIA = 'USAR_BLOCO_MEMORIA',
  FIM_BLOCO_MEMORIA = 'FIM_BLOCO_MEMORIA',
  LAYOUT_COLUNAR = 'LAYOUT_COLUNAR',
  VERSAO_LINGUAGEM = 'VERSAO_LINGUAGEM',
  OPERADOR_LOGICO = 'OPERADOR_LOGICO',
  SETA_ATRIBUICAO = 'SETA_ATRIBUICAO',
  IDENTIFICADOR = 'IDENTIFICADOR',
  STRING_LITERAL = 'STRING_LITERAL',
  NUMERO_LITERAL = 'NUMERO_LITERAL',
  DOIS_PONTOS = ':',
  PONTO = '.',
  VIRGULA = ',',
  ABRE_PARENTESE = '(',
  FECHA_PARENTESE = ')',
  ABRE_COLCHETE = '[',
  FECHA_COLCHETE = ']',
  OPERADOR_RELACIONAL = 'OPERADOR_RELACIONAL',
  OPERADOR_ARITMETICO = 'OPERADOR_ARITMETICO',
  EOF = 'EOF'
}

export interface Token {
  type: TokenType;
  value: string;
  line: number;
  column: number;
}

export interface MetadadosArquiteturaAST {
  dominio: string;
  subdominio: string;
  camada: string;
  versao: string;
  autor: string;
  sloLatencia: string;
  conformidade: string[];
}

export interface CampoEstruturaAST {
  nome: string;
  tipo: string;
}

/* ============================================================
 * EXPRESSÕES (v2.2) — árvores avaliáveis com posição no fonte
 * ============================================================ */

export type OperadorBinario =
  | '+' | '-' | '*' | '/' | '%'
  | '=' | '<>' | '<' | '<=' | '>' | '>='
  | 'E' | 'OU';

export type ExprAST =
  | LiteralInteiroAST
  | LiteralDecimalAST
  | LiteralTextoAST
  | LiteralLogicoAST
  | NuloAST
  | AcessoCampoAST
  | ChamadaAST
  | IndexacaoAST
  | FatiaLiteralAST
  | CriarRegistroAST
  | OpBinariaAST
  | OpUnariaAST;

interface PosicaoAST {
  linha: number;
  coluna: number;
}

export interface LiteralInteiroAST extends PosicaoAST {
  tipo: 'LITERAL_INTEIRO';
  valor: bigint;
}

/** Escalado segundo ISO/IEC 10967: valor real = escalado / 10^escala */
export interface LiteralDecimalAST extends PosicaoAST {
  tipo: 'LITERAL_DECIMAL';
  escalado: bigint;
  escala: number;
}

export interface LiteralTextoAST extends PosicaoAST {
  tipo: 'LITERAL_TEXTO';
  valor: string;
}

export interface LiteralLogicoAST extends PosicaoAST {
  tipo: 'LITERAL_LOGICO';
  valor: boolean;
}

export interface NuloAST extends PosicaoAST {
  tipo: 'NULO';
}

/** Caminho de acesso qualificado por ponto: item.valor_unitario */
export interface AcessoCampoAST extends PosicaoAST {
  tipo: 'ACESSO';
  caminho: string[];
}

export interface OpBinariaAST extends PosicaoAST {
  tipo: 'OP_BINARIA';
  operador: OperadorBinario;
  esquerda: ExprAST;
  direita: ExprAST;
}

export interface OpUnariaAST extends PosicaoAST {
  tipo: 'OP_UNARIA';
  operador: '-' | 'NAO';
  operando: ExprAST;
}

export interface ChamadaAST extends PosicaoAST {
  tipo: 'CHAMADA';
  caminho: string[];
  argumentos: ExprAST[];
}

export interface IndexacaoAST extends PosicaoAST {
  tipo: 'INDEXACAO';
  alvo: ExprAST;
  indice: ExprAST;
}

export interface FatiaLiteralAST extends PosicaoAST {
  tipo: 'FATIA_LITERAL';
  elementos: ExprAST[];
}

export interface CriarRegistroAST extends PosicaoAST {
  tipo: 'CRIAR_REGISTRO';
  nomeEstrutura: string;
  campos: { nome: string; valor: ExprAST }[];
}

/* ============================================================
 * CONTRATOS FORMAIS (v2.2) — cláusulas com árvore avaliável
 * ============================================================ */

export interface ClausulaContratoAST extends PosicaoAST {
  tipoClausula: 'EXIGE' | 'GARANTE';
  expressao: ExprAST;
  /** Representação textual canônica preservada para docgen e auditoria. */
  textoCanonico: string;
}

/* ============================================================
 * COMANDOS (v2.2) — corpos de operação executáveis
 * ============================================================ */

export type ComandoAST =
  | DeclVariavelAST
  | AtribuicaoAST
  | SeAST
  | EnquantoAST
  | VetorizarParaAST
  | ParaAST
  | BlocoMemoriaAST
  | ExibaAST
  | LerAST
  | ChamadaComandoAST
  | RetorneAST
  | FalharComAST;

export interface DeclVariavelAST extends PosicaoAST {
  tipoComando: 'DECL_VARIAVEL';
  nome: string;
  tipoDado: string;
  inicializacao: ExprAST;
}

export interface AtribuicaoAST extends PosicaoAST {
  tipoComando: 'ATRIBUICAO';
  alvo: string[];
  expressao: ExprAST;
}

export interface SeAST extends PosicaoAST {
  tipoComando: 'SE';
  condicao: ExprAST;
  entao: ComandoAST[];
  senao: ComandoAST[];
}

export interface EnquantoAST extends PosicaoAST {
  tipoComando: 'ENQUANTO';
  condicao: ExprAST;
  corpo: ComandoAST[];
}

export interface VetorizarParaAST extends PosicaoAST {
  tipoComando: 'VETORIZAR_PARA';
  variavel: string;
  fonte: string[];
  passoSimd?: number;
  corpo: ComandoAST[];
}

export interface ParaAST extends PosicaoAST {
  tipoComando: 'PARA';
  variavel: string;
  inicio: ExprAST;
  fim: ExprAST;
  passo?: ExprAST;
  corpo: ComandoAST[];
}

export interface BlocoMemoriaAST extends PosicaoAST {
  tipoComando: 'BLOCO_MEMORIA';
  nome: string;
  corpo: ComandoAST[];
}

export interface ExibaAST extends PosicaoAST {
  tipoComando: 'EXIBA';
  expressao: ExprAST;
}

export interface LerAST extends PosicaoAST {
  tipoComando: 'LER';
  alvo: string[];
}

export interface ChamadaComandoAST extends PosicaoAST {
  tipoComando: 'CHAMADA';
  expressao: ExprAST;
}

export interface RetorneAST extends PosicaoAST {
  tipoComando: 'RETORNE';
  expressao?: ExprAST;
}

/** Aborta a operação com valor de erro (RESULTADO[T,E]). */
export interface FalharComAST extends PosicaoAST {
  tipoComando: 'FALHAR_COM';
  expressao: ExprAST;
}

export interface EnumeracaoAST {
  nome: string;
  membros: string[];
}

export interface InvarianteAST extends PosicaoAST {
  expressao: ExprAST;
  textoCanonico: string;
}

export interface EstruturaAST {
  nome: string;
  layoutColunar: boolean;
  campos: CampoEstruturaAST[];
  invariantes: InvarianteAST[];
}

export interface ParametroOperacaoAST {
  nome: string;
  /** Tipo declarado, preservado verbatim para docgen (ex.: 'FATIA[ItemFatura]'). */
  tipo: string;
}

export interface OperacaoAST {
  nome: string;
  parametros: ParametroOperacaoAST[];
  tipoRetorno: string;
  corpo: ComandoAST[];
}

export interface ProcedimentoAST {
  nome: string;
  parametros: ParametroOperacaoAST[];
  corpo: ComandoAST[];
}

export interface RegraNegocioAST {
  nome: string;
  identificador?: string;
  rastreioRequisito?: string;
  descricao?: string;
  clausulasEntrada: ClausulaContratoAST[];
  clausulasSaida: ClausulaContratoAST[];
  operacoes: OperacaoAST[];
}

export interface ProgramaAST {
  nome: string;
  versaoLinguagem?: string;
  metadados?: MetadadosArquiteturaAST;
  estruturas: EstruturaAST[];
  enumeracoes: EnumeracaoAST[];
  regras: RegraNegocioAST[];
  procedimentos: ProcedimentoAST[];
}
