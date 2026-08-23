/* Playground THZ-LANG — Studio IDE & Playground v2.3
 * Integra: Monaco Multi-Abas, Language Service Core (G1) e Interpretador Tree-Walking no browser.
 */
import * as monaco from 'monaco-editor';
import { thzMonarch } from './thz-monarch.js';
import { analisar, obterHover, auditarFonte } from '../src/language-service.js';
import { InterpretadorThz, valorThzDe } from '../src/interpretador.js';
import { ArenaMemoria } from '../src/runtime.js';
import type { ProgramaAST, EstruturaAST, OperacaoAST } from '../src/types.js';
import { ThzDocGen } from '../src/docgen.js';
import { gerarMarkdownGovernanca } from '../src/governanca.js';
import { baixarParaIr, serializarIr, emitirLlvm } from '../src/ir.js';
import { formatar } from '../src/fmt.js';
import type { ValorThz } from '../src/interpretador.js';

import faturamentoRaw from '../exemplos/faturamento.thz?raw';
import pedidosRaw from '../exemplos/pedidos.thz?raw';
import agendaRaw from '../exemplos/agenda.thz?raw';

// Registra linguagem THZ no Monaco
monaco.languages.register({ id: 'thz' });
monaco.languages.setMonarchTokensProvider('thz', thzMonarch as any);
monaco.languages.setLanguageConfiguration('thz', {
  comments: { lineComment: '#' },
  brackets: [['(', ')'], ['[', ']']],
  autoClosingPairs: [
    { open: '"', close: '"' },
    { open: '(', close: ')' },
    { open: '[', close: ']' },
  ],
});

// Hover via Language Service
monaco.languages.registerHoverProvider('thz', {
  provideHover(model, position) {
    const fonte = model.getValue();
    const h = obterHover(fonte, position.lineNumber, position.column);
    if (!h) return null;
    return {
      range: new monaco.Range(h.range!.linha, h.range!.coluna, h.range!.linha, h.range!.coluna + h.range!.comprimento),
      contents: [{ value: h.conteudo }],
    };
  },
});

// Exemplos Canônicos Iniciais
const EXEMPLOS: Record<string, string> = {
  faturamento: faturamentoRaw,
  pedidos: pedidosRaw,
  agenda: agendaRaw,
  minimo: `VERSAO_LINGUAGEM "2.3"
PROGRAMA DemoMinimo

METADADOS_ARQUITETURA
  DOMINIO: "Exemplo"
  SLO_LATENCIA_MAXIMA: "10ms"
  CONFORMIDADE: "SOX-404"
FIM_METADADOS

ESTRUTURA Item LAYOUT_COLUNAR
  quantidade : NATURAL32
  valor_unit : DECIMAL(12, 4)
  INVARIANTE valor_unit >= 0.0000
FIM_ESTRUTURA

REGRA_NEGOCIO CalculoTotal
  IDENTIFICADOR_REGRA: "REG-001"
  RASTREIO_REQUISITO: "REQ-001"
  
  CONTRATO_ENTRADA
    EXIGE itens.quantidade > 0
  FIM_CONTRATO_ENTRADA

  OPERACAO Somar(itens: FATIA[Item]) : DECIMAL(18, 4)
  INICIO
    VARIAVEL acumulador : DECIMAL(18, 4) <- 0.0000
    VETORIZAR_PARA item EM itens PASSO_SIMD 8
      acumulador <- acumulador + item.quantidade * item.valor_unit
    FIM_PARA
    RETORNE acumulador
  FIM
FIM_REGRA_NEGOCIO

FIM_PROGRAMA`,
};

// ---- Gerenciamento de Abas Multi-Arquivo ----
interface AbaArquivo {
  id: string;
  nome: string;
  modelo: monaco.editor.ITextModel;
}

const abas: AbaArquivo[] = [];
let abaAtivaId = '';

const editorContainer = document.getElementById('editor')!;
const editor = monaco.editor.create(editorContainer, {
  language: 'thz',
  theme: 'vs-dark',
  fontSize: 13,
  fontFamily: "'JetBrains Mono', Consolas, 'Courier New', monospace",
  minimap: { enabled: false },
  automaticLayout: true,
  wordWrap: 'on',
  lineNumbers: 'on',
  renderWhitespace: 'selection',
  scrollBeyondLastLine: false,
});

