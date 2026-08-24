import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  PALAVRAS_RESERVADAS,
  ehPalavraReservada,
  tokenDe,
  categoriaDe,
  palavrasPorCategoria,
  CategoriaPalavra,
  VERSAO_LINGUAGEM_ATUAL
} from '../src/keywords.js';
import { TokenType } from '../src/types.js';

test('palavras estruturais são reservadas com categoria correta', () => {
  assert.equal(categoriaDe('PROGRAMA'), CategoriaPalavra.DECLARACAO);
  assert.equal(categoriaDe('FIM_PROGRAMA'), CategoriaPalavra.FIM_BLOCO);
  assert.equal(categoriaDe('EXIGE'), CategoriaPalavra.CONTRATO);
  assert.equal(categoriaDe('SE'), CategoriaPalavra.CONTROLE);
  assert.equal(categoriaDe('USAR_BLOCO_MEMORIA'), CategoriaPalavra.MEMORIA);
  assert.equal(categoriaDe('LAYOUT_COLUNAR'), CategoriaPalavra.MODIFICADOR);
});

test('literais verbais e conectivos mapeiam para tokens corretos', () => {
  assert.equal(tokenDe('VERDADEIRO'), TokenType.VERDADEIRO);
  assert.equal(tokenDe('FALSO'), TokenType.FALSO);
  assert.equal(tokenDe('NULO'), TokenType.NULO);
  assert.equal(tokenDe('E'), TokenType.OPERADOR_LOGICO);
  assert.equal(tokenDe('OU'), TokenType.OPERADOR_LOGICO);
  assert.equal(tokenDe('NAO'), TokenType.OPERADOR_LOGICO);
});

test('identificadores comuns não são reservados', () => {
  assert.equal(ehPalavraReservada('faturamento'), false);
  assert.equal(ehPalavraReservada('valor_total_liquido'), false);
  assert.equal(ehPalavraReservada('DECIMAL'), false, 'nomes de tipos são validados pela análise semântica, não pelo léxico');
  assert.equal(ehPalavraReservada('IDENTIFICADOR_REGRA'), false, 'chaves contextuais não são reservadas');
});

test('novas palavras da v2.2 estão registradas', () => {
  for (const p of ['SE', 'SENAO', 'ENQUANTO', 'FIM_SE', 'FIM_ENQUANTO', 'VERSAO_LINGUAGEM']) {
    assert.ok(ehPalavraReservada(p), p + ' deve ser reservada');
  }
});

test('tabela não contém duplicatas nem entradas vazias', () => {
  const chaves = Object.keys(PALAVRAS_RESERVADAS);
  assert.equal(new Set(chaves).size, chaves.length);
  for (const k of chaves) {
    assert.ok(k.length > 0);
    assert.ok(Object.values(TokenType).includes(PALAVRAS_RESERVADAS[k].token));
  }
});

test('versão corrente segue semver major.minor', () => {
  assert.match(VERSAO_LINGUAGEM_ATUAL, /^\d+\.\d+\.\d+$/);
});
