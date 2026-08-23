import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'fs';
import path from 'path';
import { ThzLexer } from '../src/lexer.js';
import { ThzParser } from '../src/parser.js';
import { formatar } from '../src/fmt.js';

function parse(fonte: string) {
  return new ThzParser(new ThzLexer(fonte).tokenize()).parse();
}

function semPos(ast: any): any {
  return JSON.parse(JSON.stringify(ast, (k, v) => {
    if (k === 'linha' || k === 'coluna') return undefined;
    if (typeof v === 'bigint') return v.toString();
    return v;
  }));
}

test('fmt — idempotente: fmt(fmt(x)) == fmt(x)', () => {
  const fonte = fs.readFileSync(path.join(process.cwd(), 'exemplos', 'faturamento.thz'), 'utf8');
  const ast = parse(fonte);
  const fmt1 = formatar(ast);
  const ast2 = parse(fmt1);
  const fmt2 = formatar(ast2);
  assert.equal(fmt2, fmt1);
});

test('fmt — preserva semântica (AST round-trip)', () => {
  const fonte = fs.readFileSync(path.join(process.cwd(), 'exemplos', 'faturamento.thz'), 'utf8');
  const ast = parse(fonte);
  const fmt = formatar(ast);
  const astFmt = parse(fmt);
  assert.deepEqual(semPos(astFmt), semPos(ast));
});

test('fmt — programa mínimo formatado contém blocos canônicos', () => {
  const fonte = `
VERSAO_LINGUAGEM "2.2"
PROGRAMA Demo
ESTRUTURA Item
  q : NATURAL32
FIM_ESTRUTURA
REGRA_NEGOCIO R
  OPERACAO Op(itens: FATIA[Item]) : DECIMAL(18,4)
  INICIO
    VARIAVEL x : DECIMAL(10,2) <- 5.00
    RETORNE x
  FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA`.trim();
  const fmt = formatar(parse(fonte));
  assert.match(fmt, /PROGRAMA Demo/);
  assert.match(fmt, /ESTRUTURA Item/);
  assert.match(fmt, /VARIAVEL x : DECIMAL\(10, 2\)/);
  assert.match(fmt, /FIM_PROGRAMA/);
});

test('fmt — invariantes e enumeracoes preservadas', () => {
  const fonte = fs.readFileSync(path.join(process.cwd(), 'exemplos', 'pedidos.thz'), 'utf8');
  const fmt = formatar(parse(fonte));
  assert.match(fmt, /INVARIANTE valor_total >= 0\.00/);
  assert.match(fmt, /ENUMERACAO StatusPedido/);
  assert.match(fmt, /FALHAR_COM/);
});

test('fmt — VETORIZAR_PARA com PASSO_SIMD preservado', () => {
  const fonte = `
PROGRAMA P
ESTRUTURA Item LAYOUT_COLUNAR
  q : NATURAL32
FIM_ESTRUTURA
REGRA_NEGOCIO R
  OPERACAO Op(itens: FATIA[Item]) : DECIMAL(18,4)
  INICIO
    VETORIZAR_PARA item EM itens PASSO_SIMD 16
      EXIBA item.q
    FIM_PARA
    RETORNE 0.0
  FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA`.trim();
  const fmt = formatar(parse(fonte));
  assert.match(fmt, /VETORIZAR_PARA item EM itens PASSO_SIMD 16/);
  assert.match(fmt, /FIM_PARA/);
});

test('fmt — já-canônico permanece idêntico', () => {
  const fonte = fs.readFileSync(path.join(process.cwd(), 'exemplos', 'faturamento.thz'), 'utf8');
  const ast = parse(fonte);
  const fmt = formatar(ast);
  const ast2 = parse(fmt);
  assert.equal(formatar(ast2), fmt);
  assert.deepEqual(semPos(ast2), semPos(ast));
});
