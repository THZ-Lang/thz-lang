import {
  ProgramaAST,
  RegraNegocioAST,
  OperacaoAST,
  ProcedimentoAST,
  ExprAST,
  ComandoAST,
  ClausulaContratoAST
} from './types.js';
import { DecimalFixo, ArenaMemoria, Monetario, ErroMonetario, DataThz, DataHoraThz, ErroData } from './runtime.js';

/* ============================================================
 * Valores do universo de execução THZ
 * ============================================================ */

export type ValorThz =
  | { classe: 'INTEIRO'; valor: bigint }
  | { classe: 'DECIMAL'; valor: DecimalFixo }
  | { classe: 'MONETARIO'; valor: Monetario }
  | { classe: 'TEXTO'; valor: string }
  | { classe: 'LOGICO'; valor: boolean }
  | { classe: 'NULO' }
  | { classe: 'DATA'; valor: DataThz }
  | { classe: 'DATA_HORA'; valor: DataHoraThz }
  | { classe: 'ENUMERADO'; nomeEnumeracao: string; valor: string }
  | { classe: 'RESULTADO'; sucesso: boolean; valor?: ValorThz; erro?: ValorThz }
  | { classe: 'REGISTRO'; nomeEstrutura: string; campos: Map<string, ValorThz> }
  | { classe: 'FATIA'; tipoInterno: string; elementos: ValorThz[] };

export const INTEIRO = (valor: bigint): ValorThz => ({ classe: 'INTEIRO', valor });
export const DECIMAL = (valor: DecimalFixo): ValorThz => ({ classe: 'DECIMAL', valor });
export const MONETARIO = (valor: Monetario): ValorThz => ({ classe: 'MONETARIO', valor });
export const TEXTO = (valor: string): ValorThz => ({ classe: 'TEXTO', valor });
export const LOGICO = (valor: boolean): ValorThz => ({ classe: 'LOGICO', valor });
export const NULO_THZ: ValorThz = { classe: 'NULO' };
export const ENUMERADO = (nomeEnumeracao: string, valor: string): ValorThz => ({ classe: 'ENUMERADO', nomeEnumeracao, valor });
export const DATA = (valor: DataThz): ValorThz => ({ classe: 'DATA', valor });
export const DATA_HORA = (valor: DataHoraThz): ValorThz => ({ classe: 'DATA_HORA', valor });

/** Exceção interna de fluxo para RETORNE (nunca atravessa o usuário). */
class SinalRetorne {
  constructor(public valor?: ValorThz) {}
}

/** Exceção interna de fluxo para FALHAR_COM (canal de erro de RESULTADO[T,E]). */
class SinalFalharCom {
  constructor(public valor: ValorThz) {}
}

export class ErroExecucao extends Error {}

/* ============================================================
 * Escopos lexically encadeados
 * ============================================================ */

export class Escopo {
  private variaveis = new Map<string, ValorThz>();

  constructor(public pai?: Escopo) {}

  public definir(nome: string, valor: ValorThz): void {
    this.variaveis.set(nome, valor);
  }

  /** Atualiza a variável no escopo onde ela foi declarada (sem shadowing acidental). */
  public atualizar(nome: string, valor: ValorThz): boolean {
    let atual: Escopo | undefined = this;
    while (atual) {
      if (atual.variaveis.has(nome)) {
        atual.variaveis.set(nome, valor);
        return true;
      }
      atual = atual.pai;
    }
    return false;
  }

  public resolver(nome: string): ValorThz | undefined {
    let atual: Escopo | undefined = this;
    while (atual) {
      const encontrado = atual.variaveis.get(nome);
      if (encontrado !== undefined) return encontrado;
      atual = atual.pai;
    }
    return undefined;
  }
}

/* ============================================================
 * Biblioteca padrão (stdlib) categorizada por namespace
 * ============================================================ */

type FnStdlib = (args: ValorThz[], ctx: ExprAST) => ValorThz;

function exigirAridade(nome: string, args: ValorThz[], esperada: number, ctx: ExprAST): void {
  if (args.length !== esperada) throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + "] Função '" + nome + "' exige " + esperada + ' argumento(s), recebidos ' + args.length + '.');
}

function exigirClasse(nome: string, v: ValorThz, classe: ValorThz['classe'], ctx: ExprAST): void {
  if (v.classe !== classe) throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + "] Função '" + nome + "' exige " + classe + ', recebido ' + v.classe + '.');
}

function comoInteiroArg(v: ValorThz, ctx: ExprAST): bigint {
  if (v.classe === 'INTEIRO') return v.valor;
  throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] Esperado INTEIRO, recebido ' + v.classe + '.');
}

function comoTexto(v: ValorThz): string {
  if (v.classe === 'TEXTO') return v.valor;
  return '';
}

