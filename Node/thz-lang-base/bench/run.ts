#!/usr/bin/env tsx
import { benchDecimal } from './decimal.bench.js';
import { benchFatia } from './fatia.bench.js';
import { benchSimd } from './simd.bench.js';

console.log('='.repeat(70));
console.log('  THZ-LANG — Benchmarks Comparativos (G6)');
console.log('  Decimal (BigInt) • Fatia SoA/AoS/Arena • SIMD VETORIZAR_PARA');
console.log('='.repeat(70));
console.log('');

const total0 = performance.now();

console.log('[1/3] Decimal');
console.log('-'.repeat(70));
benchDecimal();
console.log('');

console.log('[2/3] Fatia / Arena');
console.log('-'.repeat(70));
benchFatia();
console.log('');

console.log('[3/3] SIMD — VETORIZAR_PARA');
console.log('-'.repeat(70));
benchSimd();
console.log('');

const total1 = performance.now();
console.log('='.repeat(70));
console.log(`Concluído em ${(total1 - total0).toFixed(0)} ms — resultados são relativos ao host (Node ${process.version}, ${process.platform} ${process.arch})`);
console.log('='.repeat(70));
