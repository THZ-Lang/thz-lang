import { ArenaMemoria } from '../src/runtime.js';
import { medir } from './helpers.js';

export function benchFatia(): void {
  const N = 10_000;
  // SoA = Structure of Arrays: colunas contíguas
  const qSoA = new Array(N).fill(10);
  const vSoA = new Array(N).fill(150.5);
  // AoS = Array of Structures: objetos esparsos
  const aos = Array.from({ length: N }, () => ({ q: 10, v: 150.5 }));

  const soaScan = medir('SoA scan (q*v)', () => {
    let acc = 0;
    for (let i = 0; i < N; i++) acc += qSoA[i] * vSoA[i];
    void acc;
  }, 5_000);

  const aosScan = medir('AoS scan (obj.q*obj.v)', () => {
    let acc = 0;
    for (const o of aos) acc += o.q * o.v;
    void acc;
  }, 5_000);

  const arena = new ArenaMemoria(1);
  const arenaBench = medir('Arena alocar(64) + liberar', () => {
    arena.alocar(64);
    arena.liberarTudo();
  }, 100_000);

  console.log('— Fatias / Layout —');
  console.log(`  SoA scan: ${soaScan.mediaMs.toFixed(4)} ms/op  (${soaScan.opsSec.toFixed(0)} ops/s)  N=${N}`);
  console.log(`  AoS scan: ${aosScan.mediaMs.toFixed(4)} ms/op  (${aosScan.opsSec.toFixed(0)} ops/s)`);
  console.log(`  Arena:   ${arenaBench.mediaMs.toFixed(5)} ms/op  (${arenaBench.opsSec.toFixed(0)} ops/s) — O(1) descarte`);
  console.log('  Esperado: SoA ~1.3–2× mais rápido (localidade) e vetorizável; Arena O(1) vs GC.');
}
