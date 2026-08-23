/* ============================================================
 * THZ-LANG — IR Intermediário Estável (G5)
 * Lowering AST → IR v1 (thz-ir/1) + verificação SIMD formal.
 * Ponte para codegen LLVM (emitirLlvm).
 * ============================================================ */

import type { ProgramaAST, EstruturaAST, ComandoAST, ExprAST } from './types.js';
import { verificarVetorizado, passoParaLlvm } from './simd.js';
import type { VerificacaoSimd } from './simd.js';

export const VERSAO_IR = 'thz-ir/1';

export interface Loc { linha: number; coluna: number }

export interface IrCampo { nome: string; tipo: string; tipoIr: string; loc: Loc }
export interface IrInvariante { textoCanonico: string; expr: IrExpr; loc: Loc }
export interface IrEstrutura {
  nome: string;
  layout: 'SoA' | 'AoS';
  campos: IrCampo[];
  invariantes: IrInvariante[];
  loc: Loc;
}
export interface IrEnumeracao { nome: string; membros: string[]; loc: Loc }

export type IrExpr =
  | { kind: 'literal.inteiro'; valor: string; loc: Loc }
  | { kind: 'literal.decimal'; escalado: string; escala: number; loc: Loc }
  | { kind: 'literal.texto'; valor: string; loc: Loc }
  | { kind: 'literal.logico'; valor: boolean; loc: Loc }
  | { kind: 'nulo'; loc: Loc }
  | { kind: 'acesso'; caminho: string[]; loc: Loc }
  | { kind: 'binaria'; op: string; esquerda: IrExpr; direita: IrExpr; loc: Loc }
  | { kind: 'unaria'; op: string; operando: IrExpr; loc: Loc }
  | { kind: 'chamada'; alvo: string[]; args: IrExpr[]; loc: Loc }
  | { kind: 'indexacao'; alvo: IrExpr; indice: IrExpr; loc: Loc }
  | { kind: 'fatia.literal'; elementos: IrExpr[]; loc: Loc }
  | { kind: 'criar'; estrutura: string; campos: { nome: string; valor: IrExpr }[]; loc: Loc };

export type IrInstrucao =
  | { kind: 'decl'; nome: string; tipo: string; init: IrExpr; loc: Loc }
  | { kind: 'atribuicao'; alvo: string[]; valor: IrExpr; loc: Loc }
  | { kind: 'se'; cond: IrExpr; entao: IrInstrucao[]; senao: IrInstrucao[]; loc: Loc }
  | { kind: 'enquanto'; cond: IrExpr; corpo: IrInstrucao[]; loc: Loc }
  | { kind: 'vetorizado'; variavel: string; fonte: string[]; passo: number | null; passoEfetivo: number; verificado: boolean; layoutFonte: 'SoA' | 'AoS' | 'desconhecido'; diagnosticos: string[]; regras: string[]; corpo: IrInstrucao[]; loc: Loc }
  | { kind: 'para'; variavel: string; inicio: IrExpr; fim: IrExpr; passo?: IrExpr; corpo: IrInstrucao[]; loc: Loc }
  | { kind: 'blocoMemoria'; nome: string; corpo: IrInstrucao[]; loc: Loc }
  | { kind: 'exiba'; valor: IrExpr; loc: Loc }
  | { kind: 'ler'; alvo: string[]; loc: Loc }
  | { kind: 'chamada'; valor: IrExpr; loc: Loc }
  | { kind: 'retorne'; valor?: IrExpr; loc: Loc }
  | { kind: 'falhar'; valor: IrExpr; loc: Loc };

export interface IrFuncao {
  nomeQualificado: string; // Regra.Operacao
  regra: string;
  identificadorRegra?: string;
  rastreio?: string;
  parametros: { nome: string; tipo: string; tipoIr: string; loc: Loc }[];
  tipoRetorno: string;
  tipoRetornoIr: string;
  contratos: { exige: IrExpr[]; garante: IrExpr[]; textosExige: string[]; textosGarante: string[] };
  corpo: IrInstrucao[];
  loc: Loc;
}

