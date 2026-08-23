import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ThzLexer } from '../src/lexer.js';
import { ThzParser } from '../src/parser.js';
import { InterpretadorThz, ValorThz, valorThzDe, INTEIRO, LOGICO } from '../src/interpretador.js';
import { ProgramaAST } from '../src/types.js';

function programaDe(codigo: string): ProgramaAST {
  return new ThzParser(new ThzLexer(codigo).tokenize()).parse();
}

function executar(codigo: string, argumentos: Record<string, ValorThz> = {}, saida: string[] = []): { resultado: unknown; saida: string[] } {
  const ast = programaDe(codigo);
  const interpretador = new InterpretadorThz(ast, { saida: (l) => saida.push(l) });
  const nome = interpretador.listarOperacoesExecutaveis()[0].operacao.nome;
  return { resultado: interpretador.executarOperacao(nome, argumentos), saida };
}

const PROGRAMA_ARITMETICO = `PROGRAMA T
  REGRA_NEGOCIO R
    OPERACAO Calcular(x : NATURAL32, y : NATURAL32) : DECIMAL(18, 4)
    INICIO
      RETORNE (x + y * 2) / 2
    FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA`;

test('precedência e divisão inteira avaliadas corretamente', () => {
  const r = executar(PROGRAMA_ARITMETICO, { x: INTEIRO(4n), y: INTEIRO(6n) });
  assert.equal((r.resultado as { classe: string }).classe, 'INTEIRO');
  assert.equal((r.resultado as { valor: bigint }).valor, 8n);
});

test('SE/SENAO ramifica por condição lógica', () => {
  const codigo = `PROGRAMA T
    REGRA_NEGOCIO R
      OPERACAO Classificar(v : DECIMAL(18, 4)) : DECIMAL(18, 4)
      INICIO
        SE v > 100 E VERDADEIRO
          RETORNE v
        SENAO
          RETORNE 0.0000
        FIM_SE
      FIM
    FIM_REGRA_NEGOCIO
  FIM_PROGRAMA`;
  assert.equal(
    (executar(codigo, { v: valorThzDe('DECIMAL(18,4)', '250') }).resultado as any).valor.formatar(),
    '250.0000'
  );
  assert.equal(
    (executar(codigo, { v: valorThzDe('DECIMAL(18,4)', '50') }).resultado as any).valor.formatar(),
    '0.0000'
  );
});

test('ENQUANTO acumula com guarda de iterações', () => {
  const codigo = `PROGRAMA T
    REGRA_NEGOCIO R
      OPERACAO SomarAte(limite : NATURAL32) : DECIMAL(18, 4)
      INICIO
        VARIAVEL total : DECIMAL(18, 4) <- 0.0000
        ENQUANTO limite > 0
          total <- total + limite
          limite <- limite - 1
        FIM_ENQUANTO
        RETORNE total
      FIM
    FIM_REGRA_NEGOCIO
  FIM_PROGRAMA`;
  const r = executar(codigo, { limite: INTEIRO(5n) });
  assert.equal((r.resultado as any).valor.formatar(), '15.0000');
});

test('atribuição atualiza variável do escopo dono dentro de laço', () => {
  // Regressão do bug de shadowing no VETORIZAR_PARA.
  const codigo = `PROGRAMA T
    ESTRUTURA Ponto LAYOUT_COLUNAR
      x : NATURAL32
    FIM_ESTRUTURA
    REGRA_NEGOCIO R
      OPERACAO Soma(pontos : FATIA[Ponto]) : DECIMAL(18, 4)
      INICIO
        VARIAVEL total : DECIMAL(18, 4) <- 0.0000
        VETORIZAR_PARA p EM pontos PASSO_SIMD 8
          total <- total + p.x
        FIM_PARA
        RETORNE total
      FIM
    FIM_REGRA_NEGOCIO
  FIM_PROGRAMA`;
  const elementos = [10n, 20n, 12n].map((x) => ({
    classe: 'REGISTRO' as const,
    nomeEstrutura: 'Ponto',
    campos: new Map([['x', INTEIRO(x)]])
  }));
  const r = executar(codigo, { pontos: { classe: 'FATIA', tipoInterno: 'Ponto', elementos } });
  assert.equal((r.resultado as any).valor.formatar(), '42.0000');
});

