/* ============================================================
 * THZ-LANG — Language Service Core (G1)
 *
 * Camada única sobre lexer / parser / analisador semântico.
 * Fornece:
 *  - analyze(source)  : pipeline completo com diagnósticos unificados
 *  - símbolos         : estruturas, enumerações, regras, operações, campos…
 *  - hover            : tipo / assinatura do símbolo sob o cursor
 *  - helpers de posição (offset ↔ linha:coluna, token no cursor)
 *
 * Esta API é a base do Playground (G2) e do servidor LSP (G3).
 * ============================================================ */

import { ThzLexer } from './lexer.js';
import { ThzParser } from './parser.js';
import { AnalisadorSemantico, ErroSemantico, OpcoesAnalise } from './analisador.js';
import { formatarDiagnosticos, formatarErroComCaret, DiagnosticoEntrada } from './errors.js';
import { ProgramaAST, TokenType, Token, ComandoAST, ExprAST } from './types.js';
import { analisarNomeTipo, descrever } from './tipos.js';
import { auditar as auditarGovernanca, gerarMarkdownGovernanca, AuditoriaGovernanca, OpcoesAuditoria } from './governanca.js';
import { baixarParaIr as baixarIr, serializarIr, emitirLlvm, IrPrograma } from './ir.js';
import { formatar } from './fmt.js';

// ------------------------------------------------------------------
// Tipos públicos
// ------------------------------------------------------------------

export type OrigemDiagnostico = 'lexico' | 'sintatico' | 'semantico';

export interface Diagnostico extends DiagnosticoEntrada {
  origem: OrigemDiagnostico;
  severidade: 'erro';
}

export interface Simbolo {
  nome: string;
  categoria:
    | 'programa'
    | 'estrutura'
    | 'campo'
    | 'enumeracao'
    | 'membro-enum'
    | 'regra'
    | 'operacao'
    | 'parametro'
    | 'variavel';
  detalhe?: string;
  linha: number;
  coluna: number;
  container?: string; // ex.: campo pertence a estrutura X
}

export interface Posicao {
  linha: number;
  coluna: number;
}

export interface HoverInfo {
  conteudo: string; // markdown simples
  range?: { linha: number; coluna: number; comprimento: number };
}

export interface ResultadoAnalise {
  fonte: string;
  tokens?: Token[];
  ast?: ProgramaAST;
  diagnosticos: Diagnostico[];
  textoDiagnosticos: string[]; // blocos com caret, prontos para stderr
  temErros: boolean;
  simbolos: Simbolo[];
}

// ------------------------------------------------------------------
// Helpers de posição
// ------------------------------------------------------------------

export function posicaoParaOffset(fonte: string, linha: number, coluna: number): number {
  const linhas = fonte.split(/\r?\n/);
  let offset = 0;
  for (let i = 0; i < linha - 1 && i < linhas.length; i++) {
    offset += linhas[i].length + 1; // \n
  }
  offset += Math.max(0, coluna - 1);
  return offset;
}

export function offsetParaPosicao(fonte: string, offset: number): Posicao {
  const ate = fonte.slice(0, Math.max(0, offset));
  const linhas = ate.split('\n');
  const linha = linhas.length;
  const coluna = (linhas[linhas.length - 1]?.length ?? 0) + 1;
  return { linha, coluna };
}

export function tokenNoCursor(tokens: Token[], linha: number, coluna: number): Token | undefined {
  return tokens.find((t) => t.line === linha && coluna >= t.column && coluna < t.column + t.value.length);
}

// ------------------------------------------------------------------
// Extração de símbolos (via AST + tokens para posições)
// ------------------------------------------------------------------

