/* ============================================================
 * THZ-LANG — Diagnóstico de erros com trecho de fonte e caret
 *
 * Norma do repositório: todo erro reporta [Erro ...][Linha L:C].
 * Esta camada acrescenta o contexto visual:
 *
 *   [Erro Semântico][Linha 12:31] Atribuição incompatível ...
 *        12 |     x <- "texto"
 *           |                  ^
 * ============================================================ */

export interface DiagnosticoEntrada {
  linha: number;
  coluna: number;
  mensagem: string;
}

const LARGURA_NUMERO = 5;

function numeroLinhaFormatado(n: number): string {
  return String(n).padStart(LARGURA_NUMERO, ' ');
}

/**
 * Formata um erro com a linha do fonte e um caret ^ na coluna exata.
 * Colunas são 1-based (padrão do lexer); o caret aponta para o caractere.
 */
export function formatarErroComCaret(fonte: string, entrada: DiagnosticoEntrada): string {
  const linhas = fonte.split(/\r?\n/);
  const indice = Math.min(Math.max(entrada.linha - 1, 0), Math.max(linhas.length - 1, 0));
  const conteudo = linhas[indice] ?? '';
  const coluna = Math.max(1, entrada.coluna);

  const cabecalho = '[Erro][Linha ' + entrada.linha + ':' + coluna + '] ' + entrada.mensagem;
  const prefixo = '       | ';
  const linhaFonte = prefixo.replace('|', '') + numeroLinhaFormatado(entrada.linha) + ' | ' + conteudo;
  const caret = '         ' + ' '.repeat(LARGURA_NUMERO) + ' | ' + ' '.repeat(coluna - 1) + '^';

  return cabecalho + '\n' + linhaFonte + '\n' + caret;
}

/** Formata uma lista de erros semânticos com contexto visual. */
export function formatarDiagnosticos(fonte: string, diagnosticos: DiagnosticoEntrada[], rotulo: string = 'Semântico'): string[] {
  return diagnosticos.map((d) => {
    const bloco = formatarErroComCaret(fonte, d);
    if (!rotulo) return bloco;
    return bloco.replace(/^\[Erro\]/, '[Erro ' + rotulo + ']');
  });
}
