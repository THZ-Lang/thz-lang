/* ============================================================
 * THZ-LANG — Formatador Canônico (G6)
 * thz fmt — reconstitui AST em fonte canônica idempotente.
 * Limitacao: comentarios de linha (# ...) nao sao preservados,
 * pois o parser os descarta como espaco em branco (AST sem trivia).
 * O fmt e' estavel (fmt(fmt(x))==fmt(x)) e preserva semantica.
 * ============================================================ */

import type { ProgramaAST, ComandoAST, ExprAST } from './types.js';
import { textoCanonicoDe } from './parser.js';

const IND = '    ';

function tipoCanonico(tipo: string): string {
  // Normaliza: remove espaços, depois reintroduz espaço após vírgula
  return tipo.replace(/\s+/g, '').replace(/,/g, ', ');
}

function linha(v: string, nivel: number): string {
  return IND.repeat(nivel) + v;
}

function formatarExpr(expr: ExprAST): string {
  return textoCanonicoDe(expr);
}

function formatarComandos(comandos: ComandoAST[], nivel: number): string[] {
  const out: string[] = [];
  for (const c of comandos) {
    switch (c.tipoComando) {
      case 'DECL_VARIAVEL':
        out.push(linha(`VARIAVEL ${c.nome} : ${tipoCanonico(c.tipoDado)} <- ${formatarExpr(c.inicializacao)}`, nivel));
        break;
      case 'ATRIBUICAO':
        out.push(linha(`${c.alvo.join('.')} <- ${formatarExpr(c.expressao)}`, nivel));
        break;
      case 'SE':
        out.push(linha(`SE ${formatarExpr(c.condicao)}`, nivel));
        out.push(...formatarComandos(c.entao, nivel + 1));
        if (c.senao.length > 0) {
          out.push(linha('SENAO', nivel));
          out.push(...formatarComandos(c.senao, nivel + 1));
        }
        out.push(linha('FIM_SE', nivel));
        break;
      case 'ENQUANTO':
        out.push(linha(`ENQUANTO ${formatarExpr(c.condicao)}`, nivel));
        out.push(...formatarComandos(c.corpo, nivel + 1));
        out.push(linha('FIM_ENQUANTO', nivel));
        break;
      case 'VETORIZAR_PARA': {
        const passo = c.passoSimd !== undefined ? ` PASSO_SIMD ${c.passoSimd}` : '';
        out.push(linha(`VETORIZAR_PARA ${c.variavel} EM ${c.fonte.join('.')}${passo}`, nivel));
        out.push(...formatarComandos(c.corpo, nivel + 1));
        out.push(linha('FIM_PARA', nivel));
        break;
      }
      case 'PARA': {
        const passo = c.passo ? ` PASSO ${formatarExpr(c.passo)}` : '';
        out.push(linha(`PARA ${c.variavel} DE ${formatarExpr(c.inicio)} ATE ${formatarExpr(c.fim)}${passo}`, nivel));
        out.push(...formatarComandos(c.corpo, nivel + 1));
        out.push(linha('FIM_PARA', nivel));
        break;
      }
      case 'BLOCO_MEMORIA':
        out.push(linha(`USAR_BLOCO_MEMORIA ${c.nome}`, nivel));
        out.push(...formatarComandos(c.corpo, nivel + 1));
        out.push(linha('FIM_BLOCO_MEMORIA', nivel));
        break;
      case 'EXIBA':
        out.push(linha(`EXIBA ${formatarExpr(c.expressao)}`, nivel));
        break;
      case 'LER':
        out.push(linha(`LER ${c.alvo.join('.')}`, nivel));
        break;
      case 'CHAMADA':
        out.push(linha(formatarExpr(c.expressao), nivel));
        break;
      case 'RETORNE':
        out.push(linha(c.expressao ? `RETORNE ${formatarExpr(c.expressao)}` : 'RETORNE', nivel));
        break;
      case 'FALHAR_COM':
        out.push(linha(`FALHAR_COM ${formatarExpr(c.expressao)}`, nivel));
        break;
    }
  }
  return out;
}

