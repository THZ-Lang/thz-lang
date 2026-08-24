import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ThzParser } from '../src/parser.js';
import { ThzLexer } from '../src/lexer.js';

function analisar(codigo: string) {
  const tokens = new ThzLexer(codigo).tokenize();
  return new ThzParser(tokens).parse();
}

const PROGRAMA_MINIMO = `VERSAO_LINGUAGEM "2.2"
PROGRAMA Demo
  ESTRUTURA Item LAYOUT_COLUNAR
    quantidade : NATURAL32
    valor : DECIMAL(12, 4)
  FIM_ESTRUTURA
FIM_PROGRAMA`;

test('pragma VERSAO_LINGUAGEM é capturado na AST', () => {
  const ast = analisar(PROGRAMA_MINIMO);
  assert.equal(ast.versaoLinguagem, '2.2');
  assert.equal(ast.nome, 'Demo');
});

test('programa sem pragma permanece válido', () => {
  const ast = analisar('PROGRAMA X\nFIM_PROGRAMA');
  assert.equal(ast.versaoLinguagem, undefined);
});

test('layout colunar é reconhecido via token reservado', () => {
  const ast = analisar(PROGRAMA_MINIMO);
  assert.equal(ast.estruturas[0].layoutColunar, true);
  assert.equal(ast.estruturas[0].campos[1].tipo, 'DECIMAL(12,4)');
});

test('palavra reservada como nome de estrutura é rejeitada com mensagem clara', () => {
  assert.throws(
    () => analisar('PROGRAMA X\nESTRUTURA EM\nFIM_ESTRUTURA\nFIM_PROGRAMA'),
    /palavra reservada/
  );
});

test('token desconhecido no nível do programa gera erro (sem skip silencioso)', () => {
  assert.throws(
    () => analisar('PROGRAMA X\nFOO Bar\nFIM_PROGRAMA'),
    /\[Erro Sintático\]\[Linha 2:1\]/,
  );
});

test('PROCEDIMENTO é suportado como declaração top-level', () => {
  const ast = analisar('PROGRAMA X\nPROCEDIMENTO Aux()\nINICIO\nFIM\nFIM_PROGRAMA');
  assert.equal(ast.procedimentos.length, 1);
  assert.equal(ast.procedimentos[0].nome, 'Aux');
});

test('ausência de FIM_PROGRAMA é erro explícito', () => {
  assert.throws(() => analisar('PROGRAMA X'), /FIM_PROGRAMA/);
});
