import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'fs';
import path from 'path';
import {
  analisar,
  obterHover,
  posicaoParaOffset,
  offsetParaPosicao,
  tokenNoCursor,
} from '../src/language-service.js';

const PROGRAMA_BASE = `
VERSAO_LINGUAGEM "2.2"
PROGRAMA Demo
ESTRUTURA Item LAYOUT_COLUNAR
  q   : NATURAL32
  v   : DECIMAL(12, 4)
FIM_ESTRUTURA
ENUMERACAO Status
  ATIVO
  CANCELADO
FIM_ENUMERACAO
REGRA_NEGOCIO R
  IDENTIFICADOR_REGRA: "BR-001"
  RASTREIO_REQUISITO: "REQ-001"
  CONTRATO_ENTRADA
    EXIGE itens.q > 0
  FIM_CONTRATO_ENTRADA
  OPERACAO Op(itens: FATIA[Item]) : DECIMAL(18, 4)
  INICIO
    VARIAVEL x : DECIMAL(10, 2) <- 5.00
    VARIAVEL y : TEXTO <- "oi"
    VETORIZAR_PARA item EM itens
      VARIAVEL bruto : DECIMAL(18, 4) <- item.q * item.v
      EXIBA bruto
    FIM_PARA
    RETORNE x
  FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA
`.trim();

// ------------------------------------------------------------------
// analyze — pipeline
// ------------------------------------------------------------------

test('language-service — programa válido: zero diagnósticos e AST presente', () => {
  const r = analisar(PROGRAMA_BASE);
  assert.equal(r.temErros, false);
  assert.equal(r.diagnosticos.length, 0);
  assert.ok(r.ast);
  assert.ok(r.tokens && r.tokens.length > 0);
  assert.equal(r.textoDiagnosticos.length, 0);
  assert.ok(r.simbolos.length >= 10);
});

test('language-service — exemplo canônico faturamento.thz aprovado', () => {
  const fonte = fs.readFileSync(path.join(process.cwd(), 'exemplos', 'faturamento.thz'), 'utf8');
  const r = analisar(fonte);
  assert.equal(r.temErros, false, JSON.stringify(r.diagnosticos, null, 2));
  assert.ok(r.simbolos.some((s) => s.nome === 'ItemFatura' && s.categoria === 'estrutura'));
});

test('language-service — erro léxico (caractere inválido)', () => {
  const fonte = 'PROGRAMA X\n@\nFIM_PROGRAMA';
  const r = analisar(fonte);
  assert.equal(r.temErros, true);
  assert.ok(r.diagnosticos.some((d) => d.origem === 'lexico'));
  assert.match(r.textoDiagnosticos[0], /\^/);
  assert.match(r.textoDiagnosticos[0], /@/);
});

test('language-service — erro sintático (FIM_ESTRUTURA ausente)', () => {
  const fonte = 'PROGRAMA X\nESTRUTURA A\n  x : TEXTO\nFIM_PROGRAMA';
  const r = analisar(fonte);
  assert.equal(r.temErros, true);
  assert.ok(r.diagnosticos.some((d) => d.origem === 'sintatico'));
  assert.match(r.textoDiagnosticos[0], /\^/);
});

test('language-service — erro semântico (tipo desconhecido)', () => {
  const fonte = 'PROGRAMA X\nESTRUTURA A\n  x : DESCONHECIDO\nFIM_ESTRUTURA\nFIM_PROGRAMA';
  const r = analisar(fonte);
  assert.equal(r.temErros, true);
  assert.ok(r.diagnosticos.some((d) => d.origem === 'semantico' && /Tipo desconhecido/.test(d.mensagem)));
  assert.match(r.textoDiagnosticos[0], /\^/);
});

test('language-service — diagnóstico semântico contém linha/coluna e caret', () => {
  const fonte = 'PROGRAMA X\nESTRUTURA A\n  x : DESCONHECIDO\nFIM_ESTRUTURA\nFIM_PROGRAMA';
  const r = analisar(fonte);
  const d = r.diagnosticos[0];
  assert.equal(typeof d.linha, 'number');
  assert.equal(typeof d.coluna, 'number');
  assert.equal(d.severidade, 'erro');
});

test('language-service — lint estrito reprova ausência de pragma', () => {
  const semPragma = 'PROGRAMA X\nFIM_PROGRAMA';
  const rNormal = analisar(semPragma, {});
  assert.equal(rNormal.temErros, false);
  const rEstrito = analisar(semPragma, { estrito: true });
  assert.equal(rEstrito.temErros, true);
  assert.ok(rEstrito.diagnosticos.some((d) => /VERSAO_LINGUAGEM/.test(d.mensagem)));
});

