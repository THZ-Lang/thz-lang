import { test } from 'node:test';
import assert from 'node:assert/strict';
import { verificarVetorizado, passoParaLlvm } from '../src/simd.js';
import type { ComandoAST, EstruturaAST } from '../src/types.js';

function vetorizar(overrides: Partial<Extract<ComandoAST, { tipoComando: 'VETORIZAR_PARA' }>> = {}): Extract<ComandoAST, { tipoComando: 'VETORIZAR_PARA' }> {
  return {
    tipoComando: 'VETORIZAR_PARA',
    variavel: 'item',
    fonte: ['itens'],
    corpo: [],
    linha: 1,
    coluna: 1,
    ...overrides,
  };
}

function estMap(layout: boolean): Map<string, EstruturaAST> {
  return new Map([['Item', { nome: 'Item', layoutColunar: layout, campos: [{ nome: 'q', tipo: 'NATURAL32' }], invariantes: [] }]]);
}

test('SIMD — SoA verificado com passo válido', () => {
  const cmd = vetorizar({ passoSimd: 8, corpo: [{ tipoComando: 'DECL_VARIAVEL', nome: 'x', tipoDado: 'DECIMAL(12,4)', inicializacao: { tipo: 'ACESSO', caminho: ['item', 'q'], linha: 1, coluna: 1 }, linha: 1, coluna: 1 }] as any });
  const r = verificarVetorizado(cmd, estMap(true), new Set(['itens']));
  assert.equal(r.verificado, true);
  assert.equal(r.layoutFonte, 'SoA');
  assert.equal(r.passoEfetivo, 8);
  assert.equal(r.diagnosticos.length, 0);
});

test('SIMD — AoS gera diagnóstico R2', () => {
  const cmd = vetorizar({ passoSimd: 8 });
  const r = verificarVetorizado(cmd, estMap(false), new Set());
  assert.equal(r.verificado, false);
  assert.ok(r.diagnosticos.some((d) => d.includes('R2')));
  assert.equal(r.layoutFonte, 'AoS');
});

test('SIMD — PASSO_SIMD inválido (não potência de dois)', () => {
  const cmd = vetorizar({ passoSimd: 7 });
  const r = verificarVetorizado(cmd, estMap(true), new Set());
  assert.equal(r.verificado, false);
  assert.ok(r.diagnosticos.some((d) => d.includes('R3') && d.includes('potência')));
});

test('SIMD — PASSO_SIMD fora do intervalo 4..64', () => {
  const cmd = vetorizar({ passoSimd: 128 });
  const r = verificarVetorizado(cmd, estMap(true), new Set());
  assert.equal(r.verificado, false);
  assert.ok(r.diagnosticos.some((d) => d.includes('fora do intervalo')));
});

test('SIMD — passo ausente assume 8', () => {
  const cmd = vetorizar({});
  const r = verificarVetorizado(cmd, estMap(true), new Set());
  assert.equal(r.passoEfetivo, 8);
  assert.ok(r.regrasAplicadas.some((d) => d.includes('implícito')));
});

test('SIMD — corpo com ENQUANTO reprova R4', () => {
  const cmd = vetorizar({ corpo: [{ tipoComando: 'ENQUANTO', condicao: { tipo: 'LITERAL_LOGICO', valor: true, linha: 1, coluna: 1 }, corpo: [], linha: 1, coluna: 1 }] });
  const r = verificarVetorizado(cmd, estMap(true), new Set());
  assert.equal(r.verificado, false);
  assert.ok(r.diagnosticos.some((d) => d.includes('ENQUANTO')));
});

test('SIMD — RETORNE dentro do corpo reprova', () => {
  const cmd = vetorizar({ corpo: [{ tipoComando: 'RETORNE', linha: 1, coluna: 1 }] });
  const r = verificarVetorizado(cmd, estMap(true), new Set());
  assert.ok(r.diagnosticos.some((d) => d.includes('RETORNE')));
});

test('SIMD — redução verificada', () => {
  const cmd = vetorizar({
    corpo: [
      {
        tipoComando: 'ATRIBUICAO',
        alvo: ['acc'],
        expressao: { tipo: 'OP_BINARIA', operador: '+', esquerda: { tipo: 'ACESSO', caminho: ['acc'], linha: 1, coluna: 1 }, direita: { tipo: 'ACESSO', caminho: ['item', 'q'], linha: 1, coluna: 1 }, linha: 1, coluna: 1 },
        linha: 1,
        coluna: 1,
      } as any,
    ],
  });
  const r = verificarVetorizado(cmd, estMap(true), new Set(['acc']));
  assert.equal(r.verificado, true);
  assert.ok(r.regrasAplicadas.some((d) => d.includes('redução')));
});

test('SIMD — escrita externa não-redução reprova', () => {
  const cmd = vetorizar({
    corpo: [
      {
        tipoComando: 'ATRIBUICAO',
        alvo: ['acc'],
        expressao: { tipo: 'LITERAL_INTEIRO', valor: 1n, linha: 1, coluna: 1 },
        linha: 1,
        coluna: 1,
      } as any,
    ],
  });
  const r = verificarVetorizado(cmd, estMap(true), new Set(['acc']));
  assert.equal(r.verificado, false);
  assert.ok(r.diagnosticos.some((d) => d.includes('redução')));
});

test('SIMD — passoParaLlvm normaliza', () => {
  assert.equal(passoParaLlvm(null), 8);
  assert.equal(passoParaLlvm(16), 16);
  assert.equal(passoParaLlvm(7), 8);
  assert.equal(passoParaLlvm(32), 32);
});