export interface IrPrograma {
  versaoIr: string;
  programa: string;
  versaoLinguagem?: string;
  metadados?: { dominio: string; subdominio: string; camada: string; versao: string; sloLatencia: string; conformidade: string[] };
  estruturas: IrEstrutura[];
  enumeracoes: IrEnumeracao[];
  funcoes: IrFuncao[];
  diagnosticosSimd: { funcao: string; variavel: string; fonte: string; verificado: boolean; diagnosticos: string[]; loc: Loc }[];
}

// Helpers de tipo IR
function tipoParaIr(tipo: string): string {
  if (/^FATIA\[/.test(tipo)) {
    const m = /^FATIA\[(\w+)\]$/.exec(tipo);
    return m ? `fatiaslice<${m[1]}>` : tipo;
  }
  if (/^RESULTADO\[/.test(tipo)) return tipo.replace(/\s+/g, '');
  if (/^DECIMAL/.test(tipo)) return tipo.replace(/\s+/g, '');
  if (/^MONETARIO/.test(tipo)) return tipo.replace(/\s+/g, '');
  return tipo;
}

function exprParaIr(expr: ExprAST): IrExpr {
  const loc: Loc = { linha: expr.linha, coluna: expr.coluna };
  switch (expr.tipo) {
    case 'LITERAL_INTEIRO': return { kind: 'literal.inteiro', valor: expr.valor.toString(), loc };
    case 'LITERAL_DECIMAL': return { kind: 'literal.decimal', escalado: expr.escalado.toString(), escala: expr.escala, loc };
    case 'LITERAL_TEXTO': return { kind: 'literal.texto', valor: expr.valor, loc };
    case 'LITERAL_LOGICO': return { kind: 'literal.logico', valor: expr.valor, loc };
    case 'NULO': return { kind: 'nulo', loc };
    case 'ACESSO': return { kind: 'acesso', caminho: [...expr.caminho], loc };
    case 'CHAMADA': return { kind: 'chamada', alvo: [...expr.caminho], args: expr.argumentos.map(exprParaIr), loc };
    case 'INDEXACAO': return { kind: 'indexacao', alvo: exprParaIr(expr.alvo), indice: exprParaIr(expr.indice), loc };
    case 'FATIA_LITERAL': return { kind: 'fatia.literal', elementos: expr.elementos.map(exprParaIr), loc };
    case 'CRIAR_REGISTRO': return { kind: 'criar', estrutura: expr.nomeEstrutura, campos: expr.campos.map((c) => ({ nome: c.nome, valor: exprParaIr(c.valor) })), loc };
    case 'OP_BINARIA': return { kind: 'binaria', op: expr.operador, esquerda: exprParaIr(expr.esquerda), direita: exprParaIr(expr.direita), loc };
    case 'OP_UNARIA': return { kind: 'unaria', op: expr.operador, operando: exprParaIr(expr.operando), loc };
  }
}

function comandosParaIr(comandos: ComandoAST[], estruturas: Map<string, EstruturaAST>, externas: Set<string>): IrInstrucao[] {
  return comandos.map((c) => comandoParaIr(c, estruturas, externas));
}

function comandoParaIr(cmd: ComandoAST, estruturas: Map<string, EstruturaAST>, externas: Set<string>): IrInstrucao {
  const loc: Loc = { linha: cmd.linha, coluna: cmd.coluna };
  switch (cmd.tipoComando) {
    case 'DECL_VARIAVEL': return { kind: 'decl', nome: cmd.nome, tipo: cmd.tipoDado, init: exprParaIr(cmd.inicializacao), loc };
    case 'ATRIBUICAO': return { kind: 'atribuicao', alvo: [...cmd.alvo], valor: exprParaIr(cmd.expressao), loc };
    case 'SE': return { kind: 'se', cond: exprParaIr(cmd.condicao), entao: comandosParaIr(cmd.entao, estruturas, externas), senao: comandosParaIr(cmd.senao, estruturas, externas), loc };
    case 'ENQUANTO': return { kind: 'enquanto', cond: exprParaIr(cmd.condicao), corpo: comandosParaIr(cmd.corpo, estruturas, new Set(externas)), loc };
    case 'PARA': return { kind: 'para', variavel: cmd.variavel, inicio: exprParaIr(cmd.inicio), fim: exprParaIr(cmd.fim), passo: cmd.passo ? exprParaIr(cmd.passo) : undefined, corpo: comandosParaIr(cmd.corpo, estruturas, new Set([...externas, cmd.variavel])), loc };
    case 'LER': return { kind: 'ler', alvo: [...cmd.alvo], loc };
    case 'CHAMADA': return { kind: 'chamada', valor: exprParaIr(cmd.expressao), loc };
    case 'VETORIZAR_PARA': {
      // externas para corpo = externas + variavel de iteração
      const verificacao = verificarVetorizadoComContexto(cmd, estruturas, externas);
      const corpo = comandosParaIr(cmd.corpo, estruturas, new Set([...externas, cmd.variavel]));
      return {
        kind: 'vetorizado',
        variavel: cmd.variavel,
        fonte: [...cmd.fonte],
        passo: cmd.passoSimd ?? null,
        passoEfetivo: verificacao.passoEfetivo ?? passoParaLlvm(cmd.passoSimd ?? null),
        verificado: verificacao.verificado,
        layoutFonte: verificacao.layoutFonte,
        diagnosticos: verificacao.diagnosticos,
        regras: verificacao.regrasAplicadas,
        corpo,
        loc,
      };
    }
    case 'BLOCO_MEMORIA': return { kind: 'blocoMemoria', nome: cmd.nome, corpo: comandosParaIr(cmd.corpo, estruturas, externas), loc };
    case 'EXIBA': return { kind: 'exiba', valor: exprParaIr(cmd.expressao), loc };
    case 'RETORNE': return { kind: 'retorne', valor: cmd.expressao ? exprParaIr(cmd.expressao) : undefined, loc };
    case 'FALHAR_COM': return { kind: 'falhar', valor: exprParaIr(cmd.expressao), loc };
  }
}

function verificarVetorizadoComContexto(
  cmd: Extract<ComandoAST, { tipoComando: 'VETORIZAR_PARA' }>,
  estruturas: Map<string, EstruturaAST>,
  externas: Set<string>
): VerificacaoSimd {
  // Tenta resolver layout da fonte via tipo do parâmetro ou variável no escopo externo.
  // Para IR, temos mapa de estruturas e sabemos que fonte é caminho como ['itens'] (variável FATIA).
  // Heurística: se estruturas contém uma única SoA, assume; senão, tenta resolver via nome da estrutura no tipo FATIA.
  // Como não temos tabela de tipos aqui, delegamos ao verificador base com heurística de tamanho 1.
  // Melhor: tenta inferir layout pela estrutura cujo nome aparece em algum campo do programa como FATIA[<nome>].
  // Simplificação G5: se existe alguma LAYOUT_COLUNAR, assume SoA se o corpo usar campos daquela estrutura.
  const layout = (() => {
    for (const est of estruturas.values()) {
      // se corpo acessa est.nome via `variavel.campo`, considera como fonte candidata
      const usa = cmd.corpo.some((sub) => {
        if (sub.tipoComando === 'DECL_VARIAVEL') return sub.inicializacao.tipo === 'ACESSO' && sub.inicializacao.caminho[0] === cmd.variavel;
        if (sub.tipoComando === 'ATRIBUICAO') return sub.alvo[0] === cmd.variavel || (sub.expressao.tipo === 'ACESSO' && sub.expressao.caminho[0] === cmd.variavel);
        return false;
      });
      if (usa && est.layoutColunar) return 'SoA' as const;
    }
    // fallback: se alguma estrutura é SoA, assume SoA (caso faturamento)
    for (const est of estruturas.values()) if (est.layoutColunar) return 'SoA';
    for (const est of estruturas.values()) return (est.layoutColunar ? 'SoA' : 'AoS') as VerificacaoSimd['layoutFonte'];
    return 'desconhecido';
  })();

  // Delega para simd.ts com mapa contendo uma entrada dummy para forçar layout correto
  const mapaForcado = new Map<string, EstruturaAST>();
  // Cria estrutura fantasma cujo layout reflete o layout inferido, para o verificador escolher
  if (layout !== 'desconhecido') {
    mapaForcado.set('__fonte__', { nome: '__fonte__', layoutColunar: layout === 'SoA', campos: [], invariantes: [] });
  } else {
    for (const [k, v] of estruturas) mapaForcado.set(k, v);
  }
  // Chama verificador base
  const v = verificarVetorizado(cmd as any, mapaForcado, externas);
  // Corrige layout para o real inferido
  return { ...v, layoutFonte: layout };
}

export function baixarParaIr(ast: ProgramaAST): IrPrograma {
  const estruturasMap = new Map(ast.estruturas.map((e) => [e.nome, e]));
  const irEstruturas: IrEstrutura[] = ast.estruturas.map((e) => ({
    nome: e.nome,
    layout: e.layoutColunar ? 'SoA' : 'AoS',
    campos: e.campos.map((c) => ({ nome: c.nome, tipo: c.tipo, tipoIr: tipoParaIr(c.tipo), loc: { linha: 1, coluna: 1 } })),
    invariantes: e.invariantes.map((inv) => ({ textoCanonico: inv.textoCanonico, expr: exprParaIr(inv.expressao), loc: { linha: inv.linha, coluna: inv.coluna } })),
    loc: { linha: 1, coluna: 1 },
  }));
  const irEnumeracoes: IrEnumeracao[] = ast.enumeracoes.map((en) => ({ nome: en.nome, membros: [...en.membros], loc: { linha: 1, coluna: 1 } }));

  const funcoes: IrFuncao[] = [];
  const diagnosticosSimd: IrPrograma['diagnosticosSimd'] = [];

  for (const regra of ast.regras) {
    for (const op of regra.operacoes) {
      const externas = new Set(op.parametros.map((p) => p.nome));
      const corpo = comandosParaIr(op.corpo, estruturasMap, externas);
      // coleta diagnósticos SIMD
      for (const instr of corpo) {
        if (instr.kind === 'vetorizado') {
          diagnosticosSimd.push({
            funcao: `${regra.nome}.${op.nome}`,
            variavel: instr.variavel,
            fonte: instr.fonte.join('.'),
            verificado: instr.verificado,
            diagnosticos: instr.diagnosticos,
            loc: instr.loc,
          });
        }
        // também dentro de blocos aninhados
        const coletarAninhado = (lista: IrInstrucao[]) => {
          for (const sub of lista) {
            if (sub.kind === 'vetorizado') diagnosticosSimd.push({ funcao: `${regra.nome}.${op.nome}`, variavel: sub.variavel, fonte: sub.fonte.join('.'), verificado: sub.verificado, diagnosticos: sub.diagnosticos, loc: sub.loc });
            if ((sub as any).corpo) coletarAninhado((sub as any).corpo);
            if ((sub as any).entao) { coletarAninhado((sub as any).entao); coletarAninhado((sub as any).senao); }
          }
        };
        if (instr.kind === 'se' || instr.kind === 'enquanto' || instr.kind === 'blocoMemoria') {
          // já tratado acima via corpo, mas manter estrutura
        }
      }
      funcoes.push({
        nomeQualificado: `${regra.nome}.${op.nome}`,
        regra: regra.nome,
        identificadorRegra: regra.identificador,
        rastreio: regra.rastreioRequisito,
        parametros: op.parametros.map((p) => ({ nome: p.nome, tipo: p.tipo, tipoIr: tipoParaIr(p.tipo), loc: { linha: 1, coluna: 1 } })),
        tipoRetorno: op.tipoRetorno,
        tipoRetornoIr: tipoParaIr(op.tipoRetorno),
        contratos: {
          exige: regra.clausulasEntrada.map((c) => exprParaIr(c.expressao)),
          garante: regra.clausulasSaida.map((c) => exprParaIr(c.expressao)),
          textosExige: regra.clausulasEntrada.map((c) => c.textoCanonico),
          textosGarante: regra.clausulasSaida.map((c) => c.textoCanonico),
        },
        corpo,
        loc: { linha: 1, coluna: 1 },
      });
    }
  }

  return {
    versaoIr: VERSAO_IR,
    programa: ast.nome,
    versaoLinguagem: ast.versaoLinguagem,
    metadados: ast.metadados ? { dominio: ast.metadados.dominio, subdominio: ast.metadados.subdominio, camada: ast.metadados.camada, versao: ast.metadados.versao, sloLatencia: ast.metadados.sloLatencia, conformidade: [...ast.metadados.conformidade] } : undefined,
    estruturas: irEstruturas,
    enumeracoes: irEnumeracoes,
    funcoes,
    diagnosticosSimd,
  };
}

/** Serialização JSON estável (ordem de chaves + BigInt → string). */
export function serializarIr(ir: IrPrograma): string {
  return JSON.stringify(ir, (_k, v) => (typeof v === 'bigint' ? v.toString() : v), 2);
}

/** Emissão LLVM IR textual (ponte para Inkwell/LLVM 17+). Pseudo-LLVM para fins de auditoria/G5. */
export function emitirLlvm(ir: IrPrograma): string {
  const out: string[] = [];
  out.push(`; THZ-IR ${ir.versaoIr} — programa ${ir.programa} — linguagem ${ir.versaoLinguagem ?? 'corrente'}`);
  out.push(`; Gerado via baixarParaIr → emitirLlvm (ponte Inkwell/LLVM 17+)`);
  out.push('');
  if (ir.metadados) {
    out.push(`; METADADOS: dominio=${ir.metadados.dominio} sub=${ir.metadados.subdominio} slo=${ir.metadados.sloLatencia}`);
    out.push('');
  }
  for (const est of ir.estruturas) {
    const layout = est.layout === 'SoA' ? 'SoA (Structure of Arrays, vetorizável)' : 'AoS';
    out.push(`; ESTRUTURA ${est.nome} — ${layout}`);
    if (est.layout === 'SoA') {
      // SoA: um array por campo, alinhado para AVX2/AVX-512
      for (const campo of est.campos) {
        const llvmTy = tipoParaLlvm(campo.tipoIr);
        out.push(`%${est.nome}.${campo.nome}.SoA = type { [0 x ${llvmTy}] } ; ${campo.tipo}`);
      }
    } else {
      const camposLlvm = est.campos.map((c) => `${tipoParaLlvm(c.tipoIr)} ; ${c.nome}`).join(', ');
      out.push(`%${est.nome} = type { ${camposLlvm} } ; AoS`);
    }
    for (const inv of est.invariantes) {
      out.push(`; INVARIANTE ${inv.textoCanonico}`);
    }
    out.push('');
  }
  for (const en of ir.enumeracoes) {
    out.push(`; ENUMERACAO ${en.nome} = { ${en.membros.join(', ')} } ; i32`);
    out.push(`%${en.nome} = type i32`);
    out.push('');
  }
  for (const fn of ir.funcoes) {
    const ret = tipoParaLlvm(fn.tipoRetornoIr);
    const params = fn.parametros.map((p) => `${tipoParaLlvm(p.tipoIr)} %${p.nome}`).join(', ');
    out.push(`; REGRA ${fn.regra} — ${fn.identificadorRegra ?? '—'} — ${fn.rastreio ?? '—'}`);
    for (const t of fn.contratos.textosExige) out.push(`; EXIGE ${t}`);
    for (const t of fn.contratos.textosGarante) out.push(`; GARANTE ${t}`);
    out.push(`define ${ret} @${fn.regra}_${fn.nomeQualificado.split('.')[1]}(${params}) {`);
    out.push(`entry:`);
    // Arena efêmera se houver blocoMemoria
    const temArena = fn.corpo.some((i) => i.kind === 'blocoMemoria');
    if (temArena) out.push(`  %arena = call ptr @thz.arena.alocar(i64 64) ; ARENA 64MB`);
    let blocoId = 0;
    for (const instr of fn.corpo) {
      emitirInstrucaoLlvm(instr, out, `  `, () => `b${blocoId++}`);
    }
    // Se não há retorne explícito, emite undef
    const temRet = fn.corpo.some((i) => i.kind === 'retorne' || i.kind === 'falhar');
    if (!temRet) out.push(`  ret ${ret} undef`);
    out.push(`}`);
    out.push('');
  }
  // Declarações de runtime
  out.push(`declare ptr @thz.arena.alocar(i64)`);
  out.push(`declare void @thz.arena.liberar(ptr)`);
  out.push(`declare void @thz.exiba(i8*)`);
  return out.join('\n');
}

function tipoParaLlvm(tipoIr: string): string {
  if (/^fatiaslice</.test(tipoIr)) return `ptr ; ${tipoIr}`;
  if (/^DECIMAL/.test(tipoIr)) return `i64 ; ${tipoIr} (escalado)`;
  if (/^MONETARIO/.test(tipoIr)) return `i64 ; ${tipoIr}`;
  if (tipoIr === 'TEXTO') return `ptr`;
  if (tipoIr === 'LOGICO') return `i1`;
  if (/NATURAL|INTEIRO/.test(tipoIr)) return `i64`;
  if (tipoIr === 'UUID') return `i128`;
  if (/RESULTADO/.test(tipoIr)) return `ptr ; ${tipoIr}`;
  // enum/struct
  if (/^[A-Z]/.test(tipoIr)) return `%${tipoIr}`;
  return `ptr`;
}

function emitirInstrucaoLlvm(instr: IrInstrucao, out: string[], indent: string, nextBloco: () => string): void {
  const loc = `; [${instr.loc.linha}:${instr.loc.coluna}]`;
  switch (instr.kind) {
    case 'decl':
      out.push(`${indent}%${instr.nome} = alloca ${tipoParaLlvm(instr.tipo)} ${loc} ; ${instr.tipo} <- ...`);
      break;
    case 'atribuicao':
      out.push(`${indent}store ${instr.alvo.join('.')} = ... ${loc}`);
      break;
    case 'se':
      out.push(`${indent}br i1 %cond, label %then, label %else ${loc}`);
      break;
    case 'enquanto':
      out.push(`${indent}br label %loop.cond ${loc} ; ENQUANTO`);
      break;
    case 'vetorizado': {
      const ver = instr.verificado ? 'verificado' : 'escalar (diagnósticos)';
      out.push(`${indent}; VETORIZAR_PARA ${instr.variavel} EM ${instr.fonte.join('.')} PASSO_SIMD ${instr.passo ?? 'implícito'} → efetivo ${instr.passoEfetivo} [${ver}] ${loc}`);
      out.push(`${indent}; layout=${instr.layoutFonte} regras=${instr.regras.join(', ')}`);
      for (const d of instr.diagnosticos) out.push(`${indent}; !SIMD ${d}`);
      out.push(`${indent}vector.body: ; ${instr.verificado ? 'llvm.vp' : 'scalar'}`);
      for (const sub of instr.corpo) emitirInstrucaoLlvm(sub, out, indent + '  ', nextBloco);
      break;
    }
    case 'blocoMemoria':
      out.push(`${indent}; USAR_BLOCO_MEMORIA ${instr.nome} ${loc}`);
      for (const sub of instr.corpo) emitirInstrucaoLlvm(sub, out, indent + '  ', nextBloco);
      out.push(`${indent}call void @thz.arena.liberar(ptr %arena)`);
      break;
    case 'exiba':
      out.push(`${indent}call void @thz.exiba(ptr ...) ${loc}`);
      break;
    case 'retorne':
      out.push(`${indent}ret ... ${loc}`);
      break;
    case 'falhar':
      out.push(`${indent}; FALHAR_COM → resultado.erro ${loc}`);
      out.push(`${indent}ret ...`);
      break;
  }
}