export function formatar(ast: ProgramaAST): string {
  const out: string[] = [];

  if (ast.versaoLinguagem) {
    out.push(`VERSAO_LINGUAGEM "${ast.versaoLinguagem}"`);
    out.push('');
  }

  out.push(`PROGRAMA ${ast.nome}`);
  out.push('');

  if (ast.metadados) {
    out.push('METADADOS_ARQUITETURA');
    const m = ast.metadados;
    if (m.dominio) out.push(linha(`DOMINIO: "${m.dominio}"`, 1));
    if (m.subdominio) out.push(linha(`SUBDOMINIO: "${m.subdominio}"`, 1));
    if (m.camada) out.push(linha(`CAMADA: "${m.camada}"`, 1));
    if (m.versao) out.push(linha(`VERSAO: "${m.versao}"`, 1));
    if (m.autor) out.push(linha(`AUTOR: "${m.autor}"`, 1));
    if (m.sloLatencia) out.push(linha(`SLO_LATENCIA_MAXIMA: "${m.sloLatencia}"`, 1));
    if (m.conformidade.length > 0) {
      const lista = m.conformidade.map((c) => `"${c}"`).join(', ');
      out.push(linha(`CONFORMIDADE: ${lista}`, 1));
    }
    out.push('FIM_METADADOS');
    out.push('');
  }

  for (const est of ast.estruturas) {
    const layout = est.layoutColunar ? ' LAYOUT_COLUNAR' : '';
    out.push(`ESTRUTURA ${est.nome}${layout}`);
    for (const campo of est.campos) {
      out.push(linha(`${campo.nome} : ${tipoCanonico(campo.tipo)}`, 1));
    }
    for (const inv of est.invariantes) {
      out.push(linha(`INVARIANTE ${inv.textoCanonico}`, 1));
    }
    out.push('FIM_ESTRUTURA');
    out.push('');
  }

  for (const en of ast.enumeracoes) {
    out.push(`ENUMERACAO ${en.nome}`);
    for (const mem of en.membros) out.push(linha(mem, 1));
    out.push('FIM_ENUMERACAO');
    out.push('');
  }

  for (const regra of ast.regras) {
    out.push(`REGRA_NEGOCIO ${regra.nome}`);
    if (regra.identificador) out.push(linha(`IDENTIFICADOR_REGRA: "${regra.identificador}"`, 1));
    if (regra.rastreioRequisito) out.push(linha(`RASTREIO_REQUISITO: "${regra.rastreioRequisito}"`, 1));
    if (regra.descricao) out.push(linha(`DESCRICAO: "${regra.descricao}"`, 1));
    if (regra.clausulasEntrada.length > 0) {
      out.push(linha('CONTRATO_ENTRADA', 1));
      for (const c of regra.clausulasEntrada) out.push(linha(`EXIGE ${c.textoCanonico}`, 2));
      out.push(linha('FIM_CONTRATO_ENTRADA', 1));
    }
    if (regra.clausulasSaida.length > 0) {
      out.push(linha('CONTRATO_SAIDA', 1));
      for (const c of regra.clausulasSaida) out.push(linha(`GARANTE ${c.textoCanonico}`, 2));
      out.push(linha('FIM_CONTRATO_SAIDA', 1));
    }
    for (const op of regra.operacoes) {
      const params = op.parametros.map((p) => `${p.nome}: ${tipoCanonico(p.tipo)}`).join(', ');
      out.push(linha(`OPERACAO ${op.nome}(${params}) : ${tipoCanonico(op.tipoRetorno)}`, 1));
      if (op.corpo.length > 0) {
        out.push(linha('INICIO', 1));
        out.push(...formatarComandos(op.corpo, 2));
        out.push(linha('FIM', 1));
      }
    }
    out.push('FIM_REGRA_NEGOCIO');
    out.push('');
  }

  for (const proc of ast.procedimentos ?? []) {
    const params = proc.parametros.map((p) => `${p.nome}: ${tipoCanonico(p.tipo)}`).join(', ');
    out.push(`PROCEDIMENTO ${proc.nome}(${params})`);
    if (proc.corpo.length > 0) {
      out.push(linha('INICIO', 1));
      out.push(...formatarComandos(proc.corpo, 2));
      out.push(linha('FIM', 1));
    }
    out.push('');
  }

  out.push('FIM_PROGRAMA');
  out.push('');
  return out.join('\n');
}
