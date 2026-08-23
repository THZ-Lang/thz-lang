/* THZ-LANG — LSP Server (G3)
 * Alimentado pelo Language Service Core (G1): analisar() + obterHover().
 * Protocolo: vscode-languageserver (stdio).
 */
import {
  createConnection,
  TextDocuments,
  ProposedFeatures,
  InitializeParams,
  InitializeResult,
  TextDocumentSyncKind,
  DiagnosticSeverity,
  Hover,
  MarkupKind,
  CompletionItem,
  CompletionItemKind,
  DocumentSymbol,
  SymbolKind,
  Definition,
  Location,
  DidChangeConfigurationNotification,
} from 'vscode-languageserver/node';
import { TextDocument } from 'vscode-languageserver-textdocument';
import { analisar, obterHover } from '../language-service.js';
import { auditar, gerarMarkdownGovernanca } from '../governanca.js';
import { baixarParaIr, serializarIr, emitirLlvm } from '../ir.js';
import { formatar } from '../fmt.js';
import { PALAVRAS_RESERVADAS } from '../keywords.js';

const connection = createConnection(ProposedFeatures.all);
const documents = new TextDocuments(TextDocument);

// Config: --estrito via settings.thz-lang.lintEstrito
let lintEstrito = false;

connection.onInitialize((_params: InitializeParams): InitializeResult => {
  return {
    capabilities: {
      textDocumentSync: TextDocumentSyncKind.Incremental,
      hoverProvider: true,
      completionProvider: { triggerCharacters: ['.', ' ', ':'] },
      documentSymbolProvider: true,
      definitionProvider: true,
      documentFormattingProvider: true,
    },
  };
});

connection.onInitialized(() => {
  connection.client.register(DidChangeConfigurationNotification.type, undefined);
});

connection.onDidChangeConfiguration((params) => {
  const cfg = (params.settings as any)?.['thz-lang'];
  if (cfg && typeof cfg.lintEstrito === 'boolean') lintEstrito = cfg.lintEstrito;
});

function toLspSeverity(_origem: string): DiagnosticSeverity {
  return DiagnosticSeverity.Error;
}

