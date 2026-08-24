import fs from 'fs';
import path from 'path';
import { ThzLexer } from './lexer.js';
import { ThzParser } from './parser.js';
import { ThzDocGen } from './docgen.js';
import { AnalisadorSemantico } from './analisador.js';
import { formatarDiagnosticos } from './errors.js';
import { auditar, gerarMarkdownGovernanca } from './governanca.js';
import { baixarParaIr, serializarIr, emitirLlvm } from './ir.js';
import { formatar } from './fmt.js';
import { ProgramaAST, EstruturaAST, OperacaoAST, ProcedimentoAST, ComandoAST } from './types.js';
import { ArenaMemoria, DecimalFixo } from './runtime.js';
import {
  InterpretadorThz,
  ValorThz,
  valorThzDe,
  INTEIRO
} from './interpretador.js';

const comando = process.argv[2] || 'run';
const argumentos = process.argv.slice(3);
const estrito = argumentos.includes('--estrito');
function resolverArquivo(args: string[]): string {
  const flagsComValor = new Set(['--saida', '--principal', '--arg']);
  for (let i = 0; i < args.length; i++) {
    const a = args[i];
    if (flagsComValor.has(a)) { i++; continue; }
    if (a.startsWith('--arg=')) continue;
    if (a.startsWith('-')) continue;
    return a;
  }
  return 'exemplos/faturamento.thz';
}
const arquivo = resolverArquivo(argumentos);

if (comando === 'repl') {
  import('./repl.js')
    .then((m) => m.executarRepl())
    .catch((err) => {
      console.error('[REPL] Falha: ' + (err as Error).message);
      process.exit(1);
    });
} else {
  executarFluxoDeArquivo();
}