const STDLIB: Record<string, FnStdlib> = {
  'TEXTO.comprimento': (args, ctx) => { exigirAridade('TEXTO.comprimento', args, 1, ctx); exigirClasse('TEXTO.comprimento', args[0], 'TEXTO', ctx); return INTEIRO(BigInt((args[0] as any).valor.length)); },
  'TEXTO.maiusculas': (args, ctx) => { exigirAridade('TEXTO.maiusculas', args, 1, ctx); exigirClasse('TEXTO.maiusculas', args[0], 'TEXTO', ctx); return TEXTO((args[0] as any).valor.toUpperCase()); },
  'TEXTO.minusculas': (args, ctx) => { exigirAridade('TEXTO.minusculas', args, 1, ctx); exigirClasse('TEXTO.minusculas', args[0], 'TEXTO', ctx); return TEXTO((args[0] as any).valor.toLowerCase()); },
  'TEXTO.aparar': (args, ctx) => { exigirAridade('TEXTO.aparar', args, 1, ctx); exigirClasse('TEXTO.aparar', args[0], 'TEXTO', ctx); return TEXTO((args[0] as any).valor.trim()); },
  'TEXTO.contem': (args, ctx) => { if (args.length !== 2) throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + "] TEXTO.contem exige 2 args"); exigirClasse('TEXTO.contem', args[0], 'TEXTO', ctx); exigirClasse('TEXTO.contem', args[1], 'TEXTO', ctx); return LOGICO((args[0] as any).valor.includes((args[1] as any).valor)); },
  'TEXTO.subtexto': (args, ctx) => {
    if (args.length < 2 || args.length > 3) throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + "] TEXTO.subtexto exige 2 ou 3 args (texto, inicio, [fim])");
    exigirClasse('TEXTO.subtexto', args[0], 'TEXTO', ctx);
    const ini = Number(comoInteiroArg(args[1], ctx));
    const fim = args[2] ? Number(comoInteiroArg(args[2], ctx)) : undefined;
    return TEXTO((args[0] as any).valor.slice(ini, fim));
  },
  'TEXTO.substituir': (args, ctx) => {
    exigirAridade('TEXTO.substituir', args, 3, ctx);
    exigirClasse('TEXTO.substituir', args[0], 'TEXTO', ctx); exigirClasse('TEXTO.substituir', args[1], 'TEXTO', ctx); exigirClasse('TEXTO.substituir', args[2], 'TEXTO', ctx);
    return TEXTO((args[0] as any).valor.split((args[1] as any).valor).join((args[2] as any).valor));
  },
  'TEXTO.dividir': (args, ctx) => {
    exigirAridade('TEXTO.dividir', args, 2, ctx);
    exigirClasse('TEXTO.dividir', args[0], 'TEXTO', ctx); exigirClasse('TEXTO.dividir', args[1], 'TEXTO', ctx);
    const partes: string[] = (args[0] as any).valor.split((args[1] as any).valor);
    return { classe: 'FATIA', tipoInterno: 'TEXTO', elementos: partes.map((p: string) => TEXTO(p)) };
  },
  'TEXTO.juntar': (args, ctx) => {
    exigirAridade('TEXTO.juntar', args, 2, ctx);
    if (args[0].classe !== 'FATIA') throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + "] TEXTO.juntar exige FATIA[TEXTO] como 1º arg");
    exigirClasse('TEXTO.juntar', args[1], 'TEXTO', ctx);
    const elems = (args[0] as { classe: 'FATIA'; elementos: ValorThz[] }).elementos;
    return TEXTO(elems.map((e) => e.classe === 'TEXTO' ? e.valor : '').join((args[1] as { classe: 'TEXTO'; valor: string }).valor));
  },

  'MATEMATICA.abs': (args, ctx) => {
    exigirAridade('MATEMATICA.abs', args, 1, ctx);
    const v = args[0];
    if (v.classe === 'INTEIRO') return INTEIRO(v.valor < 0n ? -v.valor : v.valor);
    if (v.classe === 'DECIMAL') return DECIMAL(v.valor.abs());
    throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] MATEMATICA.abs exige numérico');
  },
  'MATEMATICA.min': (args, ctx) => {
    exigirAridade('MATEMATICA.min', args, 2, ctx);
    const a = args[0]; const b = args[1];
    if (a.classe === 'INTEIRO' && b.classe === 'INTEIRO') return INTEIRO((a as any).valor < (b as any).valor ? a.valor : b.valor);
    throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] MATEMATICA.min exige dois INTEIROS');
  },
  'MATEMATICA.max': (args, ctx) => {
    exigirAridade('MATEMATICA.max', args, 2, ctx);
    const a = args[0]; const b = args[1];
    if (a.classe === 'INTEIRO' && b.classe === 'INTEIRO') return INTEIRO((a as any).valor > (b as any).valor ? a.valor : b.valor);
    throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] MATEMATICA.max exige dois INTEIROS');
  },
  'MATEMATICA.potencia': (args, ctx) => {
    exigirAridade('MATEMATICA.potencia', args, 2, ctx);
    const base = Number(comoInteiroArg(args[0], ctx));
    const exp = Number(comoInteiroArg(args[1], ctx));
    return INTEIRO(BigInt(Math.trunc(Math.pow(base, exp))));
  },
  'MATEMATICA.raiz': (args, ctx) => {
    exigirAridade('MATEMATICA.raiz', args, 1, ctx);
    const n = Number(comoInteiroArg(args[0], ctx));
    if (n < 0) throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] MATEMATICA.raiz exige não-negativo');
    return INTEIRO(BigInt(Math.trunc(Math.sqrt(n))));
  },
  'MATEMATICA.arredondar': (args, ctx) => {
    if (args.length !== 2) throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + "] MATEMATICA.arredondar exige 2 args");
    const d = args[0];
    if (d.classe !== 'DECIMAL') throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] MATEMATICA.arredondar exige DECIMAL');
    const casas = Number(comoInteiroArg(args[1], ctx));
    return DECIMAL(d.valor.paraEscala(casas));
  },
  'MATEMATICA.aleatorio': (args, ctx) => {
    if (args.length !== 1) throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + "] MATEMATICA.aleatorio exige 1 arg (limite)");
    const lim = Number(comoInteiroArg(args[0], ctx));
    return INTEIRO(BigInt(Math.floor(Math.random() * lim)));
  },

  'DATA.hoje': (args, ctx) => {
    exigirAridade('DATA.hoje', args, 0, ctx);
    const agora = new Date();
    return DATA(DataThz.deComponentes(agora.getFullYear(), agora.getMonth() + 1, agora.getDate()));
  },
  'DATA.agora': (args, ctx) => {
    exigirAridade('DATA.agora', args, 0, ctx);
    const agora = new Date();
    return DATA_HORA(DataHoraThz.deComponentes(agora.getFullYear(), agora.getMonth() + 1, agora.getDate(), agora.getHours(), agora.getMinutes(), agora.getSeconds()));
  },
  'DATA.criar': (args, ctx) => {
    exigirAridade('DATA.criar', args, 3, ctx);
    const a = Number(comoInteiroArg(args[0], ctx)); const m = Number(comoInteiroArg(args[1], ctx)); const d = Number(comoInteiroArg(args[2], ctx));
    try { return DATA(DataThz.deComponentes(a, m, d)); } catch (e) { throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] ' + (e as Error).message); }
  },
  'DATA.criarDataHora': (args, ctx) => {
    if (args.length < 5 || args.length > 6) throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + "] DATA.criarDataHora exige 5 ou 6 args");
    const [a, m, d, h, mi] = args.slice(0, 5).map((v) => Number(comoInteiroArg(v, ctx)));
    const s = args[5] ? Number(comoInteiroArg(args[5], ctx)) : 0;
    try { return DATA_HORA(DataHoraThz.deComponentes(a, m, d, h, mi, s)); } catch (e) { throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] ' + (e as Error).message); }
  },
  'DATA.adicionarDias': (args, ctx) => {
    exigirAridade('DATA.adicionarDias', args, 2, ctx);
    if (args[0].classe !== 'DATA') throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] DATA.adicionarDias exige DATA');
    const dias = comoInteiroArg(args[1], ctx);
    return DATA(( (args[0] as any).valor as DataThz).adicionarDias(dias));
  },
  'DATA.adicionarHoras': (args, ctx) => {
    exigirAridade('DATA.adicionarHoras', args, 2, ctx);
    if (args[0].classe !== 'DATA_HORA') throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] DATA.adicionarHoras exige DATA_HORA');
    const h = comoInteiroArg(args[1], ctx);
    return DATA_HORA(( (args[0] as any).valor as DataHoraThz).adicionarHoras(h));
  },
  'DATA.diferencaDias': (args, ctx) => {
    exigirAridade('DATA.diferencaDias', args, 2, ctx);
    if (args[0].classe !== 'DATA' || args[1].classe !== 'DATA') throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] DATA.diferencaDias exige duas DATA');
    return INTEIRO(( (args[0] as any).valor as DataThz).diferencaDias((args[1] as any).valor as DataThz));
  },
  'DATA.ano': (args, ctx) => { exigirAridade('DATA.ano', args, 1, ctx); if (args[0].classe === 'DATA') return INTEIRO(BigInt(( (args[0] as any).valor as DataThz).ano)); if (args[0].classe === 'DATA_HORA') return INTEIRO(BigInt(( (args[0] as any).valor as DataHoraThz).data.ano)); throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] DATA.ano exige DATA ou DATA_HORA'); },
  'DATA.mes': (args, ctx) => { exigirAridade('DATA.mes', args, 1, ctx); if (args[0].classe === 'DATA') return INTEIRO(BigInt(( (args[0] as any).valor as DataThz).mes)); if (args[0].classe === 'DATA_HORA') return INTEIRO(BigInt(( (args[0] as any).valor as DataHoraThz).data.mes)); throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] DATA.mes exige DATA ou DATA_HORA'); },
  'DATA.dia': (args, ctx) => { exigirAridade('DATA.dia', args, 1, ctx); if (args[0].classe === 'DATA') return INTEIRO(BigInt(( (args[0] as any).valor as DataThz).dia)); if (args[0].classe === 'DATA_HORA') return INTEIRO(BigInt(( (args[0] as any).valor as DataHoraThz).data.dia)); throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] DATA.dia exige DATA ou DATA_HORA'); },
  'DATA.diaDaSemana': (args, ctx) => { exigirAridade('DATA.diaDaSemana', args, 1, ctx); if (args[0].classe !== 'DATA') throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] DATA.diaDaSemana exige DATA'); return INTEIRO(BigInt(( (args[0] as any).valor as DataThz).diaDaSemana())); },
  'DATA.texto': (args, ctx) => { exigirAridade('DATA.texto', args, 1, ctx); if (args[0].classe === 'DATA') return TEXTO(( (args[0] as any).valor as DataThz).formatar()); if (args[0].classe === 'DATA_HORA') return TEXTO(( (args[0] as any).valor as DataHoraThz).formatar()); throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] DATA.texto exige DATA ou DATA_HORA'); },
  'TELA.renderizarFormulario': (args, ctx) => {
    exigirAridade('TELA.renderizarFormulario', args, 2, ctx);
    if (args[0].classe !== 'REGISTRO') throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] TELA.renderizarFormulario exige REGISTRO como 1º arg');
    const reg = args[0] as any;
    const campoTitulo = reg.campos instanceof Map ? reg.campos.get('titulo') : reg.campos?.titulo;
    const titulo = (campoTitulo && campoTitulo.classe === 'TEXTO') ? campoTitulo.valor : reg.nomeEstrutura;
    return TEXTO("Formulário '" + titulo + "' renderizado.");
  },
  'TELA.alerta': (args, ctx) => {
    exigirAridade('TELA.alerta', args, 2, ctx);
    exigirClasse('TELA.alerta', args[0], 'TEXTO', ctx);
    exigirClasse('TELA.alerta', args[1], 'TEXTO', ctx);
    return TEXTO('OK');
  },
  'TELA.confirmar': (args, ctx) => {
    exigirAridade('TELA.confirmar', args, 2, ctx);
    exigirClasse('TELA.confirmar', args[0], 'TEXTO', ctx);
    exigirClasse('TELA.confirmar', args[1], 'TEXTO', ctx);
    return LOGICO(true);
  },
  'TELA.pedirTexto': (args, ctx) => {
    exigirAridade('TELA.pedirTexto', args, 2, ctx);
    exigirClasse('TELA.pedirTexto', args[0], 'TEXTO', ctx);
    exigirClasse('TELA.pedirTexto', args[1], 'TEXTO', ctx);
    return TEXTO('');
  },
};