function simbolosDe(ast: ProgramaAST, tokens?: Token[]): Simbolo[] {
  const simbolos: Simbolo[] = [];
  // índice token → lookup rápido de posição por valor
  const posPorNome = new Map<string, Posicao[]>();
  if (tokens) {
    for (const t of tokens) {
      if (t.type === TokenType.IDENTIFICADOR) {
        const arr = posPorNome.get(t.value) ?? [];
        arr.push({ linha: t.line, coluna: t.column });
        posPorNome.set(t.value, arr);
      }
    }
  }
  const primeiraPos = (nome: string): Posicao => posPorNome.get(nome)?.[0] ?? { linha: 1, coluna: 1 };

  // programa
  {
    const p = primeiraPos(ast.nome);
    simbolos.push({ nome: ast.nome, categoria: 'programa', linha: p.linha, coluna: p.coluna });
  }

  for (const e of ast.estruturas) {
    const p = primeiraPos(e.nome);
    simbolos.push({ nome: e.nome, categoria: 'estrutura', detalhe: e.layoutColunar ? 'LAYOUT_COLUNAR' : undefined, linha: p.linha, coluna: p.coluna });
    for (const c of e.campos) {
      const pc = primeiraPos(c.nome);
      // tenta achar posição mais próxima do campo dentro da estrutura: varre tokens sequencialmente — fallback simples
      simbolos.push({ nome: c.nome, categoria: 'campo', detalhe: c.tipo, linha: pc.linha, coluna: pc.coluna, container: e.nome });
    }
    for (const inv of e.invariantes) {
      // invariantes não geram símbolo nominal próprio; documentado via detalhe da estrutura
      void inv;
    }
  }

  for (const en of ast.enumeracoes) {
    const p = primeiraPos(en.nome);
    simbolos.push({ nome: en.nome, categoria: 'enumeracao', linha: p.linha, coluna: p.coluna });
    for (const m of en.membros) {
      const pm = primeiraPos(m);
      simbolos.push({ nome: m, categoria: 'membro-enum', container: en.nome, linha: pm.linha, coluna: pm.coluna });
    }
  }

  for (const regra of ast.regras) {
    const pr = primeiraPos(regra.nome);
    simbolos.push({ nome: regra.nome, categoria: 'regra', detalhe: regra.identificador ?? regra.rastreioRequisito, linha: pr.linha, coluna: pr.coluna });
    for (const op of regra.operacoes) {
      const po = primeiraPos(op.nome);
      const assinatura = op.parametros.map((p) => p.nome + ': ' + p.tipo).join(', ') + ' : ' + op.tipoRetorno;
      simbolos.push({ nome: op.nome, categoria: 'operacao', detalhe: assinatura, linha: po.linha, coluna: po.coluna, container: regra.nome });
      for (const param of op.parametros) {
        const pp = primeiraPos(param.nome);
        simbolos.push({ nome: param.nome, categoria: 'parametro', detalhe: param.tipo, linha: pp.linha, coluna: pp.coluna, container: op.nome });
      }
      coletarVariaveis(op.corpo, simbolos, posPorNome, op.nome);
    }
  }

  return simbolos;
}

function coletarVariaveis(comandos: ComandoAST[], out: Simbolo[], posPorNome: Map<string, Posicao[]>, container: string): void {
  for (const cmd of comandos) {
    switch (cmd.tipoComando) {
      case 'DECL_VARIAVEL': {
        // usa posição do comando (linha/coluna da palavra VARIAVEL) como âncora do símbolo
        // para não conflitar com outros usos do mesmo identificador
        out.push({ nome: cmd.nome, categoria: 'variavel', detalhe: cmd.tipoDado, linha: cmd.linha, coluna: cmd.coluna, container });
        break;
      }
      case 'SE':
        coletarVariaveis(cmd.entao, out, posPorNome, container);
        coletarVariaveis(cmd.senao, out, posPorNome, container);
        break;
      case 'ENQUANTO':
        coletarVariaveis(cmd.corpo, out, posPorNome, container);
        break;
      case 'VETORIZAR_PARA':
        // variável de iteração é declarada implicitamente
        out.push({ nome: cmd.variavel, categoria: 'variavel', detalhe: 'elemento de ' + cmd.fonte.join('.'), linha: cmd.linha, coluna: cmd.coluna, container });
        coletarVariaveis(cmd.corpo, out, posPorNome, container);
        break;
      case 'BLOCO_MEMORIA':
        coletarVariaveis(cmd.corpo, out, posPorNome, container);
        break;
      default:
        break;
    }
  }
}

// ------------------------------------------------------------------
// Pipeline unificado
// ------------------------------------------------------------------

