import fs from 'fs';
import path from 'path';
import { ThzLexer } from '../src/lexer.js';
import { ThzParser } from '../src/parser.js';
import { InterpretadorThz, valorThzDe, INTEIRO } from '../src/interpretador.js';
import type { ProgramaAST, EstruturaAST, OperacaoAST } from '../src/types.js';
import type { ValorThz } from '../src/interpretador.js';
import { ArenaMemoria } from '../src/runtime.js';
import { medir } from './helpers.js';

function estruturaPorNome(ast: ProgramaAST, nome: string): EstruturaAST | undefined {
  return ast.estruturas.find((e) => e.nome === nome);
}
function registroDe(est: EstruturaAST, valores: unknown[]): ValorThz {
  const campos = new Map<string, ValorThz>();
  est.campos.forEach((c, i) => {
    const bruto = valores[i];
    if (bruto !== undefined) campos.set(c.nome, valorThzDe(c.tipo, bruto));
    else if (c.tipo.startsWith('NATURAL') || c.tipo.startsWith('INTEIRO')) campos.set(c.nome, INTEIRO(0n));
    else if (c.tipo.startsWith('DECIMAL') || c.tipo.startsWith('MONETARIO')) campos.set(c.nome, valorThzDe(c.tipo, '0'));
    else campos.set(c.nome, valorThzDe(c.tipo, ''));
  });
  return { classe: 'REGISTRO', nomeEstrutura: est.nome, campos };
}
function construir(ast: ProgramaAST, op: OperacaoAST, N: number): Record<string, ValorThz> {
  const args: Record<string, ValorThz> = {};
  for (const p of op.parametros) {
    const m = /^FATIA\[(\w+)\]$/.exec(p.tipo);
    if (m) {
      const est = estruturaPorNome(ast, m[1])!;
      const elems: ValorThz[] = [];
      for (let i = 0; i < N; i++) {
        const vals: unknown[] = est.campos.map((c) => {
          if (c.tipo === 'NATURAL32' || c.tipo.startsWith('NATURAL') || c.tipo.startsWith('INTEIRO')) return 10 + (i % 5);
          if (c.tipo.startsWith('DECIMAL')) {
            const m = /,\s*(\d+)\s*\)/.exec(c.tipo);
            const escala = m ? parseInt(m[1], 10) : 4;
            return (100 + (i % 10) * 10).toFixed(escala);
          }
          if (c.tipo.startsWith('MONETARIO')) return (100 + (i % 10) * 10).toFixed(2);
          if (c.tipo === 'TEXTO') return `SKU-${i}`;
          if (c.tipo === 'UUID') return `00000000-0000-0000-0000-${String(i).padStart(12, '0')}`;
          return '0';
        });
        elems.push(registroDe(est, vals));
      }
      args[p.nome] = { classe: 'FATIA', tipoInterno: m[1], elementos: elems };
    } else args[p.nome] = valorThzDe(p.tipo, 0);
  }
  return args;
}

export function benchSimd(): void {
  const fonte = fs.readFileSync(path.join(process.cwd(), 'exemplos', 'faturamento.thz'), 'utf8');
  const ast = new ThzParser(new ThzLexer(fonte).tokenize()).parse();
  const interp = new InterpretadorThz(ast, { saida: () => {} });
  const alvo = interp.listarOperacoesExecutaveis()[0];
  const op = alvo.operacao;

  // Tamanhos de lote
  for (const N of [100, 1_000, 10_000]) {
    const args = construir(ast, op, N);
    const arena = new ArenaMemoria(64);
    arena.alocar(2048);
    const r = medir(`VETORIZAR_PARA N=${N} (fat slice)`, () => {
      interp.executarOperacao(op.nome, args);
    }, N >= 10_000 ? 20 : 100);
    arena.liberarTudo();
    console.log(`  N=${String(N).padStart(5)} → ${r.mediaMs.toFixed(3)} ms/op  (${r.opsSec.toFixed(0)} ops/s)`);
  }

  // Comparativo: passo SIMD 4 vs 8 vs 16 (via re-parse do fonte com passo diferente)
  console.log('— Passo SIMD (tradeoff largura vs overhead) —');
  for (const passo of [4, 8, 16]) {
    const fonteP = fonte.replace(/PASSO_SIMD \d+/, `PASSO_SIMD ${passo}`);
    const astP = new ThzParser(new ThzLexer(fonteP).tokenize()).parse();
    const interpP = new InterpretadorThz(astP, { saida: () => {} });
    const alvoP = interpP.listarOperacoesExecutaveis()[0];
    const argsP = construir(astP, alvoP.operacao, 5_000);
    const r = medir(`PASSO_SIMD ${passo}`, () => { interpP.executarOperacao(alvoP.operacao.nome, argsP); }, 50);
    console.log(`  passo ${String(passo).padStart(2)}: ${r.mediaMs.toFixed(3)} ms/op`);
  }
}