export function ehStdlib(nome: string): boolean { return nome in STDLIB; }

/* ============================================================
 * Interpretador Tree-Walking
 * ============================================================ */

export interface OpcoesInterpretador {
  /** Destino de saída do EXIBA (padrão: console.log). */
  saida?: (linha: string) => void;
  /** Guarda anti-loop-infinito para ENQUANTO/PARA. */
  maxIteracoes?: number;
  /** Provedor de entrada para LER (padrão: lança erro). */
  entrada?: () => string | null;
}

interface OperacaoResolvida {
  regra: RegraNegocioAST;
  operacao: OperacaoAST;
}

const LIMITE_PADRAO_ITERACOES = 10_000_000;

function ehValorResultado(v: ValorThz | undefined): boolean {
  return v !== undefined && v.classe === 'RESULTADO';
}

export class InterpretadorThz {
  private emitir: (linha: string) => void;
  private maxIteracoes: number;
  private lerEntrada: () => string | null;

  constructor(private ast: ProgramaAST, opcoes: OpcoesInterpretador = {}) {
    this.emitir = opcoes.saida ?? ((l: string) => console.log(l));
    this.maxIteracoes = opcoes.maxIteracoes ?? LIMITE_PADRAO_ITERACOES;
    this.lerEntrada = opcoes.entrada ?? (() => { throw new ErroExecucao('[Erro de Execução] LER exige provedor de entrada (use --arg ou modo interativo).'); });
  }

  /* ---------------- Resolução de operações ---------------- */

  public listarOperacoesExecutaveis(): OperacaoResolvida[] {
    const encontradas: OperacaoResolvida[] = [];
    for (const regra of this.ast.regras) {
      for (const operacao of regra.operacoes) {
        if (operacao.corpo.length > 0) encontradas.push({ regra, operacao });
      }
    }
    return encontradas;
  }

  public listarProcedimentos(): ProcedimentoAST[] {
    return this.ast.procedimentos ?? [];
  }