function validar(doc: TextDocument): void {
  const fonte = doc.getText();
  const r = analisar(fonte, { estrito: lintEstrito });
  const diags: any[] = r.diagnosticos.map((d) => {
    const line = Math.max(0, d.linha - 1);
    const col = Math.max(0, d.coluna - 1);
    const linhas = fonte.split(/\r?\n/);
    const conteudo = linhas[d.linha - 1] ?? '';
    let fim = col;
    while (fim < conteudo.length && /[A-Za-z0-9_"]/.test(conteudo[fim] ?? '')) fim++;
    if (fim === col) fim = col + 1;
    return {
      severity: toLspSeverity(d.origem),
      range: { start: { line, character: col }, end: { line, character: fim } },
      message: d.mensagem,
      source: `thz-${d.origem}`,
    };
  });
  // G4 — pendências de governança como diagnósticos adicionais
  if (r.ast) {
    const auditoria = auditar(r.ast, { estrito: lintEstrito });
    for (const p of auditoria.pendencias) {
      // Se já existe diagnóstico equivalente (mensagem idêntica), evita duplicar
      if (diags.some((d: any) => d.message === p)) continue;
      diags.push({
        severity: DiagnosticSeverity.Warning,
        range: { start: { line: 0, character: 0 }, end: { line: 0, character: 1 } },
        message: `[Governança] ${p}`,
        source: 'thz-governanca',
      });
    }
  }
  connection.sendDiagnostics({ uri: doc.uri, diagnostics: diags });
}

documents.onDidOpen((e) => validar(e.document));
documents.onDidChangeContent((e) => validar(e.document));
documents.onDidSave((e) => validar(e.document));

connection.onHover((params): Hover | null => {
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return null;
  const fonte = doc.getText();
  const linha = params.position.line + 1;
  const coluna = params.position.character + 1;
  const h = obterHover(fonte, linha, coluna, { estrito: lintEstrito });
  if (!h) return null;
  return {
    contents: { kind: MarkupKind.Markdown, value: h.conteudo },
    range: h.range
      ? {
          start: { line: h.range.linha - 1, character: h.range.coluna - 1 },
          end: { line: h.range.linha - 1, character: h.range.coluna - 1 + h.range.comprimento },
        }
      : undefined,
  };
});

function simboloParaKind(cat: string): SymbolKind {
  switch (cat) {
    case 'programa': return SymbolKind.Package;
    case 'estrutura': return SymbolKind.Struct;
    case 'campo': return SymbolKind.Field;
    case 'enumeracao': return SymbolKind.Enum;
    case 'membro-enum': return SymbolKind.EnumMember;
    case 'regra': return SymbolKind.Module;
    case 'operacao': return SymbolKind.Method;
    case 'parametro': return SymbolKind.Variable;
    case 'variavel': return SymbolKind.Variable;
    default: return SymbolKind.Variable;
  }
}

connection.onDocumentSymbol((params): DocumentSymbol[] => {
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return [];
  const r = analisar(doc.getText(), { estrito: lintEstrito });
  return r.simbolos.map((s) => ({
    name: s.nome,
    detail: s.detalhe ?? s.container ?? '',
    kind: simboloParaKind(s.categoria),
    range: {
      start: { line: s.linha - 1, character: s.coluna - 1 },
      end: { line: s.linha - 1, character: s.coluna - 1 + s.nome.length },
    },
    selectionRange: {
      start: { line: s.linha - 1, character: s.coluna - 1 },
      end: { line: s.linha - 1, character: s.coluna - 1 + s.nome.length },
    },
  }));
});

connection.onDefinition((params): Definition | null => {
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return null;
  const fonte = doc.getText();
  const linha = params.position.line + 1;
  const coluna = params.position.character + 1;
  const linhas = fonte.split(/\r?\n/);
  const conteudo = linhas[linha - 1] ?? '';
  let idx = Math.max(0, coluna - 1);
  while (idx > 0 && /[A-Za-z0-9_]/.test(conteudo[idx - 1] ?? '')) idx--;
  let fim = idx;
  while (fim < conteudo.length && /[A-Za-z0-9_]/.test(conteudo[fim] ?? '')) fim++;
  const palavra = conteudo.slice(idx, fim);
  if (!palavra) return null;
  const r = analisar(fonte, { estrito: lintEstrito });
  const alvo = r.simbolos.find((s) => s.nome === palavra);
  if (!alvo) return null;
  return Location.create(params.textDocument.uri, {
    start: { line: alvo.linha - 1, character: alvo.coluna - 1 },
    end: { line: alvo.linha - 1, character: alvo.coluna - 1 + alvo.nome.length },
  });
});

connection.onCompletion((): CompletionItem[] => {
  const kw = Object.keys(PALAVRAS_RESERVADAS).map((label) => ({
    label,
    kind: CompletionItemKind.Keyword,
    detail: 'THZ-LANG keyword',
  }));
  const tipos = ['TEXTO','LOGICO','UUID','NATURAL32','DECIMAL(12, 4)','MONETARIO("BRL")','FATIA[Item]','RESULTADO[T, E]'].map((label) => ({
    label,
    kind: CompletionItemKind.TypeParameter,
    detail: 'tipo THZ',
  }));
  return [...kw, ...tipos];
});

// G4 — comando custom para auditoria sob demanda (usado pela extensão via `thz/audit`)
(connection as any).onRequest('thz/audit', (params: { uri: string }) => {
  const doc = documents.get(params.uri);
  if (!doc) return { error: 'Documento não encontrado' };
  const r = analisar(doc.getText(), { estrito: lintEstrito });
  if (!r.ast) return { error: 'AST indisponível — corrija erros léxico/sintáticos', diagnostics: r.diagnosticos };
  const auditoria = auditar(r.ast, { estrito: lintEstrito });
  return { markdown: gerarMarkdownGovernanca(auditoria), json: auditoria };
});

// G5 — IR estável + ponte LLVM (usado pela extensão via `thz/ir` / `thz/llvm`)
(connection as any).onRequest('thz/ir', (params: { uri: string }) => {
  const doc = documents.get(params.uri);
  if (!doc) return { error: 'Documento não encontrado' };
  const r = analisar(doc.getText(), { estrito: lintEstrito });
  if (!r.ast) return { error: 'AST indisponível', diagnostics: r.diagnosticos };
  const ir = baixarParaIr(r.ast);
  return { json: ir, text: serializarIr(ir) };
});
(connection as any).onRequest('thz/llvm', (params: { uri: string }) => {
  const doc = documents.get(params.uri);
  if (!doc) return { error: 'Documento não encontrado' };
  const r = analisar(doc.getText(), { estrito: lintEstrito });
  if (!r.ast) return { error: 'AST indisponível', diagnostics: r.diagnosticos };
  const ir = baixarParaIr(r.ast);
  return { text: emitirLlvm(ir), json: ir };
});

connection.onDocumentFormatting((params) => {
  const doc = documents.get(params.textDocument.uri);
  if (!doc) return [];
  const r = analisar(doc.getText(), { estrito: lintEstrito });
  if (!r.ast) return [];
  const fmt = formatar(r.ast);
  const text = doc.getText();
  if (fmt === text) return [];
  const end = doc.positionAt(text.length);
  return [
    {
      range: {
        start: { line: 0, character: 0 },
        end,
      },
      newText: fmt,
    },
  ];
});

documents.listen(connection);
connection.listen();
