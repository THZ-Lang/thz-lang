/* Bench helpers — G6 */
export function medir(nome: string, fn: () => void, iter = 50_000): { mediaMs: number; opsSec: number } {
  // warmup
  for (let i = 0; i < Math.min(1_000, iter / 10); i++) fn();
  const t0 = performance.now();
  for (let i = 0; i < iter; i++) fn();
  const t1 = performance.now();
  const total = t1 - t0;
  const mediaMs = total / iter;
  const opsSec = 1_000 / mediaMs;
  return { mediaMs, opsSec };
}

export function formatar(n: number): string {
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(2) + 'M';
  if (n >= 1_000) return (n / 1_000).toFixed(1) + 'k';
  return n.toFixed(0);
}
