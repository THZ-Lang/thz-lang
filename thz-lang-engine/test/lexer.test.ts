import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ThzLexer, ErroLexico } from '../src/lexer.js';
import { TokenType } from '../src/types.js';

test('cabeçalho de programa tokeniza com posições corretas', () => {
  const tokens = new ThzLexer('PROGRAMA Demo\nFIM_PROGRAMA').tokenize();
  assert.equal(tokens[0].type, TokenType.PROGRAMA);
  assert.equal(tokens[0].line, 1);
  assert.equal(tokens[0].column, 1);
  assert.deepEqual([tokens[1].type, tokens[1].value], [TokenType.IDENTIFICADOR, 'Demo']);
  assert.equal(tokens[2].type, TokenType.FIM_PROGRAMA);
  assert.equal(tokens[3].type, TokenType.EOF);
});

test('seta de atribuição é atômica e distinta do relacional menor', () => {
  const tokens = new ThzLexer('total <- a < b').tokenize();
  assert.equal(tokens[1].type, TokenType.SETA_ATRIBUICAO);
  assert.equal(tokens[1].value, '<-');
  assert.equal(tokens[3].type, TokenType.OPERADOR_RELACIONAL);
  assert.equal(tokens[3].value, '<');
});

test('operadores relacionais compostos são reconhecidos', () => {
  const tokens = new ThzLexer('a <= b >= c <> d = e').tokenize();
  const ops = tokens.filter((t) => t.type === TokenType.OPERADOR_RELACIONAL).map((t) => t.value);
  assert.deepEqual(ops, ['<=', '>=', '<>', '=']);
});

test('módulo % é aritmético', () => {
  const tokens = new ThzLexer('10 % 3').tokenize();
  assert.equal(tokens[1].type, TokenType.OPERADOR_ARITMETICO);
  assert.equal(tokens[1].value, '%');
});

test('conectivos lógicos verbais viram OPERADOR_LOGICO', () => {
  const tokens = new ThzLexer('a E b OU NAO c').tokenize();
  const logicos = tokens.filter((t) => t.type === TokenType.OPERADOR_LOGICO).map((t) => t.value);
  assert.deepEqual(logicos, ['E', 'OU', 'NAO']);
});

test('números com separador de milhar e casas decimais', () => {
  const tokens = new ThzLexer('1_250.5000').tokenize();
  assert.equal(tokens[0].type, TokenType.NUMERO_LITERAL);
  assert.equal(tokens[0].value, '1250.5000');
});

test('ponto após número não digerido como acesso a campo', () => {
  const tokens = new ThzLexer('12.x').tokenize();
  assert.deepEqual(
    tokens.map((t) => t.type),
    [TokenType.NUMERO_LITERAL, TokenType.PONTO, TokenType.IDENTIFICADOR, TokenType.EOF]
  );
});

test('comentários de linha são ignorados', () => {
  const tokens = new ThzLexer('# isto é um comentário\nPROGRAMA X # trailing\nFIM_PROGRAMA').tokenize();
  assert.ok(!tokens.some((t) => t.value.includes('comentário')));
  assert.equal(tokens.filter((t) => t.type === TokenType.PROGRAMA).length, 1);
});

test('palavra reservada nunca vira identificador (política estrita)', () => {
  const tokens = new ThzLexer('ESTRUTURA EM').tokenize();
  assert.equal(tokens[0].type, TokenType.ESTRUTURA);
  assert.equal(tokens[1].type, TokenType.EM, "'EM' deve ser token reservado, não IDENTIFICADOR");
});

test('caractere desconhecido gera ErroLexico com linha e coluna', () => {
  assert.throws(() => new ThzLexer('a b @').tokenize(), (e: unknown) => {
    assert.ok(e instanceof ErroLexico);
    assert.match(e.message, /\[Erro Léxico\]\[Linha 1:5\]/);
    assert.match(e.message, /@/);
    return true;
  });
});

test('erro léxico reporta linha correta em arquivo multilinha', () => {
  assert.throws(() => new ThzLexer('linha1\nlinha2\n   §').tokenize(), /Linha 3:4/);
});

test('string não terminada gera erro explícito', () => {
  assert.throws(() => new ThzLexer('"texto sem fechamento').tokenize(), /Literal de texto não terminado/);
});

test('escape \\n dentro de string é convertido', () => {
  const tokens = new ThzLexer('"linha1\\nlinha2"').tokenize();
  assert.equal(tokens[0].value, 'linha1\nlinha2');
});