function extrairLinhaColuna(mensagem: string): { linha: number; coluna: number } | undefined {
  const m = /\[Linha (\d+):(\d+)\]/.exec(mensagem);
  if (!m) return undefined;
  return { linha: Number.parseInt(m[1], 10), coluna: Number.parseInt(m[2], 10) };
}

export function analisar(fonte: string, opcoes: OpcoesAnalise = {}): ResultadoAnalise {
  const diagnosticos: Diagnostico[] = [];
  let tokens: Token[] | undefined;
  let ast: ProgramaAST | undefined;

  // 1) léxico
  try {
    tokens = new ThzLexer(fonte).tokenize();
  } catch (e) {
    const mensagem = (e as Error).message;
    const pos = extrairLinhaColuna(mensagem) ?? { linha: 1, coluna: 1 };
    diagnosticos.push({ linha: pos.linha, coluna: pos.coluna, mensagem, origem: 'lexico', severidade: 'erro' });
    const textoDiagnosticos = formatarDiagnosticos(fonte, diagnosticos.map((d) => ({ linha: d.linha, coluna: d.coluna, mensagem: d.mensagem })), '');
    return { fonte, tokens, diagnosticos, textoDiagnosticos, temErros: true, simbolos: [] };
  }

  // 2) sintaxe
  try {
    ast = new ThzParser(tokens!).parse();
  } catch (e) {
    const mensagem = (e as Error).message;
    const pos = extrairLinhaColuna(mensagem) ?? { linha: 1, coluna: 1 };
    diagnosticos.push({ linha: pos.linha, coluna: pos.coluna, mensagem, origem: 'sintatico', severidade: 'erro' });
    const textoDiagnosticos = diagnosticos.map((d) => formatarErroComCaret(fonte, d));
    return { fonte, tokens, diagnosticos, textoDiagnosticos, temErros: true, simbolos: [] };
  }

  // 3) semântica
  const errosSemanticos: ErroSemantico[] = new AnalisadorSemantico(ast!).analisar(opcoes);
  for (const e of errosSemanticos) {
    diagnosticos.push({ linha: e.linha, coluna: e.coluna, mensagem: e.mensagem, origem: 'semantico', severidade: 'erro' });
  }

  const temErros = diagnosticos.length > 0;
  const textoDiagnosticos = diagnosticos.length > 0 ? formatarDiagnosticos(fonte, diagnosticos) : [];
  const simbolos = ast ? simbolosDe(ast, tokens) : [];

  return { fonte, tokens, ast, diagnosticos, textoDiagnosticos, temErros, simbolos };
}

// ------------------------------------------------------------------
// Hover — resolve o símbolo sob o cursor para tipo/assinatura
// ------------------------------------------------------------------

function palavraNaPosicao(fonte: string, linha: number, coluna: number): { palavra: string; colunaInicio: number } | undefined {
  const linhas = fonte.split(/\r?\n/);
  const conteudo = linhas[linha - 1];
  if (conteudo === undefined) return undefined;
  const idx = Math.max(0, Math.min(coluna - 1, conteudo.length));
  // expande para limites de identificador [A-Za-z0-9_]
  let ini = idx;
  let fim = idx;
  // se cursor está sobre separador, não há palavra
  if (conteudo[idx] !== undefined && !/[A-Za-z0-9_]/.test(conteudo[idx])) return undefined;
  while (ini > 0 && /[A-Za-z0-9_]/.test(conteudo[ini - 1] ?? '')) ini--;
  while (fim < conteudo.length && /[A-Za-z0-9_]/.test(conteudo[fim] ?? '')) fim++;
  const palavra = conteudo.slice(ini, fim);
  if (!palavra) return undefined;
  return { palavra, colunaInicio: ini + 1 };
}