  /**
   * Executa uma operação com contratos formais:
   * EXIGE na entrada (quantificador universal implícito sobre fatias),
   * GARANTE na saída, contra o estado pós-execução.
   */
  public executarOperacao(nomeOperacao: string, argumentos: Record<string, ValorThz>): ValorThz | undefined {
    const alvo = this.listarOperacoesExecutaveis().find((o) => o.operacao.nome === nomeOperacao);
    if (!alvo) {
      throw new ErroExecucao("[Erro de Execução] Operação '" + nomeOperacao + "' não encontrada ou sem corpo executável.");
    }
    const retornoResultado = alvo.operacao.tipoRetorno.startsWith('RESULTADO');

    const escopoGlobal = new Escopo();
    for (const [nome, valor] of Object.entries(argumentos)) {
      escopoGlobal.definir(nome, valor);
    }

    this.validarContratos(alvo.regra.clausulasEntrada, escopoGlobal, 'EXIGE');

    let retorno: ValorThz | undefined;
    try {
      this.executarComandos(alvo.operacao.corpo, escopoGlobal);
    } catch (sinal) {
      if (sinal instanceof SinalFalharCom) {
        if (!retornoResultado) {
          throw new ErroExecucao(
            '[Erro de Execução] FALHAR_COM exige operação com retorno RESULTADO[T,E]; ' +
            "operacao '" + nomeOperacao + "' declara '" + alvo.operacao.tipoRetorno + "'."
          );
        }
        // Canal de erro: GARANTE avalia apenas o caminho de sucesso.
        return { classe: 'RESULTADO', sucesso: false, erro: sinal.valor };
      }
      if (!(sinal instanceof SinalRetorne)) throw sinal;
      retorno = sinal.valor;
    }

    this.validarContratos(alvo.regra.clausulasSaida, escopoGlobal, 'GARANTE');
    if (retornoResultado && !ehValorResultado(retorno)) {
      return { classe: 'RESULTADO', sucesso: true, valor: retorno };
    }
    return retorno;
  }

  public executarProcedimento(nome: string, argumentos: Record<string, ValorThz> = {}): void {
    const proc = (this.ast.procedimentos ?? []).find((p) => p.nome === nome);
    if (!proc) throw new ErroExecucao("[Erro de Execução] Procedimento '" + nome + "' não encontrado.");
    const escopo = new Escopo();
    for (const p of proc.parametros) {
      const v = argumentos[p.nome];
      if (v === undefined) throw new ErroExecucao("[Erro de Execução] Parâmetro '" + p.nome + "' não fornecido para procedimento '" + nome + "'.");
      escopo.definir(p.nome, v);
    }
    try {
      this.executarComandos(proc.corpo, escopo);
    } catch (sinal) {
      if (sinal instanceof SinalFalharCom) {
        throw new ErroExecucao('[Erro de Execução] FALHAR_COM não permitido dentro de PROCEDIMENTO (sem canal RESULTADO).');
      }
      if (sinal instanceof SinalRetorne) {
        if (sinal.valor !== undefined) {
          throw new ErroExecucao('[Erro de Execução] RETORNE com valor não permitido dentro de PROCEDIMENTO; use RETORNE sem valor.');
        }
        return;
      }
      throw sinal;
    }
  }

  /* ---------------- Contratos formais ---------------- */

  private validarContratos(clausulas: ClausulaContratoAST[], escopo: Escopo, natureza: 'EXIGE' | 'GARANTE'): void {
    for (const clausula of clausulas) {
      if (!this.avaliarClausulaUniversal(clausula.expressao, escopo)) {
        throw new ErroExecucao(
          '[Violação de Contrato ' + natureza + '][Linha ' + clausula.linha + ':' + clausula.coluna + '] Cláusula reprovada: ' + clausula.textoCanonico
        );
      }
    }
  }

  /**
   * Identificadores enraizados em FATIA recebem quantificador universal:
   * a cláusula vale para TODO elemento (fatia vazia é verdade vacuosa).
   */
  private avaliarClausulaUniversal(expr: ExprAST, escopo: Escopo): boolean {
    const raizes = this.coletarRaizesDeFatias(expr, escopo);
    return this.quantificar(expr, escopo, raizes, 0);
  }

  private coletarRaizesDeFatias(expr: ExprAST, escopo: Escopo): Set<string> {
    const raizes = new Set<string>();
    const visitar = (e: ExprAST): void => {
      switch (e.tipo) {
        case 'ACESSO': {
          const base = escopo.resolver(e.caminho[0]);
          if (base?.classe === 'FATIA') raizes.add(e.caminho[0]);
          break;
        }
        case 'CHAMADA':
          e.argumentos.forEach(visitar);
          break;
        case 'INDEXACAO':
          visitar(e.alvo); visitar(e.indice);
          break;
        case 'FATIA_LITERAL':
          e.elementos.forEach(visitar);
          break;
        case 'CRIAR_REGISTRO':
          e.campos.forEach((c) => visitar(c.valor));
          break;
        case 'OP_BINARIA':
          visitar(e.esquerda);
          visitar(e.direita);
          break;
        case 'OP_UNARIA':
          visitar(e.operando);
          break;
        default:
          break;
      }
    };
    visitar(expr);
    return raizes;
  }

  private quantificar(expr: ExprAST, escopo: Escopo, raizes: Set<string>, indice: number): boolean {
    if (indice >= raizes.size) {
      return this.exigirLogico(this.avaliar(expr, escopo), 'cláusula de contrato');
    }
    const nome = Array.from(raizes)[indice];
    const fatia = escopo.resolver(nome);
    if (!fatia || fatia.classe !== 'FATIA') return this.quantificar(expr, escopo, raizes, indice + 1);
    if (fatia.elementos.length === 0) return true;
    for (const elemento of fatia.elementos) {
      const sombra = new Escopo(escopo);
      sombra.definir(nome, elemento);
      if (!this.quantificar(expr, sombra, raizes, indice + 1)) return false;
    }
    return true;
  }

  /* ---------------- Avaliação de expressões ---------------- */