// ------------------------------------------------------------------
// símbolos
// ------------------------------------------------------------------

test('language-service — símbolos extraídos cobrem todas as categorias', () => {
  const r = analisar(PROGRAMA_BASE);
  const cats = new Set(r.simbolos.map((s) => s.categoria));
  for (const esperada of ['programa', 'estrutura', 'campo', 'enumeracao', 'membro-enum', 'regra', 'operacao', 'parametro', 'variavel'] as const) {
    assert.ok(cats.has(esperada), `categoria ausente: ${esperada} — obtidas: ${[...cats].join(', ')}`);
  }
  const item = r.simbolos.find((s) => s.nome === 'Item');
  assert.ok(item && item.categoria === 'estrutura');
  assert.equal(item?.detalhe, 'LAYOUT_COLUNAR');
  const campoQ = r.simbolos.find((s) => s.nome === 'q' && s.categoria === 'campo');
  assert.ok(campoQ);
  assert.equal(campoQ?.container, 'Item');
  const membro = r.simbolos.find((s) => s.nome === 'CANCELADO');
  assert.equal(membro?.categoria, 'membro-enum');
  assert.equal(membro?.container, 'Status');
  const param = r.simbolos.find((s) => s.nome === 'itens');
  assert.equal(param?.categoria, 'parametro');
  const variavel = r.simbolos.find((s) => s.nome === 'x' && s.categoria === 'variavel');
  assert.equal(variavel?.detalhe, 'DECIMAL(10,2)');
  const iteracao = r.simbolos.find((s) => s.nome === 'item' && s.categoria === 'variavel');
  assert.ok(iteracao);
});

test('language-service — símbolos possuem linha e coluna válidas', () => {
  const r = analisar(PROGRAMA_BASE);
  for (const s of r.simbolos) {
    assert.ok(s.linha >= 1, `linha inválida para ${s.nome}`);
    assert.ok(s.coluna >= 1, `coluna inválida para ${s.nome}`);
  }
});

// ------------------------------------------------------------------
// helpers de posição
// ------------------------------------------------------------------

test('language-service — posicaoParaOffset / offsetParaPosicao são inversos', () => {
  const fonte = 'a\nbc\ndef';
  // offsets: 0:a 1:\n 2:b 3:c 4:\n 5:d 6:e 7:f
  assert.equal(posicaoParaOffset(fonte, 1, 1), 0);
  assert.equal(posicaoParaOffset(fonte, 1, 2), 1);
  assert.equal(posicaoParaOffset(fonte, 2, 1), 2);
  assert.equal(posicaoParaOffset(fonte, 2, 2), 3);
  assert.equal(posicaoParaOffset(fonte, 3, 1), 5);
  assert.deepEqual(offsetParaPosicao(fonte, 0), { linha: 1, coluna: 1 });
  assert.deepEqual(offsetParaPosicao(fonte, 2), { linha: 2, coluna: 1 });
  assert.deepEqual(offsetParaPosicao(fonte, 5), { linha: 3, coluna: 1 });
});

test('language-service — tokenNoCursor localiza identificador', () => {
  const fonte = 'PROGRAMA Demo\nFIM_PROGRAMA';
  const r = analisar(fonte);
  const tokProg = tokenNoCursor(r.tokens!, 1, 1); // "PROGRAMA" inicia em 1:1
  assert.ok(tokProg);
  assert.equal(tokProg?.value, 'PROGRAMA');
  const tokDemo = tokenNoCursor(r.tokens!, 1, 10); // "Demo" inicia em 1:10
  assert.ok(tokDemo);
  assert.equal(tokDemo?.value, 'Demo');
});

// ------------------------------------------------------------------
// hover
// ------------------------------------------------------------------

test('language-service — hover em ESTRUTURA mostra campos e layout', () => {
  const r = analisar(PROGRAMA_BASE);
  const linhaEstrutura = PROGRAMA_BASE.split('\n').findIndex((l) => l.includes('ESTRUTURA Item')) + 1;
  const coluna = PROGRAMA_BASE.split('\n')[linhaEstrutura - 1].indexOf('Item') + 1;
  const h = obterHover(PROGRAMA_BASE, linhaEstrutura, coluna);
  assert.ok(h);
  assert.match(h!.conteudo, /ESTRUTURA/);
  assert.match(h!.conteudo, /LAYOUT_COLUNAR/);
  assert.match(h!.conteudo, /NATURAL32/);
});

