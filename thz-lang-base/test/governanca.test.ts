import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'fs';
import path from 'path';
import { ThzLexer } from '../src/lexer.js';
import { ThzParser } from '../src/parser.js';
import { auditar, gerarMarkdownGovernanca } from '../src/governanca.js';

function auditarFonte(fonte: string, estrito = false) {
  const ast = new ThzParser(new ThzLexer(fonte).tokenize()).parse();
  return auditar(ast, { estrito });
}

const FAT = fs.readFileSync(path.join(process.cwd(), 'exemplos', 'faturamento.thz'), 'utf8');
const PED = fs.readFileSync(path.join(process.cwd(), 'exemplos', 'pedidos.thz'), 'utf8');

test('Governança — faturamento auditado aprovado', () => {
  const a = auditarFonte(FAT, true);
  assert.equal(a.aprovada, true);
  assert.equal(a.metricas.totalRegras, 1);
  assert.equal(a.metricas.totalExige, 2);
  assert.equal(a.metricas.totalGarante, 1);
  assert.equal(a.metricas.totalInvariantes, 1);
  assert.equal(a.regras[0].rastreio, 'REQ-FISCAL-9102');
  assert.equal(a.regras[0].status, 'ok');
});

test('Governança — pedidos auditado aprovado', () => {
  const a = auditarFonte(PED, true);
  assert.equal(a.aprovada, true);
  assert.equal(a.metricas.comRastreio, 1);
  assert.equal(a.regras[0].identificador, 'BR-VENDAS-2026-01');
});

test('Governança — regra sem rastreio é pendência no estrito', () => {
  const fonte = `
PROGRAMA X
ESTRUTURA Item
  q : NATURAL32
FIM_ESTRUTURA
REGRA_NEGOCIO R
  OPERACAO Op(itens: FATIA[Item]) : DECIMAL(18,4)
  INICIO RETORNE 0.0 FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA`.trim();
  const semEstrito = auditarFonte(fonte, false);
  assert.equal(semEstrito.pendencias.length, 0); // não-estrito não coleta pendências de regra (apenas coverage)
  assert.equal(semEstrito.metricas.coberturaRastreio, 0);
  const comEstrito = auditarFonte(fonte, true);
  assert.ok(comEstrito.pendencias.some((p) => /RASTREIO_REQUISITO/.test(p)));
  assert.equal(comEstrito.aprovada, false);
  assert.equal(comEstrito.regras[0].status, 'reprovado');
});

test('Governança — rastreio duplicado é detectado', () => {
  const fonte = `
VERSAO_LINGUAGEM "2.2"
PROGRAMA X
ESTRUTURA Item
  q : NATURAL32
FIM_ESTRUTURA
REGRA_NEGOCIO A
  IDENTIFICADOR_REGRA: "BR-001"
  RASTREIO_REQUISITO: "REQ-001"
  CONTRATO_ENTRADA EXIGE itens.q > 0 FIM_CONTRATO_ENTRADA
  OPERACAO Op(itens: FATIA[Item]) : DECIMAL(18,4)
  INICIO RETORNE 0.0 FIM
FIM_REGRA_NEGOCIO
REGRA_NEGOCIO B
  IDENTIFICADOR_REGRA: "BR-002"
  RASTREIO_REQUISITO: "REQ-001"
  CONTRATO_ENTRADA EXIGE itens.q > 0 FIM_CONTRATO_ENTRADA
  OPERACAO Op2(itens: FATIA[Item]) : DECIMAL(18,4)
  INICIO RETORNE 0.0 FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA`.trim();
  const a = auditarFonte(fonte, true);
  assert.ok(a.pendencias.some((p) => /duplicado/.test(p)));
  assert.equal(a.regras[1].status, 'reprovado');
});

test('Governança — cobertura e invariantes vinculadas', () => {
  const fonte = `
VERSAO_LINGUAGEM "2.2"
PROGRAMA X
METADADOS_ARQUITETURA
  DOMINIO: "D"
  SLO_LATENCIA_MAXIMA: "10ms"
FIM_METADADOS
ESTRUTURA Item
  q : NATURAL32
  INVARIANTE q > 0
FIM_ESTRUTURA
REGRA_NEGOCIO R
  IDENTIFICADOR_REGRA: "BR-001"
  RASTREIO_REQUISITO: "REQ-001"
  DESCRICAO: "ok"
  CONTRATO_ENTRADA EXIGE itens.q > 0 FIM_CONTRATO_ENTRADA
  OPERACAO Op(itens: FATIA[Item]) : DECIMAL(18,4)
  INICIO RETORNE 0.0 FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA`.trim();
  const a = auditarFonte(fonte, true);
  assert.equal(a.regras[0].invariantesVinculadas, 1);
  assert.equal(a.metricas.coberturaContrato, 1);
  assert.equal(a.aprovada, true);
});

test('Governança — gerarMarkdownGovernanca contém seções', () => {
  const a = auditarFonte(FAT, false);
  const md = gerarMarkdownGovernanca(a);
  assert.match(md, /Relatório de Governança/);
  assert.match(md, /Matriz de Rastreabilidade/);
  assert.match(md, /Mermaid/);
  assert.match(md, /REQ-FISCAL-9102/);
});