  private avaliar(expr: ExprAST, escopo: Escopo): ValorThz {
    switch (expr.tipo) {
      case 'LITERAL_INTEIRO':
        return INTEIRO(expr.valor);

      case 'LITERAL_DECIMAL':
        return DECIMAL(new DecimalFixo(expr.escalado, expr.escala));

      case 'LITERAL_TEXTO':
        return TEXTO(expr.valor);

      case 'LITERAL_LOGICO':
        return LOGICO(expr.valor);

      case 'NULO':
        return NULO_THZ;

      case 'FATIA_LITERAL': {
        const elementos = expr.elementos.map((e) => this.avaliar(e, escopo));
        let tipoInterno = 'TEXTO';
        if (elementos.length > 0) {
          const primeiro = elementos[0];
          if (primeiro.classe === 'REGISTRO') tipoInterno = primeiro.nomeEstrutura;
          else if (primeiro.classe === 'INTEIRO') tipoInterno = 'INTEIRO';
          else if (primeiro.classe === 'DECIMAL') tipoInterno = 'DECIMAL';
          else if (primeiro.classe === 'TEXTO') tipoInterno = 'TEXTO';
          else if (primeiro.classe === 'DATA') tipoInterno = 'DATA';
          else if (primeiro.classe === 'DATA_HORA') tipoInterno = 'DATA_HORA';
          else tipoInterno = primeiro.classe;
        }
        return { classe: 'FATIA', tipoInterno, elementos };
      }

      case 'CRIAR_REGISTRO': {
        const estrutura = this.ast.estruturas.find((e) => e.nome === expr.nomeEstrutura);
        if (!estrutura) throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + "] Estrutura '" + expr.nomeEstrutura + "' não declarada.");
        // exige todos os campos
        if (expr.campos.length !== estrutura.campos.length) {
          throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + "] CRIAR '" + expr.nomeEstrutura + "' exige " + estrutura.campos.length + ' campos, recebidos ' + expr.campos.length + '.');
        }
        const campos = new Map<string, ValorThz>();
        for (const campo of estrutura.campos) {
          const fornecido = expr.campos.find((c) => c.nome === campo.nome);
          if (!fornecido) throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + "] Campo '" + campo.nome + "' não fornecido em CRIAR '" + expr.nomeEstrutura + "'.");
          campos.set(campo.nome, this.avaliar(fornecido.valor, escopo));
        }
        const registro: ValorThz = { classe: 'REGISTRO', nomeEstrutura: expr.nomeEstrutura, campos };
        this.validarInvariantes(registro, expr as unknown as ComandoAST);
        return registro;
      }

      case 'INDEXACAO': {
        const alvo = this.avaliar(expr.alvo, escopo);
        const indice = this.avaliar(expr.indice, escopo);
        if (indice.classe !== 'INTEIRO') throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + '] Índice deve ser INTEIRO, recebido ' + indice.classe);
        const i = Number(indice.valor);
        if (!Number.isSafeInteger(i) || i < 0) throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + "] Índice fora de limites: '" + indice.valor + "'.");
        if (alvo.classe === 'FATIA') {
          if (i >= alvo.elementos.length) throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + "] Índice " + i + ' fora da fatia (tamanho ' + alvo.elementos.length + ').');
          return alvo.elementos[i];
        }
        if (alvo.classe === 'TEXTO') {
          if (i >= alvo.valor.length) throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + "] Índice " + i + ' fora do texto (tamanho ' + alvo.valor.length + ').');
          return TEXTO(alvo.valor[i]);
        }
        throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + '] Indexação exige FATIA ou TEXTO, recebido ' + alvo.classe);
      }

      case 'CHAMADA': {
        const nomeQualificado = expr.caminho.join('.');
        const args = expr.argumentos.map((a) => this.avaliar(a, escopo));
        // stdlib primeiro
        const fn = STDLIB[nomeQualificado];
        if (fn) {
          try { return fn(args, expr); } catch (e) { if (e instanceof ErroExecucao) throw e; throw new ErroExecucao((e as Error).message); }
        }
        // procedimento?
        const proc = (this.ast.procedimentos ?? []).find((p) => p.nome === expr.caminho[0] && expr.caminho.length === 1);
        if (proc) {
          if (args.length !== proc.parametros.length) throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + "] Procedimento '" + proc.nome + "' exige " + proc.parametros.length + ' arg(s), recebidos ' + args.length + '.');
          const escopoChamada = new Escopo();
          proc.parametros.forEach((p, i) => escopoChamada.definir(p.nome, args[i]));
          let ret: ValorThz | undefined;
          try {
            this.executarComandos(proc.corpo, escopoChamada);
          } catch (s) {
            if (s instanceof SinalRetorne) { if (s.valor !== undefined) throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + '] RETORNE com valor não permitido em PROCEDIMENTO.'); return NULO_THZ; }
            if (s instanceof SinalFalharCom) throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + '] FALHAR_COM não permitido em PROCEDIMENTO.');
            throw s;
          }
          return ret ?? NULO_THZ;
        }
        // operação de regra por nome
        for (const regra of this.ast.regras) {
          const op = regra.operacoes.find((o) => o.nome === expr.caminho[0] && expr.caminho.length === 1);
          if (op) {
            if (args.length !== op.parametros.length) throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + "] Operação '" + op.nome + "' exige " + op.parametros.length + ' arg(s), recebidos ' + args.length + '.');
            const mapa: Record<string, ValorThz> = {};
            op.parametros.forEach((p, i) => mapa[p.nome] = args[i]);
            const ret = this.executarOperacao(op.nome, mapa);
            return ret ?? NULO_THZ;
          }
        }
        throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + "] Chamada desconhecida: '" + nomeQualificado + "'.");
      }

      case 'ACESSO': {
        const base = escopo.resolver(expr.caminho[0]);
        if (base === undefined) {
          // Membros de ENUMERACAO são identificadores globais.
          if (expr.caminho.length === 1) {
            const dono = this.ast.enumeracoes.find((e) => e.membros.includes(expr.caminho[0]));
            if (dono) return ENUMERADO(dono.nome, expr.caminho[0]);
          }
          throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + "] Identificador não declarado: '" + expr.caminho[0] + "'.");
        }
        let atual: ValorThz = base;
        for (let i = 1; i < expr.caminho.length; i++) {
          const campo = expr.caminho[i];
          if (atual.classe !== 'REGISTRO') {
            throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + "] Acesso a campo '" + campo + "' em valor que não é registro.");
          }
          const proximo = atual.campos.get(campo);
          if (proximo === undefined) {
            throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + "] Campo '" + campo + "' inexistente em '" + atual.nomeEstrutura + "'.");
          }
          atual = proximo;
        }
        return atual;
      }

      case 'OP_UNARIA': {
        const operando = this.avaliar(expr.operando, escopo);
        if (expr.operador === 'NAO') {
          return LOGICO(!this.exigirLogico(operando, "operando do conectivo 'NAO'"));
        }
        if (operando.classe === 'INTEIRO') return INTEIRO(-operando.valor);
        if (operando.classe === 'DECIMAL') return DECIMAL(operando.valor.negar());
        throw new ErroExecucao('[Erro de Execução][Linha ' + expr.linha + ':' + expr.coluna + '] Negação aritmética exige valor numérico.');
      }

      case 'OP_BINARIA': {
        if (expr.operador === 'E') {
          const esq = this.avaliar(expr.esquerda, escopo);
          if (!this.exigirLogico(esq, "conectivo 'E'")) return LOGICO(false);
          return LOGICO(this.exigirLogico(this.avaliar(expr.direita, escopo), "conectivo 'E'"));
        }
        if (expr.operador === 'OU') {
          const esq = this.avaliar(expr.esquerda, escopo);
          if (this.exigirLogico(esq, "conectivo 'OU'")) return LOGICO(true);
          return LOGICO(this.exigirLogico(this.avaliar(expr.direita, escopo), "conectivo 'OU'"));
        }

        const esquerda = this.avaliar(expr.esquerda, escopo);
        const direita = this.avaliar(expr.direita, escopo);

        if (['=', '<>', '<', '<=', '>', '>='].includes(expr.operador)) {
          return LOGICO(this.comparar(esquerda, direita, expr.operador, expr));
        }
        return this.aritmetica(esquerda, direita, expr.operador, expr);
      }
    }
  }

  private exigirLogico(v: ValorThz, contexto: string): boolean {
    if (v.classe !== 'LOGICO') {
      throw new ErroExecucao('[Erro de Execução] Esperado valor lógico em ' + contexto + '.');
    }
    return v.valor;
  }

  private comparar(a: ValorThz, b: ValorThz, operador: string, ctx: ExprAST): boolean {
    const ordemNumerica = (x: ValorThz, y: ValorThz): number | null => {
      if (x.classe === 'INTEIRO' && y.classe === 'INTEIRO') return x.valor === y.valor ? 0 : x.valor < y.valor ? -1 : 1;
      try {
        const dx = this.comoDecimal(x, ctx);
        const dy = this.comoDecimal(y, ctx);
        return dx.valorEscalado === dy.valorEscalado ? 0 : dx.valorEscalado < dy.valorEscalado ? -1 : 1;
      } catch { return null; }
    };

    if (a.classe === 'TEXTO' && b.classe === 'TEXTO') {
      switch (operador) {
        case '=': return a.valor === b.valor;
        case '<>': return a.valor !== b.valor;
        case '<': return (a as any).valor < (b as any).valor;
        case '<=': return a.valor <= b.valor;
        case '>': return (a as any).valor > (b as any).valor;
        case '>=': return a.valor >= b.valor;
      }
    }

    if (a.classe === 'LOGICO' && b.classe === 'LOGICO') {
      if (operador === '=') return a.valor === b.valor;
      if (operador === '<>') return a.valor !== b.valor;
    }

    if (a.classe === 'MONETARIO' && b.classe === 'MONETARIO') {
      const c = (a.valor as Monetario).comparar(b.valor as Monetario);
      switch (operador) {
        case '=': return c === 0;
        case '<>': return c !== 0;
        case '<': return c < 0;
        case '<=': return c <= 0;
        case '>': return c > 0;
        case '>=': return c >= 0;
      }
    }

    if (a.classe === 'DATA' && b.classe === 'DATA') {
      const c = (a.valor as DataThz).comparar(b.valor as DataThz);
      switch (operador) {
        case '=': return c === 0;
        case '<>': return c !== 0;
        case '<': return c < 0;
        case '<=': return c <= 0;
        case '>': return c > 0;
        case '>=': return c >= 0;
      }
    }
    if (a.classe === 'DATA_HORA' && b.classe === 'DATA_HORA') {
      const c = (a.valor as DataHoraThz).comparar(b.valor as DataHoraThz);
      switch (operador) {
        case '=': return c === 0;
        case '<>': return c !== 0;
        case '<': return c < 0;
        case '<=': return c <= 0;
        case '>': return c > 0;
        case '>=': return c >= 0;
      }
    }

    if (this.ehNumerico(a) && this.ehNumerico(b)) {
      const c = ordemNumerica(a, b);
      if (c !== null) {
        switch (operador) {
          case '=': return c === 0;
          case '<>': return c !== 0;
          case '<': return c < 0;
          case '<=': return c <= 0;
          case '>': return c > 0;
          case '>=': return c >= 0;
        }
      }
    }

    if (a.classe === 'ENUMERADO' && b.classe === 'ENUMERADO') {
      const mesmaEnum = a.nomeEnumeracao === b.nomeEnumeracao;
      switch (operador) {
        case '=': return mesmaEnum && a.valor === b.valor;
        case '<>': return !(mesmaEnum && a.valor === b.valor);
      }
      throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] ENUMERACAO suporta apenas os operadores = e <>.');
    }

    throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] Comparação entre tipos incompatíveis (' + a.classe + ' ' + operador + ' ' + b.classe + ').');
  }

  private ehNumerico(v: ValorThz): boolean {
    return v.classe === 'INTEIRO' || v.classe === 'DECIMAL';
  }

  private comoDecimal(v: ValorThz, ctx: ExprAST): DecimalFixo {
    if (v.classe === 'DECIMAL') return v.valor;
    if (v.classe === 'INTEIRO') return DecimalFixo.deInteiro(v.valor);
    throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] Esperado valor numérico, recebido ' + v.classe + '.');
  }

  private aritmetica(a: ValorThz, b: ValorThz, operador: string, ctx: ExprAST): ValorThz {
    // Concatenação textual: qualquer valor + TEXTO formata à direita.
    if (operador === '+' && (a.classe === 'TEXTO' || b.classe === 'TEXTO')) {
      return TEXTO(this.formatar(a) + this.formatar(b));
    }

    if (a.classe === 'INTEIRO' && b.classe === 'INTEIRO') {
      switch (operador) {
        case '+': return INTEIRO(a.valor + b.valor);
        case '-': return INTEIRO(a.valor - b.valor);
        case '*': return INTEIRO(a.valor * b.valor);
        case '/':
          if (b.valor === 0n) throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] Divisão por zero.');
          return INTEIRO(a.valor / b.valor);
        case '%':
          if (b.valor === 0n) throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] Módulo por zero.');
          return INTEIRO(a.valor % b.valor);
      }
    }

    if (this.ehNumerico(a) && this.ehNumerico(b)) {
      const da = this.comoDecimal(a, ctx);
      const db = this.comoDecimal(b, ctx);
      switch (operador) {
        case '+': return DECIMAL(da.somar(db));
        case '-': return DECIMAL(da.subtrair(db));
        case '*': return DECIMAL(da.multiplicar(db));
        case '/':
          if (db.valorEscalado === 0n) throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] Divisão por zero.');
          return DECIMAL(da.dividir(db));
        case '%':
          throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + "] Operador '%' suportado apenas entre inteiros.");
      }
    }

    if (a.classe === 'MONETARIO' && b.classe === 'MONETARIO') {
      const ma = a.valor as Monetario;
      const mb = b.valor as Monetario;
      switch (operador) {
        case '+': return MONETARIO(ma.somar(mb));
        case '-': return MONETARIO(ma.subtrair(mb));
      }
    }

    if (a.classe === 'MONETARIO' && b.classe === 'DECIMAL') {
      if (operador === '*') return MONETARIO((a.valor as Monetario).multiplicar(b.valor));
      throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] Monetário só admite multiplicação por fator decimal.');
    }

    if (a.classe === 'DECIMAL' && b.classe === 'MONETARIO' && operador === '*') {
      return MONETARIO((b.valor as Monetario).multiplicar(a.valor));
    }

    throw new ErroExecucao('[Erro de Execução][Linha ' + ctx.linha + ':' + ctx.coluna + '] Operação ' + operador + ' inválida entre ' + a.classe + ' e ' + b.classe + '.');
  }

  public formatar(v: ValorThz): string {
    switch (v.classe) {
      case 'INTEIRO': return v.valor.toString();
      case 'DECIMAL': return v.valor.formatar();
      case 'MONETARIO': return v.valor.formatar();
      case 'TEXTO': return v.valor;
      case 'LOGICO': return v.valor ? 'VERDADEIRO' : 'FALSO';
      case 'NULO': return 'NULO';
      case 'DATA': return v.valor.formatar();
      case 'DATA_HORA': return v.valor.formatar();
      case 'ENUMERADO': return v.valor;
      case 'RESULTADO': return v.sucesso
        ? 'SUCESSO(' + (v.valor ? this.formatar(v.valor) : 'NULO') + ')'
        : 'FALHA(' + (v.erro ? this.formatar(v.erro) : 'NULO') + ')';
      case 'REGISTRO': return v.nomeEstrutura + '{...}';
      case 'FATIA': return 'FATIA[' + v.tipoInterno + '](' + v.elementos.length + ')';
    }
  }

  /* ---------------- Execução de comandos ---------------- */

  private executarComandos(comandos: ComandoAST[], escopo: Escopo): void {
    for (const comando of comandos) {
      this.executarComando(comando, escopo);
    }
  }

  private executarComando(cmd: ComandoAST, escopo: Escopo): void {
    switch (cmd.tipoComando) {
      case 'DECL_VARIAVEL': {
        if (escopo.resolver(cmd.nome) !== undefined) {
          throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] Variável '" + cmd.nome + "' já declarada neste escopo.");
        }
        escopo.definir(cmd.nome, this.avaliar(cmd.inicializacao, escopo));
        return;
      }

      case 'ATRIBUICAO': {
        const base = escopo.resolver(cmd.alvo[0]);
        if (base === undefined) {
          throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] Atribuição a identificador não declarado: '" + cmd.alvo[0] + "'.");
        }
        const valor = this.avaliar(cmd.expressao, escopo);
        if (cmd.alvo.length === 1) {
          if (!escopo.atualizar(cmd.alvo[0], valor)) {
            throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] Atribuição a identificador não declarado: '" + cmd.alvo[0] + "'.");
          }
          this.validarInvariantes(valor, cmd);
          return;
        }
        this.atribuirCampo(base, cmd.alvo.slice(1), valor, cmd);
        this.validarInvariantes(base, cmd);
        return;
      }

      case 'SE': {
        if (this.exigirLogico(this.avaliar(cmd.condicao, escopo), "condição do 'SE'")) {
          this.executarComandos(cmd.entao, new Escopo(escopo));
        } else {
          this.executarComandos(cmd.senao, new Escopo(escopo));
        }
        return;
      }

      case 'ENQUANTO': {
        let iteracoes = 0;
        while (this.exigirLogico(this.avaliar(cmd.condicao, escopo), "condição do 'ENQUANTO'")) {
          if (++iteracoes > this.maxIteracoes) {
            throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] Laço 'ENQUANTO' excedeu " + this.maxIteracoes + ' iterações (guarda anti-loop).');
          }
          this.executarComandos(cmd.corpo, new Escopo(escopo));
        }
        return;
      }

      case 'PARA': {
        const inicio = this.avaliar(cmd.inicio, escopo);
        const fim = this.avaliar(cmd.fim, escopo);
        if (inicio.classe !== 'INTEIRO') throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] 'PARA' exige início INTEIRO, recebido " + inicio.classe);
        if (fim.classe !== 'INTEIRO') throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] 'PARA' exige fim INTEIRO, recebido " + fim.classe);
        let passo = 1n;
        if (cmd.passo) {
          const v = this.avaliar(cmd.passo, escopo);
          if (v.classe !== 'INTEIRO') throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] 'PASSO' exige INTEIRO");
          passo = v.valor;
          if (passo === 0n) throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] 'PASSO' não pode ser zero.");
        }
        let iter = 0;
        for (let cur = inicio.valor; passo > 0n ? cur <= fim.valor : cur >= fim.valor; cur += passo) {
          if (++iter > this.maxIteracoes) throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] Laço 'PARA' excedeu " + this.maxIteracoes + ' iterações (guarda anti-loop).');
          const esc = new Escopo(escopo);
          esc.definir(cmd.variavel, INTEIRO(cur));
          this.executarComandos(cmd.corpo, esc);
        }
        return;
      }

      case 'VETORIZAR_PARA': {
        const fonte = escopo.resolver(cmd.fonte[0]);
        if (!fonte || fonte.classe !== 'FATIA') {
          throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] Fonte do 'VETORIZAR_PARA' deve ser uma fatia: " + cmd.fonte.join('.'));
        }
        for (const elemento of fonte.elementos) {
          const escopoIteracao = new Escopo(escopo);
          escopoIteracao.definir(cmd.variavel, elemento);
          this.executarComandos(cmd.corpo, escopoIteracao);
        }
        return;
      }

      case 'BLOCO_MEMORIA': {
        // Arena efêmera: todo consumo do bloco é descartável em O(1).
        const arena = new ArenaMemoria(1);
        arena.alocar(1024);
        try {
          this.executarComandos(cmd.corpo, new Escopo(escopo));
        } finally {
          arena.liberarTudo();
        }
        return;
      }

      case 'EXIBA': {
        this.emitir(this.formatar(this.avaliar(cmd.expressao, escopo)));
        return;
      }

      case 'LER': {
        const linha = this.lerEntrada();
        if (linha === null || linha === undefined) throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + '] Entrada encerrada (EOF) em LER ' + cmd.alvo.join('.'));
        const base = escopo.resolver(cmd.alvo[0]);
        if (base === undefined) throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] LER alvo não declarado: '" + cmd.alvo[0] + "'.");
        // Inferir tipo do alvo pelo valor atual: atualizar com coerção textual
        let novo: ValorThz;
        if (base.classe === 'INTEIRO') {
          const s = linha.trim();
          if (!/^-?\d+$/.test(s)) throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] LER INTEIRO exige dígitos: '" + linha + "'");
          novo = INTEIRO(BigInt(s));
        } else if (base.classe === 'DECIMAL') {
          try { novo = DECIMAL(DecimalFixo.deTexto(linha.trim(), base.valor.escala)); } catch (e) { throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + '] LER DECIMAL inválido: ' + (e as Error).message); }
        } else if (base.classe === 'DATA') {
          try { novo = DATA(DataThz.deTexto(linha.trim())); } catch (e) { throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + '] LER DATA inválido: ' + (e as Error).message); }
        } else if (base.classe === 'DATA_HORA') {
          try { novo = DATA_HORA(DataHoraThz.deTexto(linha.trim())); } catch (e) { throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + '] LER DATA_HORA inválido: ' + (e as Error).message); }
        } else if (base.classe === 'TEXTO') {
          novo = TEXTO(linha);
        } else {
          // LOGICO, etc: tentar heurística
          const t = linha.trim().toUpperCase();
          if (base.classe === 'LOGICO') novo = LOGICO(t === 'VERDADEIRO' || t === 'TRUE' || t === '1');
          else novo = TEXTO(linha);
        }
        if (cmd.alvo.length === 1) {
          if (!escopo.atualizar(cmd.alvo[0], novo)) throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] LER alvo não declarado: '" + cmd.alvo[0] + "'.");
        } else {
          // campo de registro
          this.atribuirCampo(base, cmd.alvo.slice(1), novo, cmd);
        }
        return;
      }

      case 'CHAMADA': {
        this.avaliar(cmd.expressao, escopo);
        return;
      }

      case 'RETORNE': {
        throw new SinalRetorne(cmd.expressao ? this.avaliar(cmd.expressao, escopo) : undefined);
      }

      case 'FALHAR_COM': {
        throw new SinalFalharCom(this.avaliar(cmd.expressao, escopo));
      }
    }
  }

  /* ---------------- Invariantes de estrutura (Arquitetura Viva) ---------------- */

  /**
   * Valida os INVARIANTE declarados na ESTRUTURA do registro informado.
   * Chamado após toda mutação de campos e na construção (fixtures/CLI).
   */
  public validarInvariantes(valor: ValorThz, cmd?: ComandoAST): void {
    if (valor.classe !== 'REGISTRO') return;
    const estrutura = this.ast.estruturas.find((e) => e.nome === valor.nomeEstrutura);
    if (!estrutura || estrutura.invariantes.length === 0) return;
    const escopo = new Escopo();
    for (const [nome, campo] of valor.campos) escopo.definir(nome, campo);
    for (const invariante of estrutura.invariantes) {
      const resultado = this.avaliar(invariante.expressao, escopo);
      if (resultado.classe !== 'LOGICO' || !resultado.valor) {
        const posicao = cmd ? '[Linha ' + cmd.linha + ':' + cmd.coluna + '] ' : '[Linha ' + invariante.linha + ':' + invariante.coluna + '] ';
        throw new ErroExecucao(
          '[Violação de Invariante]' + posicao + "Estrutura '" + valor.nomeEstrutura + "' reprovou: " + invariante.textoCanonico
        );
      }
    }
  }

  private atribuirCampo(alvo: ValorThz, caminhoRestante: string[], valor: ValorThz, cmd: ComandoAST): void {
    if (caminhoRestante.length === 0) {
      throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + '] Caminho de atribuição malformado.');
    }
    const campoFinal = caminhoRestante[caminhoRestante.length - 1];
    let container: ValorThz = alvo;
    for (let i = 0; i < caminhoRestante.length - 1; i++) {
      if (container.classe !== 'REGISTRO') {
        throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] Caminho de atribuição inválido em '" + caminhoRestante[i] + "'.");
      }
      const proximo = container.campos.get(caminhoRestante[i]);
      if (proximo === undefined) {
        throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + "] Campo '" + caminhoRestante[i] + "' inexistente.");
      }
      container = proximo;
    }
    if (container.classe !== 'REGISTRO') {
      throw new ErroExecucao('[Erro de Execução][Linha ' + cmd.linha + ':' + cmd.coluna + '] Atribuição a campo exige registro.');
    }
    container.campos.set(campoFinal, valor);
  }
}

