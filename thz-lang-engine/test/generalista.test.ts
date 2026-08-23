import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { ThzLexer } from '../src/lexer.js';
import { ThzParser } from '../src/parser.js';
import { AnalisadorSemantico } from '../src/analisador.js';
import { InterpretadorThz } from '../src/interpretador.js';

function analisar(codigo: string) {
  const tokens = new ThzLexer(codigo).tokenize();
  const ast = new ThzParser(tokens).parse();
  const erros = new AnalisadorSemantico(ast).analisar();
  return { ast, erros };
}
function rodar(codigo: string, entrada?: string[]) {
  const { ast, erros } = analisar(codigo);
  assert.equal(erros.length, 0, 'semântica deve passar: ' + JSON.stringify(erros));
  let idx = 0;
  const saidas: string[] = [];
  const interp = new InterpretadorThz(ast, {
    saida: (l) => saidas.push(l),
    entrada: entrada ? () => entrada[idx++] ?? null : undefined
  });
  // Se existe Principal, executa; senão tenta primeira operação
  const procs = (ast.procedimentos ?? []).find(p => p.nome === 'Principal');
  if (procs) interp.executarProcedimento('Principal', {});
  else {
    const ops = interp.listarOperacoesExecutaveis();
    if (ops.length) interp.executarOperacao(ops[0].operacao.nome, {});
  }
  return { ast, saidas, interp };
}