// DOM Refs
const editorTabsEl = document.getElementById('editorTabs') as HTMLDivElement;
const btnNovaAba = document.getElementById('btnNovaAba') as HTMLButtonElement;
const saidaEl = document.getElementById('saida') as HTMLDivElement;
const diagEl = document.getElementById('diagnosticos') as HTMLDivElement;
const diagBadge = document.getElementById('diagBadge') as HTMLSpanElement;
const hoverBox = document.getElementById('hoverBox') as HTMLDivElement;
const auditOutput = document.getElementById('auditOutput') as HTMLDivElement;
const irOutput = document.getElementById('irOutput') as HTMLDivElement;
const docOutput = document.getElementById('docOutput') as HTMLDivElement;
const statusLine = document.getElementById('statusLine') as HTMLSpanElement;
const statusDot = document.getElementById('statusDot') as HTMLSpanElement;
const simbolosInfo = document.getElementById('simbolosInfo') as HTMLSpanElement;
const cursorPos = document.getElementById('cursorPos') as HTMLSpanElement;

// Seleção de Abas no Painel de Saída
function selecionarAbaSaida(tabId: string) {
  document.querySelectorAll('.output-tab').forEach((t) => {
    t.classList.toggle('active', t.getAttribute('data-tab') === tabId);
  });
  document.querySelectorAll('.tab-pane').forEach((p) => {
    p.classList.toggle('active', p.id === `tab-${tabId}`);
  });
}

document.querySelectorAll('.output-tab').forEach((tabBtn) => {
  tabBtn.addEventListener('click', () => {
    const alvo = tabBtn.getAttribute('data-tab');
    if (alvo) selecionarAbaSaida(alvo);
  });
});

// Renderização e Navegação de Abas do Editor
function renderizarAbasEditor() {
  editorTabsEl.innerHTML = '';
  abas.forEach((aba) => {
    const tabEl = document.createElement('button');
    tabEl.className = `editor-tab ${aba.id === abaAtivaId ? 'active' : ''}`;
    
    const titleSpan = document.createElement('span');
    titleSpan.textContent = aba.nome;
    tabEl.appendChild(titleSpan);

    if (abas.length > 1) {
      const closeBtn = document.createElement('span');
      closeBtn.className = 'close-tab';
      closeBtn.innerHTML = '&times;';
      closeBtn.title = 'Fechar aba';
      closeBtn.onclick = (e) => {
        e.stopPropagation();
        fecharAba(aba.id);
      };
      tabEl.appendChild(closeBtn);
    }

    tabEl.onclick = () => alternarAba(aba.id);
    editorTabsEl.appendChild(tabEl);
  });
}

function alternarAba(id: string) {
  const aba = abas.find((a) => a.id === id);
  if (!aba) return;
  abaAtivaId = id;
  editor.setModel(aba.modelo);
  renderizarAbasEditor();
  renderDiagnosticos(aba.modelo.getValue());
}

function criarAba(nome: string, conteudo: string): AbaArquivo {
  const id = 'aba_' + Math.random().toString(36).substring(2, 9);
  const modelo = monaco.editor.createModel(conteudo, 'thz');
  const novaAba: AbaArquivo = { id, nome, modelo };
  abas.push(novaAba);
  alternarAba(id);
  return novaAba;
}

function fecharAba(id: string) {
  const idx = abas.findIndex((a) => a.id === id);
  if (idx === -1 || abas.length <= 1) return;
  const aba = abas[idx];
  aba.modelo.dispose();
  abas.splice(idx, 1);
  if (abaAtivaId === id) {
    const novoIdx = Math.max(0, idx - 1);
    alternarAba(abas[novoIdx].id);
  } else {
    renderizarAbasEditor();
  }
}

btnNovaAba.onclick = () => {
  const num = abas.length + 1;
  criarAba(`arquivo${num}.thz`, `VERSAO_LINGUAGEM "2.3"\nPROGRAMA Programa${num}\n\nPROCEDIMENTO Principal()\nINICIO\n    EXIBA "Executando Programa ${num}!"\nFIM\n\nFIM_PROGRAMA\n`);
};