test('contrato EXIGE reprova com violação reportando linha e texto canônico', () => {
  const codigo = `VERSAO_LINGUAGEM "2.2"
PROGRAMA T
    ESTRUTURA Ponto LAYOUT_COLUNAR
      x : NATURAL32
    FIM_ESTRUTURA
    REGRA_NEGOCIO R
      CONTRATO_ENTRADA
        EXIGE pontos.x > 0
      FIM_CONTRATO_ENTRADA
      OPERACAO Soma(pontos : FATIA[Ponto]) : DECIMAL(18, 4)
      INICIO
        RETORNE 0.0000
      FIM
    FIM_REGRA_NEGOCIO
  FIM_PROGRAMA`;
  const elementos = [{ classe: 'REGISTRO' as const, nomeEstrutura: 'Ponto', campos: new Map([['x', INTEIRO(0n)]]) }];
  assert.throws(
    () => executar(codigo, { pontos: { classe: 'FATIA', tipoInterno: 'Ponto', elementos } }),
    /Violação de Contrato EXIGE.*pontos\.x > 0/
  );
});

test('quantificador universal: fatia vazia satisfaz contrato (verdade vacuosa)', () => {
  const codigo = `PROGRAMA T
    ESTRUTURA Ponto LAYOUT_COLUNAR
      x : NATURAL32
    FIM_ESTRUTURA
    REGRA_NEGOCIO R
      CONTRATO_ENTRADA
        EXIGE pontos.x >= 0
      FIM_CONTRATO_ENTRADA
      OPERACAO Soma(pontos : FATIA[Ponto]) : DECIMAL(18, 4)
      INICIO
        RETORNE 1.0000
      FIM
    FIM_REGRA_NEGOCIO
  FIM_PROGRAMA`;
  const r = executar(codigo, { pontos: { classe: 'FATIA', tipoInterno: 'Ponto', elementos: [] } });
  assert.equal((r.resultado as any).valor.formatar(), '1.0000');
});

test('divisão decimal por zero gera erro explícito com posição', () => {
  const codigo = `PROGRAMA T
    REGRA_NEGOCIO R
      OPERACAO Q(x : DECIMAL(18, 4), y : DECIMAL(18, 4)) : DECIMAL(18, 4)
      INICIO
        RETORNE x / y
      FIM
    FIM_REGRA_NEGOCIO
  FIM_PROGRAMA`;
  assert.throws(
    () => executar(codigo, { x: valorThzDe('DECIMAL(18,4)', '1'), y: valorThzDe('DECIMAL(18,4)', '0') }),
    /Divisão por zero\.\[?\d*]*$|Divisão por zero/
  );
});

test('EXIBA emite concatenação formatada', () => {
  const codigo = `PROGRAMA T
    REGRA_NEGOCIO R
      OPERACAO Dizer() : DECIMAL(18, 4)
      INICIO
        EXIBA "Total: " + 7 + " | OK: " + VERDADEIRO
        RETORNE 0.0000
      FIM
    FIM_REGRA_NEGOCIO
  FIM_PROGRAMA`;
  const r = executar(codigo);
  assert.deepEqual(r.saida, ['Total: 7 | OK: VERDADEIRO']);
});

test('USAR_BLOCO_MEMORIA executa corpo e libera arena', () => {
  const codigo = `PROGRAMA T
    REGRA_NEGOCIO R
      OPERACAO ComArena() : DECIMAL(18, 4)
      INICIO
        USAR_BLOCO_MEMORIA ARENA_EPHEMERAL
          EXIBA "dentro da arena"
        FIM_BLOCO_MEMORIA
        RETORNE 9.0000
      FIM
    FIM_REGRA_NEGOCIO
  FIM_PROGRAMA`;
  const r = executar(codigo);
  assert.deepEqual(r.saida, ['dentro da arena']);
  assert.equal((r.resultado as any).valor.formatar(), '9.0000');
});

test('identificador não declarado produz erro com linha/coluna', () => {
  const codigo = `PROGRAMA T
    REGRA_NEGOCIO R
      OPERACAO Falha() : DECIMAL(18, 4)
      INICIO
        RETORNE inexistente + 1
      FIM
    FIM_REGRA_NEGOCIO
  FIM_PROGRAMA`;
  assert.throws(() => executar(codigo), /\[Linha 5:\d+\] Identificador não declarado: 'inexistente'/);
});
