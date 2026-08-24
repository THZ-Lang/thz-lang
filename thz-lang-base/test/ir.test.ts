import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'fs';
import path from 'path';
import { ThzLexer } from '../src/lexer.js';
import { ThzParser } from '../src/parser.js';
import { baixarParaIr, serializarIr, emitirLlvm, VERSAO_IR } from '../src/ir.js';

function irDe(fonte: string) {
  const ast = new ThzParser(new ThzLexer(fonte).tokenize()).parse();
  return baixarParaIr(ast);
}

test('IR — versão estável thz-ir/1', () => {
  const ir = irDe('PROGRAMA X\nFIM_PROGRAMA');
  assert.equal(ir.versaoIr, VERSAO_IR);
  assert.equal(ir.versaoIr, 'thz-ir/1');
});

test('IR — estruturas SoA vs AoS', () => {
  const ir = irDe(`
PROGRAMA P
ESTRUTURA A LAYOUT_COLUNAR
  q : NATURAL32
FIM_ESTRUTURA
ESTRUTURA B
  x : TEXTO
FIM_ESTRUTURA
FIM_PROGRAMA`.trim());
  const a = ir.estruturas.find((e) => e.nome === 'A')!;
  const b = ir.estruturas.find((e) => e.nome === 'B')!;
  assert.equal(a.layout, 'SoA');
  assert.equal(b.layout, 'AoS');
  assert.equal(a.campos[0].tipoIr, 'NATURAL32');
});

test('IR — invariantes preservadas', () => {
  const ir = irDe(`
PROGRAMA P
ESTRUTURA Item
  v : DECIMAL(12,4)
  INVARIANTE v >= 0.0000
FIM_ESTRUTURA
FIM_PROGRAMA`.trim());
  assert.equal(ir.estruturas[0].invariantes.length, 1);
  assert.equal(ir.estruturas[0].invariantes[0].textoCanonico, 'v >= 0.0000');
});

test('IR — funcoes com contratos', () => {
  const ir = irDe(`
PROGRAMA P
ESTRUTURA Item
  q : NATURAL32
FIM_ESTRUTURA
REGRA_NEGOCIO R
  IDENTIFICADOR_REGRA: "BR-001"
  RASTREIO_REQUISITO: "REQ-001"
  CONTRATO_ENTRADA EXIGE itens.q > 0 FIM_CONTRATO_ENTRADA
  CONTRATO_SAIDA GARANTE itens.q >= 0 FIM_CONTRATO_SAIDA
  OPERACAO Op(itens: FATIA[Item]) : DECIMAL(18,4)
  INICIO RETORNE 0.0 FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA`.trim());
  assert.equal(ir.funcoes.length, 1);
  assert.equal(ir.funcoes[0].nomeQualificado, 'R.Op');
  assert.equal(ir.funcoes[0].contratos.textosExige[0], 'itens.q > 0');
  assert.equal(ir.funcoes[0].tipoRetornoIr, 'DECIMAL(18,4)');
});

test('IR — vetorizado verificado com layout SoA e passo', () => {
  const ir = irDe(`
PROGRAMA P
ESTRUTURA Item LAYOUT_COLUNAR
  q : NATURAL32
  v : DECIMAL(12,4)
FIM_ESTRUTURA
REGRA_NEGOCIO R
  OPERACAO Somar(itens: FATIA[Item]) : DECIMAL(18,4)
  INICIO
    VARIAVEL acc : DECIMAL(18,4) <- 0.0000
    VETORIZAR_PARA item EM itens PASSO_SIMD 8
      acc <- acc + item.q * item.v
    FIM_PARA
    RETORNE acc
  FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA`.trim());
  assert.equal(ir.diagnosticosSimd.length, 1);
  assert.equal(ir.diagnosticosSimd[0].verificado, true);
  const instr = ir.funcoes[0].corpo.find((i: any) => i.kind === 'vetorizado') as any;
  assert.equal(instr.verificado, true);
  assert.equal(instr.passoEfetivo, 8);
  assert.equal(instr.layoutFonte, 'SoA');
});

test('IR — vetorizado com passo inválido gera diagnóstico', () => {
  const ir = irDe(`
PROGRAMA P
ESTRUTURA Item LAYOUT_COLUNAR
  q : NATURAL32
FIM_ESTRUTURA
REGRA_NEGOCIO R
  OPERACAO Op(itens: FATIA[Item]) : DECIMAL(18,4)
  INICIO
    VETORIZAR_PARA item EM itens PASSO_SIMD 7
      EXIBA item.q
    FIM_PARA
    RETORNE 0.0
  FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA`.trim());
  assert.equal(ir.diagnosticosSimd[0].verificado, false);
  assert.ok(ir.diagnosticosSimd[0].diagnosticos.some((d: string) => d.includes('R3')));
});

test('IR — golden faturamento produz IR verificável', () => {
  const fonte = fs.readFileSync(path.join(process.cwd(), 'exemplos', 'faturamento.thz'), 'utf8');
  const ir = irDe(fonte);
  assert.equal(ir.programa, 'ProcessamentoFaturamentoLote');
  assert.equal(ir.estruturas[0].layout, 'SoA');
  assert.equal(ir.funcoes[0].parametros[0].tipoIr, 'fatiaslice<ItemFatura>');
  assert.equal(ir.diagnosticosSimd[0].verificado, true);
  const json = serializarIr(ir);
  assert.match(json, /thz-ir\/1/);
  assert.match(json, /ProcessamentoFaturamentoLote/);
});

test('IR — emitirLlvm contém SoA, contratos e vector.body', () => {
  const fonte = fs.readFileSync(path.join(process.cwd(), 'exemplos', 'faturamento.thz'), 'utf8');
  const ir = irDe(fonte);
  const llvm = emitirLlvm(ir);
  assert.match(llvm, /THZ-IR thz-ir\/1/);
  assert.match(llvm, /SoA \(Structure of Arrays/);
  assert.match(llvm, /define .* @CalculoTributarioLote_ProcessarVetorizado/);
  assert.match(llvm, /EXIGE/);
  assert.match(llvm, /vector\.body/);
  assert.match(llvm, /declare ptr @thz\.arena\.alocar/);
});

test('IR — serializarIr é determinístico e ordenado', () => {
  const ir1 = irDe('PROGRAMA A\nFIM_PROGRAMA');
  const ir2 = irDe('PROGRAMA A\nFIM_PROGRAMA');
  assert.equal(serializarIr(ir1), serializarIr(ir2));
});