test('language-service — hover em campo qualificado item.q', () => {
  const linha = PROGRAMA_BASE.split('\n').findIndex((l) => l.includes('item.q')) + 1;
  const conteudoLinha = PROGRAMA_BASE.split('\n')[linha - 1];
  const colQ = conteudoLinha.indexOf('item.q') + 'item.'.length + 1;
  const h = obterHover(PROGRAMA_BASE, linha, colQ);
  assert.ok(h, `hover ausente em linha ${linha} col ${colQ}`);
  assert.match(h!.conteudo, /campo/);
  assert.match(h!.conteudo, /NATURAL32/);
});

test('language-service — hover em variável mostra tipo', () => {
  const linha = PROGRAMA_BASE.split('\n').findIndex((l) => l.includes('VARIAVEL x :')) + 1;
  const col = PROGRAMA_BASE.split('\n')[linha - 1].indexOf('x :') + 1;
  const h = obterHover(PROGRAMA_BASE, linha, col);
  assert.ok(h);
  assert.match(h!.conteudo, /variavel/);
  assert.match(h!.conteudo, /DECIMAL/);
});

test('language-service — hover em literal e tipo primitivo', () => {
  const hNum = obterHover(PROGRAMA_BASE, PROGRAMA_BASE.split('\n').findIndex((l) => l.includes('5.00')) + 1, PROGRAMA_BASE.split('\n').find((l) => l.includes('5.00'))!.indexOf('5.00') + 1);
  assert.ok(hNum);
  assert.match(hNum!.conteudo, /literal decimal/);
  const hTipo = obterHover(PROGRAMA_BASE, PROGRAMA_BASE.split('\n').findIndex((l) => l.includes('NATURAL32')) + 1, PROGRAMA_BASE.split('\n').find((l) => l.includes('NATURAL32'))!.indexOf('NATURAL32') + 1);
  assert.ok(hTipo);
  assert.match(hTipo!.conteudo, /tipo/);
});

test('language-service — hover em ENUMERACAO e membro-enum', () => {
  const linhaEnum = PROGRAMA_BASE.split('\n').findIndex((l) => l.includes('ENUMERACAO Status')) + 1;
  const colEnum = PROGRAMA_BASE.split('\n')[linhaEnum - 1].indexOf('Status') + 1;
  const hEnum = obterHover(PROGRAMA_BASE, linhaEnum, colEnum);
  assert.ok(hEnum);
  assert.match(hEnum!.conteudo, /ENUMERACAO/);
  const linhaMembro = PROGRAMA_BASE.split('\n').findIndex((l) => l.trim() === 'CANCELADO') + 1;
  const hMembro = obterHover(PROGRAMA_BASE, linhaMembro, 3);
  assert.ok(hMembro);
  assert.match(hMembro!.conteudo, /membro/);
});

test('language-service — hover retorna undefined fora de símbolo / whitespace', () => {
  assert.equal(obterHover(PROGRAMA_BASE, 1, 1), undefined); // V de VERSAO_LINGUAGEM? Na verdade é palavra reservada, não símbolo — espera undefined
  // espaço em branco
  assert.equal(obterHover(PROGRAMA_BASE, 1, 20), undefined);
  // linha inexistente
  assert.equal(obterHover(PROGRAMA_BASE, 999, 1), undefined);
});

test('language-service — hover em programa mostra versão', () => {
  const h = obterHover(PROGRAMA_BASE, 2, 10); // PROGRAMA Demo — coluna em Demo
  assert.ok(h);
  assert.match(h!.conteudo, /PROGRAMA/);
  assert.match(h!.conteudo, /2\.2/);
});

test('language-service — fonte com erro ainda permite hover parcial em prefixo válido', () => {
  const fonte = 'VERSAO_LINGUAGEM "2.2"\nPROGRAMA X\nESTRUTURA A\n  x : DESCONHECIDO\nFIM_ESTRUTURA\nFIM_PROGRAMA';
  // hover na estrutura declarada deve funcionar mesmo com diagnóstico pendente
  const h = obterHover(fonte, 3, 12);
  // pode estar ausente se AST não foi produzida? No caso de erro semântico, AST existe.
  // Então hover em A deve existir.
  assert.ok(h);
  assert.match(h!.conteudo, /ESTRUTURA/);
});