// Diagnósticos e Lint
function renderDiagnosticos(fonte: string, estrito = false) {
  const r = analisar(fonte, { estrito });
  const markers: monaco.editor.IMarkerData[] = r.diagnosticos.map((d) => ({
    startLineNumber: d.linha,
    startColumn: d.coluna,
    endLineNumber: d.linha,
    endColumn: d.coluna + 12,
    message: d.mensagem,
    severity: monaco.MarkerSeverity.Error,
  }));
  const model = editor.getModel();
  if (model) monaco.editor.setModelMarkers(model, 'thz', markers);

  diagEl.innerHTML = '';
  diagBadge.textContent = String(r.diagnosticos.length);
  if (r.diagnosticos.length === 0) {
    statusDot.style.background = '#10b981';
    const ok = document.createElement('div');
    ok.className = 'diag-item ok';
    ok.textContent = estrito ? '✓ Nenhum diagnóstico — Lint estrito aprovado com sucesso!' : '✓ Nenhum erro encontrado. Código válido.';
    diagEl.appendChild(ok);
  } else {
    statusDot.style.background = '#ef4444';
    for (const bloco of r.textoDiagnosticos) {
      const div = document.createElement('div');
      div.className = 'diag-item';
      div.textContent = bloco;
      diagEl.appendChild(div);
    }
  }

  simbolosInfo.textContent = `${r.simbolos.length} símbolos • ${r.tokens?.length ?? 0} tokens`;
  statusLine.textContent = r.temErros ? `${r.diagnosticos.length} erro(s)` : estrito ? 'OK (estrito)' : 'OK';
  return r;
}

// Event Listeners no Editor
let timer: number | undefined;
editor.onDidChangeModelContent(() => {
  window.clearTimeout(timer);
  timer = window.setTimeout(() => renderDiagnosticos(editor.getValue()), 300) as unknown as number;
});

editor.onDidChangeCursorPosition((e) => {
  cursorPos.textContent = `Ln ${e.position.lineNumber}, Col ${e.position.column}`;
  const h = obterHover(editor.getValue(), e.position.lineNumber, e.position.column);
  hoverBox.textContent = h ? h.conteudo : '— Posicione o cursor sobre um símbolo no editor para ver sua assinatura —';
});

// Ações do Motor
const LOTE_FATURAMENTO: unknown[][] = [
  ['a1b2c3d4-0000-0000-0000-000000000001', 'PROD-SKU-901', 10, '150.5000', '18.00', '0'],
  ['a1b2c3d4-0000-0000-0000-000000000002', 'PROD-SKU-902', 5, '320.0000', '12.00', '0'],
];

function valorDemo(tipo: string, linha: number): unknown {
  if (/^NATURAL|INTEIRO/.test(tipo)) return 5 + linha * 5;
  if (tipo.startsWith('DECIMAL')) return (100 + linha * 50).toFixed(4);
  if (tipo.startsWith('MONETARIO')) return (100 + linha * 50).toFixed(2);
  if (tipo === 'TEXTO') return `VAL-${linha + 1}`;
  if (tipo === 'UUID') return `00000000-0000-0000-0000-00000000000${linha + 1}`;
  if (tipo === 'LOGICO') return linha % 2 === 0;
  return `VAL-${linha + 1}`;
}

function registroDe(estrutura: EstruturaAST, valores: unknown[], validar?: (v: ValorThz) => void): ValorThz {
  const campos = new Map<string, ValorThz>();
  estrutura.campos.forEach((campo, i) => {
    const bruto = valores[i];
    if (bruto !== undefined) {
      try { campos.set(campo.nome, valorThzDe(campo.tipo, bruto)); return; } catch {}
    }
    const demo = valorDemo(campo.tipo, i);
    try { campos.set(campo.nome, valorThzDe(campo.tipo, demo)); }
    catch { campos.set(campo.nome, valorThzDe(campo.tipo, '0')); }
  });
  const reg: ValorThz = { classe: 'REGISTRO', nomeEstrutura: estrutura.nome, campos };
  if (validar) validar(reg);
  return reg;
}

