import * as readline from 'readline';
import { ThzLexer } from './lexer.js';
import { ThzParser } from './parser.js';
import { AnalisadorSemantico } from './analisador.js';
import { InterpretadorThz } from './interpretador.js';
import { formatarErroComCaret } from './errors.js';

/* ============================================================
 * THZ REPL v2.2 — sessão persistente com reconstrução do programa
 *
 * Digite comandos THZ (VARIAVEL, EXIBA, SE..., ESTRUTURA, ENUMERACAO).
 * Termine o bloco com uma linha vazia para avaliar.
 *
 *   .ajuda   mostra os comandos
 *   .limpar  reinicia a sessão
 *   .codigo  exibe o programa-fonte acumulado
 *   .sair    encerra
 * ============================================================ */

interface EstadoSessao {
  declaracoesTopo: string[];
  corpo: string[];
}

const CABECALHO_LINHAS = 3; // pragma + PROGRAMA + (vazio)

function montarPrograma(sessao: EstadoSessao): { fonte: string; linhaInicioCorpo: number } {
  const partes: string[] = ['VERSAO_LINGUAGEM "2.2"', 'PROGRAMA SESSAO'];
  for (const decl of sessao.declaracoesTopo) {
    partes.push(decl);
  }
  partes.push('REGRA_NEGOCIO Sessao', 'OPERACAO Principal() : DECIMAL(38, 10)', 'INICIO');
  const linhaInicioCorpo = partes.length + 1; // 1-based: próxima linha após INICIO
  for (const cmd of sessao.corpo) {
    partes.push(cmd);
  }
  partes.push('FIM', 'FIM_REGRA_NEGOCIO', 'FIM_PROGRAMA');
  return { fonte: partes.join('\n'), linhaInicioCorpo };
}

/** Traduz posição do fonte gerado para o trecho digitado pelo usuário. */
function mapearParaChunk(
  linhaGerada: number,
  colunaGerada: number,
  linhaInicioCorpo: number,
  indicePrimeiraLinhaChunk: number,
  chunk: string
): { linha: number; coluna: number } | undefined {
  const linhasCorpoAntes = linhaGerada - linhaInicioCorpo;
  const linhasChunk = chunk.split(/\r?\n/).length;
  if (linhasCorpoAntes < 0 || linhasCorpoAntes >= linhasChunk) return undefined;
  return { linha: linhasCorpoAntes + 1, coluna: colunaGerada };
}

export async function executarRepl(): Promise<void> {
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    prompt: 'thz> '
  });

  const sessao: EstadoSessao = { declaracoesTopo: [], corpo: [] };
  let buffer: string[] = [];
  let encerrar = false;

  console.log('THZ-LANG REPL v2.2 — digite ".ajuda" para os comandos, ".sair" para encerrar.');
  rl.prompt();

  const avaliarChunk = (chunk: string): void => {
    const indicePrimeiraLinhaChunk = sessao.corpo.length;

    // Declarações de topo viram parte do programa acumulado.
    const inicioTrim = chunk.trimStart();
    if (/^(ESTRUTURA|ENUMERACAO)\b/.test(inicioTrim)) {
      sessao.declaracoesTopo.push(chunk);
      console.log('[SESSAO] Declaração registrada.');
      return;
    }

    sessao.corpo.push(chunk);

    const { fonte, linhaInicioCorpo } = montarPrograma(sessao);
    try {
      const tokens = new ThzLexer(fonte).tokenize();
      const ast = new ThzParser(tokens).parse();

      const erros = new AnalisadorSemantico(ast).analisar({});
      if (erros.length > 0) {
        for (const erro of erros) {
          const posicao = mapearParaChunk(erro.linha, erro.coluna, linhaInicioCorpo, indicePrimeiraLinhaChunk, chunk)
            ?? { linha: erro.linha, coluna: erro.coluna };
          console.error(formatarErroComCaret(chunk, { linha: posicao.linha, coluna: posicao.coluna, mensagem: erro.mensagem }));
        }
        sessao.corpo.pop(); // não polui a sessão com código reprovado
        return;
      }

      const linhas: string[] = [];
      const interpretador = new InterpretadorThz(ast, { saida: (l) => linhas.push(l) });
      interpretador.executarOperacao('Principal', {});
      for (const linha of linhas) console.log(linha);
    } catch (err) {
      const mensagem = (err as Error).message;
      const casamento = /\[Linha (\d+):(\d+)\]/.exec(mensagem);
      if (casamento) {
        const posicao = mapearParaChunk(Number(casamento[1]), Number(casamento[2]), linhaInicioCorpo, indicePrimeiraLinhaChunk, chunk);
        if (posicao) {
          console.error(formatarErroComCaret(chunk, { linha: posicao.linha, coluna: posicao.coluna, mensagem }));
        } else {
          console.error(mensagem);
        }
      } else {
        console.error(mensagem);
      }
      sessao.corpo.pop();
    }
  };

  const processarLinha = (entrada: string): void => {
    const linha = entrada.trim();

    if (buffer.length === 0 && linha.startsWith('.')) {
      switch (linha) {
        case '.sair':
        case '.quit':
          encerrar = true;
          rl.close();
          return;
        case '.limpar':
          sessao.declaracoesTopo.length = 0;
          sessao.corpo.length = 0;
          console.log('[SESSAO] Sessão reiniciada.');
          return;
        case '.codigo':
          console.log(montarPrograma(sessao).fonte || '(vazio)');
          return;
        case '.ajuda':
          console.log([
            'Bloco de comandos + <enter> em linha vazia avalia o bloco.',
            '.ajuda  esta ajuda',
            '.limpar reinicia a sessão',
            '.codigo exibe o programa acumulado',
            '.sair   encerra o REPL'
          ].join('\n'));
          return;
        default:
          console.log("[REPL] Comando desconhecido: '" + linha + "'. Use .ajuda.");
          return;
      }
    }

    if (linha === '') {
      if (buffer.length > 0) {
        avaliarChunk(buffer.join('\n'));
        buffer = [];
      }
      return;
    }

    buffer.push(entrada);
  };

  rl.on('line', (entrada) => {
    processarLinha(entrada);
    if (!encerrar) rl.prompt();
  });

  rl.on('close', () => {
    console.log('\n[SESSAO] Encerrada. Arena liberada.');
  });
}