export function obterHover(fonte: string, linha: number, coluna: number, opcoes: OpcoesAnalise = {}): HoverInfo | undefined {
  const resultado = analisar(fonte, opcoes);
  const ast = resultado.ast;
  if (!ast) return undefined;

  const alvo = palavraNaPosicao(fonte, linha, coluna);
  if (!alvo) return undefined;
  const { palavra, colunaInicio } = alvo;
  const range = { linha, coluna: colunaInicio, comprimento: palavra.length };

  // 1) símbolos diretos (programa, estrutura, enum, regra, operação, parâmetro, variável, campo, membro)
  const candidato = resultado.simbolos.find((s) => s.nome === palavra);
  // Para acesso qualificado (ex.: item.quantidade com cursor em quantidade)
  // tenta resolver campo via estrutura da variável base.
  const acessoQualificado = resolverCampoQualificado(fonte, linha, coluna, palavra, ast, resultado.simbolos);
  if (acessoQualificado) {
    return { conteudo: acessoQualificado, range };
  }

  if (candidato) {
    const cat = candidato.categoria;
    if (cat === 'estrutura') {
      const est = ast.estruturas.find((e) => e.nome === palavra);
      const campos = est ? est.campos.map((c) => c.nome + ': ' + c.tipo).join(', ') : '';
      const inv = est && est.invariantes.length > 0 ? '\n\n**Invariantes:** ' + est.invariantes.map((i) => '`' + i.textoCanonico + '`').join(', ') : '';
      const layout = est?.layoutColunar ? ' `LAYOUT_COLUNAR`' : '';
      return { conteudo: '**ESTRUTURA** `' + palavra + '`' + layout + (campos ? '\n\nCampos: `' + campos + '`' : '') + inv, range };
    }
    if (cat === 'campo') {
      return { conteudo: '**campo** `' + palavra + '` : `' + (candidato.detalhe ?? 'desconhecido') + '` — em `' + candidato.container + '`', range };
    }
    if (cat === 'enumeracao') {
      const en = ast.enumeracoes.find((e) => e.nome === palavra);
      const membros = en ? en.membros.join(', ') : '';
      return { conteudo: '**ENUMERACAO** `' + palavra + '` — membros: `' + membros + '`', range };
    }
    if (cat === 'membro-enum') {
      return { conteudo: '**membro** `' + palavra + '` : `' + (candidato.container ?? 'ENUMERACAO') + '`', range };
    }
    if (cat === 'regra') {
      const regra = ast.regras.find((r) => r.nome === palavra);
      return { conteudo: '**REGRA_NEGOCIO** `' + palavra + '`' + (regra?.identificador ? ' — ID: `' + regra.identificador + '`' : ''), range };
    }
    if (cat === 'operacao') {
      return { conteudo: '**OPERACAO** `' + palavra + '(' + (candidato.detalhe ?? '') + ')` — em `' + (candidato.container ?? '') + '`', range };
    }
    if (cat === 'parametro' || cat === 'variavel') {
      const tipo = candidato.detalhe ?? 'desconhecido';
      const tipoDesc = (() => {
        const t = analisarNomeTipo(tipo);
        return t ? descrever(t) : '`' + tipo + '`';
      })();
      return { conteudo: '**' + cat + '** `' + palavra + '` : ' + tipoDesc, range };
    }
    if (cat === 'programa') {
      return { conteudo: '**PROGRAMA** `' + palavra + '`' + (ast.versaoLinguagem ? ' — `VERSAO_LINGUAGEM "' + ast.versaoLinguagem + '"`' : ''), range };
    }
  }

  // 2) literal sob o cursor (número, texto, booleano) — fallback via token
  if (resultado.tokens) {
    const tok = tokenNoCursor(resultado.tokens, linha, coluna);
    if (tok) {
      if (tok.type === TokenType.NUMERO_LITERAL) {
        const ehDecimal = tok.value.includes('.');
        return { conteudo: ehDecimal ? '**literal decimal** `' + tok.value + '`' : '**literal inteiro** `' + tok.value + '`', range };
      }
      if (tok.type === TokenType.STRING_LITERAL) return { conteudo: '**literal texto** `"' + tok.value + '"`', range };
      if (tok.type === TokenType.VERDADEIRO || tok.type === TokenType.FALSO) return { conteudo: '**literal lógico** `' + tok.value + '` : `LOGICO`', range };
      if (tok.type === TokenType.NULO) return { conteudo: '**literal** `NULO`', range };
    }
  }

  // 3) tipo primitivo / nome de tipo escrito no fonte
  const tipoNome = analisarNomeTipo(palavra);
  if (tipoNome) {
    return { conteudo: '**tipo** `' + palavra + '` : ' + descrever(tipoNome), range };
  }

  return undefined;
}