function executarFluxoDeArquivo(): void {
  if (!fs.existsSync(arquivo)) {
    console.error('[ERRO] Arquivo não encontrado: ' + arquivo);
    process.exit(1);
  }
  const codigoFonte = fs.readFileSync(arquivo, 'utf8');

/** Lote posicional de demonstração (determinístico), mapeado ao schema da estrutura.
 *  Valores decimais respeitam a escala declarada de cada campo. */
const LOTE_DEMONSTRACAO: unknown[][] = [
  ['a1b2c3d4-0000-0000-0000-000000000001', 'PROD-SKU-901', 10, '150.5000', '18.00', '0'],
  ['a1b2c3d4-0000-0000-0000-000000000002', 'PROD-SKU-902', 5, '320.0000', '12.00', '0']
];

function estruturaPorNome(ast: ProgramaAST, nome: string): EstruturaAST | undefined {
  return ast.estruturas.find((e) => e.nome === nome);
}

function registroDe(estrutura: EstruturaAST, valores: unknown[], validarInvariantes?: (v: ValorThz) => void): ValorThz {
  const campos = new Map<string, ValorThz>();
  estrutura.campos.forEach((campo, indice) => {
    const bruto = valores[indice];
    if (bruto !== undefined) {
      campos.set(campo.nome, valorThzDe(campo.tipo, bruto));
      return;
    }
    if (campo.tipo.startsWith('NATURAL') || campo.tipo.startsWith('INTEIRO')) {
      campos.set(campo.nome, INTEIRO(0n));
    } else if (campo.tipo.startsWith('DECIMAL') || campo.tipo.startsWith('MONETARIO')) {
      campos.set(campo.nome, valorThzDe(campo.tipo, '0'));
    } else {
      campos.set(campo.nome, valorThzDe(campo.tipo, ''));
    }
  });
  const registro: ValorThz = { classe: 'REGISTRO', nomeEstrutura: estrutura.nome, campos };
  if (validarInvariantes) validarInvariantes(registro);
  return registro;
}

function construirArgumentos(
  operacao: OperacaoAST,
  ast: ProgramaAST,
  validarInvariantes?: (v: ValorThz) => void
): Record<string, ValorThz> {
  const argumentos: Record<string, ValorThz> = {};
  for (const parametro of operacao.parametros) {
    const casamentoFatia = /^FATIA\[(\w+)\]$/.exec(parametro.tipo);
    if (casamentoFatia) {
      const estrutura = estruturaPorNome(ast, casamentoFatia[1]);
      if (!estrutura) {
        throw new Error("[Erro de Execução] Estrutura '" + casamentoFatia[1] + "' referenciada por '" + parametro.tipo + "' não declarada.");
      }
      argumentos[parametro.nome] = {
        classe: 'FATIA',
        tipoInterno: casamentoFatia[1],
        elementos: LOTE_DEMONSTRACAO.map((linha) => registroDe(estrutura, linha, validarInvariantes))
      };
    } else {
      argumentos[parametro.nome] = valorThzDe(parametro.tipo, 0);
    }
  }
  return argumentos;
}

function construirArgumentosProcedimento(proc: ProcedimentoAST, mapaArgs: Map<string, string>): Record<string, ValorThz> {
  const out: Record<string, ValorThz> = {};
  for (const p of proc.parametros) {
    const bruto = mapaArgs.get(p.nome);
    if (bruto === undefined) {
      // tenta padrão zero/texto vazio se não fornecido e não obrigatório? Exige fornecido
      throw new Error("[Erro de Execução] Parâmetro '" + p.nome + "' não fornecido. Use --arg " + p.nome + "=valor");
    }
    out[p.nome] = valorThzDe(p.tipo, bruto);
  }
  return out;
}

function parseArgsMapa(args: string[]): Map<string, string> {
  const mapa = new Map<string, string>();
  for (let i = 0; i < args.length; i++) {
    const a = args[i];
    if (a === '--arg' && i + 1 < args.length) {
      const par = args[i + 1]; i++;
      const eq = par.indexOf('=');
      if (eq >= 0) mapa.set(par.slice(0, eq), par.slice(eq + 1));
    } else if (a.startsWith('--arg=')) {
      const par = a.slice(6);
      const eq = par.indexOf('=');
      if (eq >= 0) mapa.set(par.slice(0, eq), par.slice(eq + 1));
    }
  }
  return mapa;
}

function criarLeitorEntrada(): () => string | null {
  let bufferRestante = '';
  let eof = false;
  // Buffer para leituras incrementais
  return (): string | null => {
    // Se já temos linha completa no buffer, retorna
    const nlIdx = bufferRestante.indexOf('\n');
    if (nlIdx >= 0) {
      const linha = bufferRestante.slice(0, nlIdx);
      bufferRestante = bufferRestante.slice(nlIdx + 1);
      return linha.replace(/\r$/, '');
    }
    if (eof) {
      if (bufferRestante.length > 0) { const r = bufferRestante; bufferRestante = ''; return r; }
      return null;
    }
    // Lê sincrono do stdin até newline ou EOF
    try {
      const chunk = Buffer.alloc(4096);
      let linhaAcumulada = bufferRestante;
      // Tenta ler até obter uma linha
      while (true) {
        let bytes: number;
        try { bytes = fs.readSync(0, chunk, 0, chunk.length, null); } catch { return null; }
        if (bytes === 0) { eof = true; bufferRestante = ''; return linhaAcumulada.length > 0 ? linhaAcumulada : null; }
        const texto = chunk.toString('utf8', 0, bytes);
        linhaAcumulada += texto;
        const idx = linhaAcumulada.indexOf('\n');
        if (idx >= 0) {
          bufferRestante = linhaAcumulada.slice(idx + 1);
          return linhaAcumulada.slice(0, idx).replace(/\r$/, '');
        }
        // continua lendo
      }
    } catch { return null; }
  };
}

try {
  // 1. Lexer → 2. Parser
  const tokens = new ThzLexer(codigoFonte).tokenize();
  const ast = new ThzParser(tokens).parse();

  if (comando === 'check') {
    const errosSemanticos = new AnalisadorSemantico(ast).analisar({ estrito });
    if (errosSemanticos.length > 0) {
      for (const bloco of formatarDiagnosticos(codigoFonte, errosSemanticos)) {
        console.error(bloco + '\n');
      }
      console.error('[THZ CHECK] ' + errosSemanticos.length + ' erro(s) semântico(s).');
      process.exit(1);
    }
    const versao = ast.versaoLinguagem ? ' (ver. linguagem declarada: ' + ast.versaoLinguagem + ')' : ' (sem pragma VERSAO_LINGUAGEM — assumindo versão corrente)';
    console.log('[THZ CHECK] Código validado com sucesso! AST íntegra para o programa: ' + ast.nome + versao + (estrito ? ' [lint estrito aprovado]' : ''));
    process.exit(0);
  }

  if (comando === 'ast') {
    console.log(JSON.stringify(ast, (_, v) => (typeof v === 'bigint' ? v.toString() : v), 2));
    process.exit(0);
  }

  if (comando === 'doc') {
    const docMd = ThzDocGen.gerarMarkdown(ast);
    const saidaDoc = path.join('docs', ast.nome + '_arquitetura.md');
    fs.writeFileSync(saidaDoc, docMd, 'utf8');
    console.log('[THZ DOC] Documentação gerada em: ' + saidaDoc);
    process.exit(0);
  }

  if (comando === 'audit') {
    const formatoJson = argumentos.includes('--json');
    const idxSaida = argumentos.indexOf('--saida');
    const arquivoSaida = idxSaida >= 0 ? argumentos[idxSaida + 1] : undefined;
    const auditoria = auditar(ast, { estrito });
    const conteudo = formatoJson ? JSON.stringify(auditoria, null, 2) : gerarMarkdownGovernanca(auditoria);
    if (arquivoSaida) {
      const alvo = arquivoSaida.endsWith('.json') || arquivoSaida.endsWith('.md') ? arquivoSaida : path.join(arquivoSaida, ast.nome + '_governanca.md');
      fs.mkdirSync(path.dirname(alvo), { recursive: true });
      fs.writeFileSync(alvo, conteudo, 'utf8');
      console.log('[THZ AUDIT] Relatório gravado em: ' + alvo);
    } else {
      console.log(conteudo);
    }
    if (!auditoria.aprovada) {
      console.error('[THZ AUDIT] ' + auditoria.pendencias.length + ' pendência(s).' + (estrito ? ' (reprovado no estrito)' : ' (aprovada com alertas)'));
      process.exit(estrito ? 1 : 0);
    }
    console.log('[THZ AUDIT] Governança aprovada.');
    process.exit(0);
  }

  if (comando === 'ir') {
    const comLlvm = argumentos.includes('--llvm');
    const idxSaida = argumentos.indexOf('--saida');
    const arquivoSaida = idxSaida >= 0 ? argumentos[idxSaida + 1] : undefined;
    const ir = baixarParaIr(ast);
    const erros = new AnalisadorSemantico(ast).analisar({ estrito: false });
    if (erros.length > 0) {
      console.error('[THZ IR] AST possui ' + erros.length + ' erro(s) semântico(s) — IR gerado com diagnósticos SIMD preservados.');
    }
    const conteudo = comLlvm ? emitirLlvm(ir) : serializarIr(ir);
    if (arquivoSaida) {
      const ext = comLlvm ? '.ll' : '.thz-ir.json';
      const alvo = arquivoSaida.includes('.') ? arquivoSaida : path.join(arquivoSaida, ast.nome + ext);
      fs.mkdirSync(path.dirname(alvo), { recursive: true });
      fs.writeFileSync(alvo, conteudo, 'utf8');
      console.log('[THZ IR] ' + (comLlvm ? 'LLVM' : 'IR') + ' gravado em: ' + alvo + (ir.diagnosticosSimd.some((d) => !d.verificado) ? ' (com diagnósticos SIMD)' : ''));
    } else {
      console.log(conteudo);
      if (ir.diagnosticosSimd.length > 0) {
        console.error('\n[THZ IR — SIMD]');
        for (const d of ir.diagnosticosSimd) {
          const tag = d.verificado ? '✔' : '✘';
          console.error(` ${tag} ${d.funcao} :: ${d.variavel} EM ${d.fonte} [${d.loc.linha}:${d.loc.coluna}]${d.diagnosticos.length ? ' — ' + d.diagnosticos.join('; ') : ''}`);
        }
      }
    }
    process.exit(0);
  }

  if (comando === 'fmt') {
    const check = argumentos.includes('--check');
    const escrever = argumentos.includes('--escrever') || argumentos.includes('-w');
    const idxSaida = argumentos.indexOf('--saida');
    const arquivoSaida = idxSaida >= 0 ? argumentos[idxSaida + 1] : undefined;
    const formatado = formatar(ast);
    const original = codigoFonte;
    if (check) {
      if (original !== formatado) {
        console.error('[THZ FMT] Arquivo não está formatado. Use `thz fmt --escrever` para corrigir.');
        // diff curto: primeira linha divergente
        const a = original.split('\n');
        const b = formatado.split('\n');
        for (let i = 0; i < Math.max(a.length, b.length); i++) {
          if (a[i] !== b[i]) {
            console.error(`  Linha ${i + 1} esperada: ${JSON.stringify(b[i] ?? '')}`);
            console.error(`  Linha ${i + 1} obtida:   ${JSON.stringify(a[i] ?? '')}`);
            break;
          }
        }
        process.exit(1);
      }
      console.log('[THZ FMT] OK — arquivo já está canônico.');
      process.exit(0);
    }
    if (arquivoSaida) {
      const alvo = arquivoSaida.includes('.thz') ? arquivoSaida : path.join(arquivoSaida, path.basename(arquivo));
      fs.mkdirSync(path.dirname(alvo), { recursive: true });
      fs.writeFileSync(alvo, formatado, 'utf8');
      console.log('[THZ FMT] Arquivo formatado gravado em: ' + alvo);
      process.exit(0);
    }
    if (escrever) {
      fs.writeFileSync(arquivo, formatado, 'utf8');
      console.log('[THZ FMT] ' + arquivo + ' formatado.');
      process.exit(0);
    }
    console.log(formatado);
    process.exit(0);
  }

  if (comando === 'run') {
    console.log('================================================================================');
    console.log('   EXECUTANDO MOTOR NATIVO THZ-LANG: ' + ast.nome);
    console.log('================================================================================\n');

    const arena = new ArenaMemoria(64);
    arena.alocar(2048);

    const dom = ast.metadados ? ast.metadados.dominio : 'N/A';
    const slo = ast.metadados ? ast.metadados.sloLatencia : 'N/A';
    const conf = ast.metadados ? ast.metadados.conformidade.join(', ') : 'N/A';

    console.log('[ARQUITETURA] Domínio: ' + dom + ' | SLO: ' + slo);
    console.log('[CONFORMIDADE] Diretrizes ativas: ' + conf + '\n');

    const idxPrincipal = argumentos.indexOf('--principal');
    const nomePrincipal = idxPrincipal >= 0 ? argumentos[idxPrincipal + 1] : undefined;
    const mapaArgs = parseArgsMapa(argumentos);
    const precisaEntrada = (() => { // detecta se algum comando é LER
      const temLer = (cmds: ComandoAST[]): boolean => cmds.some((c) => c.tipoComando === 'LER' || (c.tipoComando === 'SE' && (temLer(c.entao) || temLer(c.senao))) || (c.tipoComando === 'ENQUANTO' && temLer(c.corpo)) || (c.tipoComando === 'PARA' && temLer(c.corpo)) || (c.tipoComando === 'VETORIZAR_PARA' && temLer(c.corpo)) || (c.tipoComando === 'BLOCO_MEMORIA' && temLer(c.corpo)) );
      const todosComandos: ComandoAST[][] = [
        ...ast.regras.flatMap((r) => r.operacoes.map((o) => o.corpo)),
        ...(ast.procedimentos ?? []).map((p) => p.corpo)
      ];
      return todosComandos.some(temLer);
    })();
    const entrada = precisaEntrada ? criarLeitorEntrada() : undefined;
    const interpretador = new InterpretadorThz(ast, { entrada });

    // --principal explícito
    if (nomePrincipal) {
      const proc = (ast.procedimentos ?? []).find((p) => p.nome === nomePrincipal);
      if (proc) {
        console.log('[PROCEDIMENTO] ' + proc.nome + '()\n');
        const args = construirArgumentosProcedimento(proc, mapaArgs);
        interpretador.executarProcedimento(proc.nome, args);
        arena.liberarTudo();
        console.log('\n[RUNTIME] Arena de memória liberada em O(1). Execução finalizada sem erros.');
        process.exit(0);
      }
      // tenta operação
      const opEncontrada = interpretador.listarOperacoesExecutaveis().find((o) => o.operacao.nome === nomePrincipal);
      if (opEncontrada) {
        console.log('[REGRA] ' + opEncontrada.regra.nome + (opEncontrada.regra.identificador ? ' (' + opEncontrada.regra.identificador + ')' : '') + ' :: ' + opEncontrada.operacao.nome + '()\n');
        const args: Record<string, ValorThz> = {};
        for (const p of opEncontrada.operacao.parametros) {
          const bruto = mapaArgs.get(p.nome);
          if (bruto !== undefined) args[p.nome] = valorThzDe(p.tipo, bruto);
          else {
            const fatiaMatch = /^FATIA\[(\w+)\]$/.exec(p.tipo);
            if (fatiaMatch) {
              const est = estruturaPorNome(ast, fatiaMatch[1]);
              if (!est) throw new Error("Estrutura '" + fatiaMatch[1] + "' não declarada.");
              args[p.nome] = { classe: 'FATIA', tipoInterno: fatiaMatch[1], elementos: LOTE_DEMONSTRACAO.map((linha) => registroDe(est, linha, (v) => interpretador.validarInvariantes(v))) };
            } else args[p.nome] = valorThzDe(p.tipo, bruto ?? 0);
          }
        }
        const resultado = interpretador.executarOperacao(opEncontrada.operacao.nome, args);
        console.log('--------------------------------------------------------------');
        if (resultado && resultado.classe === 'DECIMAL') {
          console.log('[RESULTADO] Total de Tributos Retidos em Lote: R$ ' + resultado.valor.formatar());
        } else if (resultado) {
          console.log('[RESULTADO] ' + interpretador.formatar(resultado));
        }
        arena.liberarTudo();
        console.log('\n[RUNTIME] Arena de memória liberada em O(1). Execução finalizada sem erros.');
        process.exit(0);
      }
      console.error("[ERRO] Entrada '--principal " + nomePrincipal + "' não encontrada como PROCEDIMENTO nem OPERACAO.");
      process.exit(1);
    }

    // Sem --principal: tenta PROCEDIMENTO Principal() como default
    const procPrincipal = (ast.procedimentos ?? []).find((p) => p.nome === 'Principal');
    if (procPrincipal) {
      console.log('[PROCEDIMENTO] Principal()\n');
      const args = procPrincipal.parametros.length > 0 ? construirArgumentosProcedimento(procPrincipal, mapaArgs) : {};
      interpretador.executarProcedimento('Principal', args);
      arena.liberarTudo();
      console.log('\n[RUNTIME] Arena de memória liberada em O(1). Execução finalizada sem erros.');
      process.exit(0);
    }

    const executaveis = interpretador.listarOperacoesExecutaveis();

    if (executaveis.length === 0) {
      console.error('[ERRO] Nenhuma operação com corpo executável declarada. Adicione um bloco INICIO ... FIM a uma OPERACAO ou declare PROCEDIMENTO Principal.');
      process.exit(1);
    }

    const { regra, operacao } = executaveis[0];
    console.log('[REGRA] ' + regra.nome + (regra.identificador ? ' (' + regra.identificador + ')' : '') + ' :: ' + operacao.nome + '()\n');

    const resultado = interpretador.executarOperacao(operacao.nome, construirArgumentos(operacao, ast, (v) => interpretador.validarInvariantes(v)));

    console.log('--------------------------------------------------------------');
    if (resultado && resultado.classe === 'DECIMAL') {
      console.log('[RESULTADO] Total de Tributos Retidos em Lote: R$ ' + resultado.valor.formatar());
    } else if (resultado) {
      console.log('[RESULTADO] ' + interpretador.formatar(resultado));
    }

    arena.liberarTudo();
    console.log('\n[RUNTIME] Arena de memória liberada em O(1). Execução finalizada sem erros.');
  }
} catch (err) {
  const mensagem = (err as Error).message;
  const casamento = /\[Linha (\d+):(\d+)\]/.exec(mensagem);
  if (casamento) {
    const linha = Number.parseInt(casamento[1], 10);
    const coluna = Number.parseInt(casamento[2], 10);
    console.error(formatarDiagnosticos(codigoFonte, [{ linha, coluna, mensagem }], '').join('\n'));
  } else {
    console.error(mensagem);
  }
  process.exit(1);
}
}