describe('THZ v2.3 Generalista', () => {
  test('chamada stdlib TEXTO e MATEMATICA', () => {
    const { saidas } = rodar(`
VERSAO_LINGUAGEM "2.3"
PROGRAMA Demo
ESTRUTURA X
    v : TEXTO
FIM_ESTRUTURA
PROCEDIMENTO Principal()
INICIO
    VARIAVEL t : TEXTO <- "  ola "
    EXIBA TEXTO.aparar(t)
    EXIBA TEXTO.maiusculas("abc")
    VARIAVEL a : INTEIRO32 <- 5
    VARIAVEL b : INTEIRO32 <- 3
    EXIBA MATEMATICA.min(a, b)
    EXIBA MATEMATICA.max(a, b)
FIM
FIM_PROGRAMA`);
    assert.deepEqual(saidas, ['ola', 'ABC', '3', '5']);
  });

  test('fatia literal e indexação', () => {
    const { saidas } = rodar(`
VERSAO_LINGUAGEM "2.3"
PROGRAMA Demo
PROCEDIMENTO Principal()
INICIO
    VARIAVEL lista : FATIA[TEXTO] <- ["a", "b", "c"]
    EXIBA lista[1]
    VARIAVEL t : TEXTO <- "THZ"
    EXIBA t[1]
FIM
FIM_PROGRAMA`);
    assert.deepEqual(saidas, ['b', 'H']);
  });

  test('CRIAR com invariante válido e violação', () => {
    const codigoValido = `
VERSAO_LINGUAGEM "2.3"
PROGRAMA Demo
ESTRUTURA P
    nome : TEXTO
    INVARIANTE nome <> ""
FIM_ESTRUTURA
PROCEDIMENTO Principal()
INICIO
    VARIAVEL p : P <- CRIAR P(nome: "ok")
    EXIBA p.nome
FIM
FIM_PROGRAMA`;
    const { saidas } = rodar(codigoValido);
    assert.deepEqual(saidas, ['ok']);

    const codigoInvalido = `
VERSAO_LINGUAGEM "2.3"
PROGRAMA Demo
ESTRUTURA P
    nome : TEXTO
    INVARIANTE nome <> ""
FIM_ESTRUTURA
PROCEDIMENTO Principal()
INICIO
    VARIAVEL p : P <- CRIAR P(nome: "")
FIM
FIM_PROGRAMA`;
    const { ast } = analisar(codigoInvalido);
    const interp = new InterpretadorThz(ast, { saida: () => {} });
    assert.throws(() => interp.executarProcedimento('Principal'), /Invariante/);
  });

  test('PARA DE ATE com PASSO', () => {
    const { saidas } = rodar(`
VERSAO_LINGUAGEM "2.3"
PROGRAMA Demo
PROCEDIMENTO Principal()
INICIO
    VARIAVEL soma : INTEIRO32 <- 0
    PARA i DE 1 ATE 5
        soma <- soma + i
    FIM_PARA
    EXIBA soma
    VARIAVEL soma2 : INTEIRO32 <- 0
    PARA j DE 10 ATE 0 PASSO -2
        soma2 <- soma2 + j
    FIM_PARA
    EXIBA soma2
FIM
FIM_PROGRAMA`);
    assert.deepEqual(saidas, ['15', '30']);
  });

  test('DATA criação, comparação e stdlib', () => {
    const { saidas } = rodar(`
VERSAO_LINGUAGEM "2.3"
PROGRAMA Demo
PROCEDIMENTO Principal()
INICIO
    VARIAVEL d1 : DATA <- DATA.criar(2026, 1, 1)
    VARIAVEL d2 : DATA <- DATA.criar(2026, 12, 31)
    EXIBA DATA.texto(d1)
    EXIBA DATA.diferencaDias(d2, d1)
    SE d1 < d2
        EXIBA "antes"
    FIM_SE
    VARIAVEL dh : DATA_HORA <- DATA.criarDataHora(2026, 8, 23, 14, 30)
    EXIBA DATA.texto(dh)
FIM
FIM_PROGRAMA`);
    assert.equal(saidas[0], '2026-01-01');
    assert.equal(saidas[1], '364');
    assert.equal(saidas[2], 'antes');
    assert.equal(saidas[3], '2026-08-23T14:30');
  });

  test('LER com entrada injetada', () => {
    const { saidas } = rodar(`
VERSAO_LINGUAGEM "2.3"
PROGRAMA Demo
PROCEDIMENTO Principal()
INICIO
    VARIAVEL nome : TEXTO <- ""
    EXIBA "Qual seu nome?"
    LER nome
    EXIBA "Ola, " + nome
FIM
FIM_PROGRAMA`, ['Mundo']);
    assert.deepEqual(saidas, ['Qual seu nome?', 'Ola, Mundo']);
  });

  test('procedimento chamado como função', () => {
    const { saidas } = rodar(`
VERSAO_LINGUAGEM "2.3"
PROGRAMA Demo
PROCEDIMENTO Saudacao(nome : TEXTO)
INICIO
    EXIBA "Oi " + nome
FIM
PROCEDIMENTO Principal()
INICIO
    Saudacao("THZ")
FIM
FIM_PROGRAMA`);
    assert.deepEqual(saidas, ['Oi THZ']);
  });

  test('analisador detecta chamada desconhecida e aridade', () => {
    const { erros } = analisar(`
VERSAO_LINGUAGEM "2.3"
PROGRAMA Demo
PROCEDIMENTO Principal()
INICIO
    VARIAVEL x : TEXTO <- FOO("a")
FIM
FIM_PROGRAMA`);
    assert.ok(erros.some(e => /Chamada desconhecida/.test(e.mensagem)));

    const { erros: e2 } = analisar(`
VERSAO_LINGUAGEM "2.3"
PROGRAMA Demo
PROCEDIMENTO Principal()
INICIO
    EXIBA TEXTO.aparar()
FIM
FIM_PROGRAMA`);
    assert.ok(e2.some(e => /exige 1 arg/.test(e.mensagem)));
  });

  test('indexação fora de limites gera erro de execução', () => {
    const { ast } = analisar(`
VERSAO_LINGUAGEM "2.3"
PROGRAMA Demo
PROCEDIMENTO Principal()
INICIO
    VARIAVEL lista : FATIA[TEXTO] <- ["a"]
    EXIBA lista[5]
FIM
FIM_PROGRAMA`);
    const interp = new InterpretadorThz(ast, { saida: () => {} });
    assert.throws(() => interp.executarProcedimento('Principal'), /fora da fatia/);
  });

  test('agenda exemplo roda', () => {
    const src = fs.readFileSync('exemplos/agenda.thz','utf8');
    const { ast, erros } = analisar(src);
    assert.equal(erros.length, 0);
    const saidas: string[] = [];
    const interp = new InterpretadorThz(ast, { saida: (l) => saidas.push(l) });
    interp.executarProcedimento('Principal');
    assert.ok(saidas.some(s => s.includes('Agenda')));
    assert.ok(saidas.some(s => s.includes('Reuniao')));
  });
});
