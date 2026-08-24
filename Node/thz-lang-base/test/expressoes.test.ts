import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ThzLexer } from '../src/lexer.js';
import { ThzParser } from '../src/parser.js';
import { ExprAST } from '../src/types.js';
import { DecimalFixo } from '../src/runtime.js';

function exprDe(expressao: string): ExprAST {
  const tokens = new ThzLexer(expressao).tokenize();
  const parser = new ThzParser(tokens);
  return parser.parseExpressao();
}

function avaliar(expressao: string): unknown {
  const expr = exprDe(expressao);
  // Avaliação direta sem interpretador completo: usa apenas literais.
  switch (expr.tipo) {
    case 'LITERAL_INTEIRO': return expr.valor;
    case 'LITERAL_DECIMAL': return { escalado: expr.escalado, escala: expr.escala };
    case 'LITERAL_TEXTO': return expr.valor;
    case 'LITERAL_LOGICO': return expr.valor;
    default: throw new Error('Teste espera literal direto');
  }
}

test('literal inteiro preserva precisão arbitrária via BigInt', () => {
  assert.equal(avaliar('9007199254740993'), 9007199254740993n);
});

test('literal decimal é armazenado escalado (ISO/IEC 10967)', () => {
  const d = avaliar('150.5000') as { escalado: bigint; escala: number };
  assert.equal(d.escalado, 1505000n);
  assert.equal(d.escala, 4);
});

test('literais verbais lógicos e nulo', () => {
  assert.equal(avaliar('VERDADEIRO'), true);
  assert.equal(avaliar('FALSO'), false);
});

test('precedência multiplicativa sobre aditiva', () => {
  // 2 + 3 * 4 => OP_BINARIA(+, 2, OP_BINARIA(*, 3, 4))
  const e = exprDe('2 + 3 * 4') as Extract<ExprAST, { tipo: 'OP_BINARIA' }>;
  assert.equal(e.operador, '+');
  assert.equal((e.direita as Extract<ExprAST, { tipo: 'OP_BINARIA' }>).operador, '*');
});

test('parênteses alteram a árvore', () => {
  const e = exprDe('(2 + 3) * 4') as Extract<ExprAST, { tipo: 'OP_BINARIA' }>;
  assert.equal(e.operador, '*');
  assert.equal((e.esquerda as Extract<ExprAST, { tipo: 'OP_BINARIA' }>).operador, '+');
});

test('precedência relacional abaixo de aritmética', () => {
  const e = exprDe('a + 1 > b * 2') as Extract<ExprAST, { tipo: 'OP_BINARIA' }>;
  assert.equal(e.operador, '>');
});

test("conectivos 'E'/'OU' com precedência correta (OU no topo)", () => {
  const e = exprDe('a OU b E c') as Extract<ExprAST, { tipo: 'OP_BINARIA' }>;
  assert.equal(e.operador, 'OU');
  assert.equal((e.direita as Extract<ExprAST, { tipo: 'OP_BINARIA' }>).operador, 'E');
});

test('negação unária e lógica', () => {
  assert.equal((exprDe('-5') as Extract<ExprAST, { tipo: 'OP_UNARIA' }>).operador, '-');
  assert.equal((exprDe('NAO x') as Extract<ExprAST, { tipo: 'OP_UNARIA' }>).operador, 'NAO');
});

test('acesso qualificado por ponto gera caminho completo', () => {
  const e = exprDe('item.valor_total_liquido') as Extract<ExprAST, { tipo: 'ACESSO' }>;
  assert.deepEqual(e.caminho, ['item', 'valor_total_liquido']);
});

test('contratos do exemplo canônico parseiam como árvores avaliáveis', () => {
  const codigo = `VERSAO_LINGUAGEM "2.2"
PROGRAMA T
  REGRA_NEGOCIO R
    CONTRATO_ENTRADA
      EXIGE itens.quantidade > 0
    FIM_CONTRATO_ENTRADA
  FIM_REGRA_NEGOCIO
FIM_PROGRAMA`;
  const ast = new ThzParser(new ThzLexer(codigo).tokenize()).parse();
  const clausula = ast.regras[0].clausulasEntrada[0];
  assert.equal(clausula.expressao.tipo, 'OP_BINARIA');
  assert.equal(clausula.textoCanonico, 'itens.quantidade > 0');
});

test('texto canônico de decimal preserva casas', () => {
  const codigo = `PROGRAMA T
  REGRA_NEGOCIO R
    CONTRATO_SAIDA
      GARANTE x >= 0.0000
    FIM_CONTRATO_SAIDA
  FIM_REGRA_NEGOCIO
FIM_PROGRAMA`;
  const ast = new ThzParser(new ThzLexer(codigo).tokenize()).parse();
  assert.equal(ast.regras[0].clausulasSaida[0].textoCanonico, 'x >= 0.0000');
});

test('DecimalFixo corrige formatação de negativos', () => {
  assert.equal(DecimalFixo.deTexto('-0.5', 4).formatar(), '-0.5000');
  assert.equal(DecimalFixo.deTexto('-1.25', 4).formatar(), '-1.2500');
});