function resolverCampoQualificado(
  fonte: string,
  linha: number,
  coluna: number,
  palavra: string,
  ast: ProgramaAST,
  simbolos: Simbolo[]
): string | undefined {
  const linhas = fonte.split(/\r?\n/);
  const conteudo = linhas[linha - 1];
  if (!conteudo) return undefined;
  // encontra início da palavra atual
  let ini = Math.max(0, coluna - 1);
  while (ini > 0 && /[A-Za-z0-9_]/.test(conteudo[ini - 1] ?? '')) ini--;
  // caractere antes do identificador
  let pos = ini - 1;
  while (pos >= 0 && conteudo[pos] === ' ') pos--;
  if (pos < 0 || conteudo[pos] !== '.') return undefined;
  // base antes do ponto
  let fimBase = pos - 1;
  while (fimBase >= 0 && conteudo[fimBase] === ' ') fimBase--;
  let iniBase = fimBase;
  while (iniBase >= 0 && /[A-Za-z0-9_]/.test(conteudo[iniBase] ?? '')) iniBase--;
  iniBase++;
  const base = conteudo.slice(iniBase, fimBase + 1);
  if (!base) return undefined;

  // resolve tipo da base
  const simboloBase = simbolos.find((s) => s.nome === base && (s.categoria === 'variavel' || s.categoria === 'parametro'));
  if (!simboloBase?.detalhe) return undefined;

  // base pode ser FATIA[T] ou registro direto
  let estruturaNome: string | undefined;
  const det = simboloBase.detalhe;
  const fatia = /^FATIA\s*\[\s*(\w+)\s*\]$/.exec(det);
  if (fatia) estruturaNome = fatia[1];
  else if (ast.estruturas.some((e) => e.nome === det)) estruturaNome = det;
  // caso de variável de iteração VETORIZAR_PARA: detalhe = "elemento de X"
  if (!estruturaNome && det.startsWith('elemento de ')) {
    const src = det.slice('elemento de '.length).split('.')[0].trim();
    const simboloOrigem = simbolos.find((s) => s.nome === src);
    if (simboloOrigem?.detalhe) {
      const f2 = /^FATIA\s*\[\s*(\w+)\s*\]$/.exec(simboloOrigem.detalhe);
      if (f2) estruturaNome = f2[1];
    }
  }
  if (!estruturaNome) return undefined;

  const estrutura = ast.estruturas.find((e) => e.nome === estruturaNome);
  const campo = estrutura?.campos.find((c) => c.nome === palavra);
  if (!campo) return undefined;

  return '**campo** `' + estruturaNome + '.' + palavra + '` : `' + campo.tipo + '`';
}

// Auditoria de governança (G4) — reuso no CLI, LSP e Playground
export function auditarFonte(fonte: string, opcoes: OpcoesAuditoria = {}): { resultado: ResultadoAnalise; auditoria: AuditoriaGovernanca | null } {
  const resultado = analisar(fonte, opcoes);
  const auditoria = resultado.ast ? auditarGovernanca(resultado.ast, opcoes) : null;
  return { resultado, auditoria };
}

// IR estável + ponte LLVM (G5)
export function baixarIrFonte(fonte: string, opcoes: OpcoesAuditoria = {}): { resultado: ResultadoAnalise; ir: IrPrograma | null; llvm?: string } {
  const resultado = analisar(fonte, opcoes);
  if (!resultado.ast) return { resultado, ir: null };
  const ir = baixarIr(resultado.ast);
  return { resultado, ir, llvm: emitirLlvm(ir) };
}

// Re-export utilitário para consumo externo (LSP / Playground)
export { formatarDiagnosticos, formatarErroComCaret, auditarGovernanca, gerarMarkdownGovernanca, baixarIr, serializarIr, emitirLlvm, formatar };
export type { AuditoriaGovernanca, OpcoesAuditoria, IrPrograma };

export function formatarFonte(fonte: string): { texto: string; mudou: boolean } {
  const r = analisar(fonte);
  if (!r.ast) return { texto: fonte, mudou: false };
  const fmt = formatar(r.ast);
  return { texto: fmt, mudou: fmt !== fonte };
}