function construirArgumentos(operacao: OperacaoAST, ast: ProgramaAST, validar?: (v: ValorThz) => void): Record<string, ValorThz> {
  const args: Record<string, ValorThz> = {};
  for (const p of operacao.parametros) {
    const m = /^FATIA\[(\w+)\]$/.exec(p.tipo);
    if (m) {
      const est = ast.estruturas.find((e) => e.nome === m[1]);
      if (!est) throw new Error(`Estrutura '${m[1]}' não declarada.`);
      const lote = est.nome === 'ItemFatura' && est.campos.length === 6 ? LOTE_FATURAMENTO : [est.campos.map((c, i) => valorDemo(c.tipo, i))];
      args[p.nome] = { classe: 'FATIA', tipoInterno: m[1], elementos: lote.map((linha) => registroDe(est, linha, validar)) };
    } else {
      try { args[p.nome] = valorThzDe(p.tipo, 0); }
      catch { args[p.nome] = valorThzDe('TEXTO', ''); }
    }
  }
  return args;
}

function executar(estrito = false) {
  selecionarAbaSaida('saida');
  const t0 = performance.now();
  const fonte = editor.getValue();
  const r = renderDiagnosticos(fonte, estrito);
  if (r.temErros && !estrito) {
    saidaEl.textContent = 'Corrija os diagnósticos antes de executar.\n\n' + r.textoDiagnosticos.join('\n\n');
    return;
  }
  if (!r.ast) {
    saidaEl.textContent = 'Sem AST — corrija os erros léxicos/sintáticos.';
    return;
  }

  const arena = new ArenaMemoria(64);
  arena.alocar(2048);
  const dom = r.ast.metadados?.dominio ?? 'Geral';
  const slo = r.ast.metadados?.sloLatencia ?? 'N/A';
  const conf = r.ast.metadados?.conformidade.join(', ') ?? 'N/A';
  const linhas: string[] = [];
  linhas.push('═'.repeat(64));
  linhas.push(`  THZ-LANG RUNTIME :: ${r.ast.nome}`);
  linhas.push('═'.repeat(64));
  linhas.push(`[ARQUITETURA] Domínio: ${dom} | SLO: ${slo}`);
  linhas.push(`[CONFORMIDADE] ${conf}\n`);

  const interp = new InterpretadorThz(r.ast, { saida: (l) => linhas.push(l) });
  
  // Procura PROCEDIMENTO Principal primeiro
  const procPrincipal = r.ast.procedimentos?.find((p) => p.nome === 'Principal');
  if (procPrincipal) {
    linhas.push(`[PROCEDIMENTO] Principal()\n`);
    try {
      interp.executarProcedimento('Principal', {});
      const elapsed = (performance.now() - t0).toFixed(2);
      linhas.push('─'.repeat(64));
      linhas.push(`[STATUS] Execução concluída com sucesso em ${elapsed}ms.`);
      linhas.push(`[MEMÓRIA] ArenaMemoria descartada em O(1).`);
    } catch (e) {
      linhas.push(`[ERRO DE EXECUÇÃO] ${(e as Error).message}`);
    } finally {
      arena.liberarTudo();
    }
    saidaEl.textContent = linhas.join('\n');
    return;
  }

  // Caso contrário, busca operação executável
  const execs = interp.listarOperacoesExecutaveis();
  if (execs.length === 0) {
    linhas.push('[AVISO] Nenhum PROCEDIMENTO Principal ou OPERAÇÃO com corpo (INICIO...FIM) encontrado.');
    saidaEl.textContent = linhas.join('\n');
    return;
  }

  const alvo = execs[0];
  linhas.push(`[REGRA] ${alvo.regra.nome}${alvo.regra.identificador ? ` (${alvo.regra.identificador})` : ''} :: ${alvo.operacao.nome}()\n`);
  try {
    const resultado = interp.executarOperacao(alvo.operacao.nome, construirArgumentos(alvo.operacao, r.ast, (v) => interp.validarInvariantes(v)));
    linhas.push('─'.repeat(64));
    if (resultado?.classe === 'DECIMAL') linhas.push(`[RESULTADO] ${resultado.valor.formatar()}`);
    else if (resultado) linhas.push(`[RESULTADO] ${interp.formatar(resultado)}`);
    else linhas.push('[RESULTADO] (sem retorno)');
    const elapsed = (performance.now() - t0).toFixed(2);
    linhas.push(`\n[PERFORMANCE] Tempo total: ${elapsed}ms | Arena liberada em O(1).`);
  } catch (e) {
    linhas.push(`[ERRO DE EXECUÇÃO] ${(e as Error).message}`);
  } finally {
    arena.liberarTudo();
  }
  saidaEl.textContent = linhas.join('\n');
}

