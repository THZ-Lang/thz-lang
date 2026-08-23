import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'fs';
import path from 'path';
import { ThzLexer } from '../src/lexer.js';
import { ThzParser } from '../src/parser.js';

const DIRETORIO_SNAPSHOTS = path.join(process.cwd(), 'test', '__snapshots__');

/** Serialização canônica da AST (bigint → texto) para comparação determinística. */
function astJson(fonte: string): string {
  const tokens = new ThzLexer(fonte).tokenize();
  const ast = new ThzParser(tokens).parse();
  return JSON.stringify(ast, (_, v) => (typeof v === 'bigint' ? v.toString() : v), 2);
}

/**
 * Golden tests (Tier 1 aprovado): qualquer mudança não intencional na forma
 * da AST do exemplo canônico quebra a build imediatamente.
 */
for (const exemplo of ['faturamento.thz', 'pedidos.thz']) {
  test('Golden AST — ' + exemplo + ' permanece estável', () => {
    const fonte = fs.readFileSync(path.join(process.cwd(), 'exemplos', exemplo), 'utf8');
    const atual = astJson(fonte);

    const arquivoSnapshot = path.join(DIRETORIO_SNAPSHOTS, exemplo.replace(/\.thz$/, '') + '.ast.json');
    if (!fs.existsSync(arquivoSnapshot)) {
      fs.mkdirSync(DIRETORIO_SNAPSHOTS, { recursive: true });
      fs.writeFileSync(arquivoSnapshot, atual, 'utf8');
      console.log('[GOLDEN] Snapshot criado: ' + arquivoSnapshot);
      return;
    }

    const esperado = fs.readFileSync(arquivoSnapshot, 'utf8');
    assert.equal(atual, esperado, 'AST divergiu do golden snapshot. Se a mudança for intencional, regenere o snapshot em ' + arquivoSnapshot);
  });
}