/* ============================================================
 * Utilidades numéricas
 * ============================================================ */

/** Converte valores JS (de fixtures) para o universo THZ segundo um tipo declarado. */
export function valorThzDe(tipoDado: string, bruto: unknown): ValorThz {
  if (tipoDado.startsWith('NATURAL') || tipoDado.startsWith('INTEIRO')) {
    return INTEIRO(BigInt(typeof bruto === 'bigint' ? bruto : Math.trunc(Number(bruto))));
  }
  if (tipoDado.startsWith('MONETARIO')) {
    const casamento = /^MONETARIO\s*\(\s*"?([A-Z]{3})"?\s*\)/.exec(tipoDado);
    const codigoMoeda = casamento?.[1];
    if (!codigoMoeda) {
      throw new ErroMonetario("[Erro Monetário] Tipo '" + tipoDado + "' exige código ISO 4217: MONETARIO(\"BRL\") por exemplo.");
    }
    return typeof bruto === 'bigint'
      ? MONETARIO(Monetario.deInteiro(bruto, codigoMoeda))
      : MONETARIO(Monetario.deTexto(String(bruto), codigoMoeda));
  }
  if (tipoDado.startsWith('DECIMAL')) {
    // Escala do literal segue a declaração DECIMAL(P,S); padrão do motor: 4.
    const casamentoEscala = /,\s*(\d+)\s*\)\s*$/.exec(tipoDado);
    const escala = casamentoEscala ? Math.min(Number.parseInt(casamentoEscala[1], 10), DecimalFixo.ESCALA_PADRAO) : DecimalFixo.ESCALA_PADRAO;
    const numero = typeof bruto === 'string' ? bruto : Number(bruto).toFixed(escala);
    return DECIMAL(DecimalFixo.deTexto(numero, escala));
  }
  if (tipoDado === 'DATA') {
    if (typeof bruto === 'string') return DATA(DataThz.deTexto(bruto));
    throw new ErroData("[Erro Data] Valor para DATA deve ser texto 'AAAA-MM-DD'.");
  }
  if (tipoDado === 'DATA_HORA') {
    if (typeof bruto === 'string') return DATA_HORA(DataHoraThz.deTexto(bruto));
    throw new ErroData("[Erro DataHora] Valor para DATA_HORA deve ser texto 'AAAA-MM-DDTHH:MM[:SS]'.");
  }
  if (tipoDado === 'TEXTO') return TEXTO(String(bruto));
  if (tipoDado === 'LOGICO') {
    const s = String(bruto).toUpperCase();
    return LOGICO(s === 'VERDADEIRO' || s === 'TRUE' || s === '1');
  }
  // ENUMERACAO: tenta resolver membro
  return TEXTO(String(bruto));
}
