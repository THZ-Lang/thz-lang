import { DecimalFixo } from '../src/runtime.js';
import { medir } from './helpers.js';

export function benchDecimal(): void {
  const a = DecimalFixo.deTexto('150.5000', 4);
  const b = DecimalFixo.deTexto('18.00', 2);
  const sum = medir('DecimalFixo.somar', () => { a.somar(b); }, 100_000);
  const mul = medir('DecimalFixo.multiplicar', () => { a.multiplicar(b); }, 100_000);
  const div = medir('DecimalFixo.dividir', () => { a.dividir(b); }, 50_000);
  // baselines com Number (IEEE 754) — apenas referência, não exata
  const an = 150.5; const bn = 18.0;
  const sumN = medir('Number +', () => { const _ = an + bn; void _; }, 500_000);
  const mulN = medir('Number *', () => { const _ = an * bn; void _; }, 500_000);

  console.log('— Decimal (BigInt escalado, bancário half-even) —');
  console.log(`  somar:       ${sum.mediaMs.toFixed(4)} ms/op  (${(sum.opsSec).toFixed(0)} ops/s)`);
  console.log(`  multiplicar: ${mul.mediaMs.toFixed(4)} ms/op  (${(mul.opsSec).toFixed(0)} ops/s)`);
  console.log(`  dividir:     ${div.mediaMs.toFixed(4)} ms/op  (${(div.opsSec).toFixed(0)} ops/s)`);
  console.log('— Number (IEEE 754, não exato) — referência —');
  console.log(`  + : ${sumN.mediaMs.toFixed(4)} ms/op  (${(sumN.opsSec).toFixed(0)} ops/s)`);
  console.log(`  * : ${mulN.mediaMs.toFixed(4)} ms/op  (${(mulN.opsSec).toFixed(0)} ops/s)`);
  console.log('  Nota: DecimalFixo garante ISO/IEC 10967; Number é apenas baseline de velocidade.');
}
