import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  DecimalFixo,
  Monetario,
  ModoArredondamento,
  ErroDecimal,
  ErroMonetario
} from '../src/runtime.js';

/* ---------------- Escalas paramétricas (P,S) ---------------- */

test('soma alinha escalas distintas ao resultado na maior escala', () => {
  const r = DecimalFixo.deTexto('1.5', 1).somar(DecimalFixo.deTexto('2.25', 2));
  assert.equal(r.escala, 2);
  assert.equal(r.formatar(), '3.75');
});

test('subtração com sinais opostos preserva escala comum', () => {
  const r = DecimalFixo.deTexto('0.10', 2).subtrair(DecimalFixo.deTexto('1.0001', 4));
  assert.equal(r.formatar(), '-0.9001');
});

test('multiplicação é exata e reescalada uma única vez (bancário)', () => {
  // 1505.0000 * 0.1800 = 270.9 exato
  const r = DecimalFixo.deTexto('1505.0000', 4).multiplicar(DecimalFixo.deTexto('0.1800', 4));
  assert.equal(r.escala, 4);
  assert.equal(r.formatar(), '270.9000');
});

/* ---------------- Arredondamento bancário (half-even) ---------------- */

test('empate half-even arredonda ao par vizinho', () => {
  const base = DecimalFixo.deTexto('1.25', 2); // produto exato 1.25 → escala 1
  const paraPar = base.paraEscala(1, ModoArredondamento.BANCARIO);
  assert.equal(paraPar.formatar(), '1.2'); // 2 é par

  const impares = DecimalFixo.deTexto('1.35', 2).paraEscala(1, ModoArredondamento.BANCARIO);
  assert.equal(impares.formatar(), '1.4'); // arredonda p/ 4 (par)
});

test('meia-cima resolve empates sempre para cima', () => {
  assert.equal(DecimalFixo.deTexto('1.25', 2).paraEscala(1, ModoArredondamento.MEIA_CIMA).formatar(), '1.3');
});

test('truncar descarta dígitos excedentes em direção ao zero', () => {
  assert.equal(DecimalFixo.deTexto('-1.39', 2).paraEscala(1, ModoArredondamento.TRUNCAR).formatar(), '-1.3');
  assert.equal(DecimalFixo.deTexto('1.39', 2).paraEscala(1, ModoArredondamento.TRUNCAR).formatar(), '1.3');
});

/* ---------------- Divisão ---------------- */

test('divisão periódica usa dígitos de guarda e arredonda uma vez', () => {
  const umTerco = DecimalFixo.deInteiro(1n).dividir(DecimalFixo.deInteiro(3n));
  assert.equal(umTerco.formatar(), '0.3333');
  const doisTercos = DecimalFixo.deInteiro(2n).dividir(DecimalFixo.deInteiro(3n));
  assert.equal(doisTercos.formatar(), '0.6667'); // resto acima da metade → sobe
});

test('divisão por zero é erro explícito', () => {
  assert.throws(() => DecimalFixo.deInteiro(1n).dividir(DecimalFixo.deInteiro(0n)), ErroDecimal);
});

test('divisão de negativos sinaliza corretamente na escala comum', () => {
  const r = DecimalFixo.deTexto('-7.0', 1).dividir(DecimalFixo.deTexto('2.0', 1));
  assert.equal(r.escala, 1);
  assert.equal(r.formatar(), '-3.5');
});

/* ---------------- Comparação e literais ---------------- */

test('comparação normaliza escalas antes de comparar escalados', () => {
  assert.ok(DecimalFixo.deTexto('1.5', 1).comparar(DecimalFixo.deTexto('1.5000', 4)) === 0);
  assert.ok(DecimalFixo.deTexto('2.0', 1).comparar(DecimalFixo.deTexto('1.9999', 4)) > 0);
});

test('literal com mais casas que a escala declarada é rejeitado', () => {
  assert.throws(() => DecimalFixo.deTexto('18.0000', 2), /mais casas decimais/);
});

test('negativos entre -1 e 0 preservam o sinal', () => {
  assert.equal(DecimalFixo.deTexto('-0.5', 2).formatar(), '-0.50');
  assert.equal(DecimalFixo.deTexto('-0.0050', 4).abs().formatar(), '0.0050');
});

/* ---------------- Monetário ISO 4217 ---------------- */

test('monetário soma apenas moedas idênticas', () => {
  const brl1 = Monetario.deTexto('100.50', 'BRL');
  const brl2 = Monetario.deTexto('29.50', 'BRL');
  assert.equal(brl1.somar(brl2).formatar(), '130.00 BRL');
});

test('mistura de moedas é erro explícito citando os códigos', () => {
  const brl = Monetario.deTexto('100.00', 'BRL');
  const usd = Monetario.deTexto('20.00', 'USD');
  assert.throws(() => brl.somar(usd), (e: unknown) => {
    assert.ok(e instanceof ErroMonetario);
    assert.match(e.message, /somar BRL com USD/);
    return true;
  });
});

test('JPY tem zero casas decimais (ISO 4217 exponent 0)', () => {
  const iene = Monetario.deTexto('1234567', 'JPY');
  assert.equal(iene.quantia.escala, 0);
  assert.equal(iene.formatar(), '1234567 JPY');
  assert.throws(() => Monetario.deTexto('12.50', 'JPY'), ErroDecimal);
});

test('multiplicação monetária por fator decimal mantém a moeda', () => {
  const total = Monetario.deTexto('250.00', 'BRL').multiplicar(DecimalFixo.deTexto('0.18', 2));
  assert.equal(total.formatar(), '45.00 BRL');
});

test('código de moeda inválido é rejeitado', () => {
  assert.throws(() => Monetario.deTexto('1.00', 'XXX'), ErroMonetario);
});