function gerarDoc() {
  selecionarAbaSaida('doc');
  const fonte = editor.getValue();
  const r = analisar(fonte);
  if (!r.ast || r.temErros) {
    docOutput.textContent = 'Não é possível gerar documentação com erros de sintaxe.\n\n' + r.textoDiagnosticos.join('\n\n');
    return;
  }
  docOutput.textContent = ThzDocGen.gerarMarkdown(r.ast);
}

function gerarAudit() {
  selecionarAbaSaida('audit');
  const fonte = editor.getValue();
  const { resultado, auditoria } = auditarFonte(fonte, { estrito: false });
  if (!auditoria) {
    auditOutput.textContent = 'Não é possível auditar — corrija os erros sintáticos.\n\n' + resultado.textoDiagnosticos.join('\n\n');
    return;
  }
  auditOutput.textContent = gerarMarkdownGovernanca(auditoria);
}

function aplicarFmt() {
  const fonte = editor.getValue();
  const r = analisar(fonte);
  if (!r.ast) {
    saidaEl.textContent = 'Não é possível formatar — corrija erros antes.\n\n' + r.textoDiagnosticos.join('\n\n');
    return;
  }
  const formatado = formatar(r.ast);
  if (formatado !== fonte) {
    editor.setValue(formatado);
    saidaEl.textContent = '✨ Código formatado com sucesso segundo as regras canônicas.';
  } else {
    saidaEl.textContent = '✓ O código já está em formato canônico.';
  }
}

function gerarIr(modoLlvm = false) {
  selecionarAbaSaida('ir');
  const fonte = editor.getValue();
  const r = analisar(fonte);
  if (!r.ast) {
    irOutput.textContent = 'Não é possível gerar IR — corrija os erros sintáticos.\n\n' + r.textoDiagnosticos.join('\n\n');
    return;
  }
  const ir = baixarParaIr(r.ast);
  irOutput.textContent = modoLlvm ? emitirLlvm(ir) : serializarIr(ir);
}

// Bindings da Barra de Ferramentas
(document.getElementById('btnRun') as HTMLButtonElement).onclick = () => executar(false);
(document.getElementById('btnCheck') as HTMLButtonElement).onclick = () => {
  selecionarAbaSaida('diagnosticos');
  renderDiagnosticos(editor.getValue(), false);
};
(document.getElementById('btnCheckEstrito') as HTMLButtonElement).onclick = () => {
  selecionarAbaSaida('diagnosticos');
  renderDiagnosticos(editor.getValue(), true);
};
(document.getElementById('btnDoc') as HTMLButtonElement).onclick = gerarDoc;
(document.getElementById('btnAudit') as HTMLButtonElement).onclick = gerarAudit;
(document.getElementById('btnIr') as HTMLButtonElement).onclick = () => gerarIr(false);
(document.getElementById('btnLlvm') as HTMLButtonElement).onclick = () => gerarIr(true);
(document.getElementById('btnFmt') as HTMLButtonElement).onclick = aplicarFmt;
(document.getElementById('btnLimpar') as HTMLButtonElement).onclick = () => {
  saidaEl.textContent = '';
  auditOutput.textContent = '';
  irOutput.textContent = '';
  docOutput.textContent = '';
};

// Atalhos de Teclado
editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, () => aplicarFmt());
editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => executar(false));

// Seletor de Exemplos
(document.getElementById('exemploSelect') as HTMLSelectElement).onchange = (e) => {
  const v = (e.target as HTMLSelectElement).value;
  if (!v || !EXEMPLOS[v]) return;
  const abaExistente = abas.find((a) => a.nome === `${v}.thz`);
  if (abaExistente) {
    alternarAba(abaExistente.id);
  } else {
    criarAba(`${v}.thz`, EXEMPLOS[v]);
  }
  (e.target as HTMLSelectElement).value = '';
};

// Inicialização com Abas Canônicas
criarAba('faturamento.thz', EXEMPLOS.faturamento);
criarAba('pedidos.thz', EXEMPLOS.pedidos);
criarAba('agenda.thz', EXEMPLOS.agenda);
alternarAba(abas[0].id);
