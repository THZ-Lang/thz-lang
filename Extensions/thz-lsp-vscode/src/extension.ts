import * as fs from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';
import {
  LanguageClient,
  LanguageClientOptions,
  ServerOptions,
  Executable,
} from 'vscode-languageclient/node';
import { ThzDebugConfigurationProvider, ThzDebugAdapterDescriptorFactory } from './debugAdapter';

let client: LanguageClient | undefined;
let thzTerminal: vscode.Terminal | undefined;

export function activate(context: vscode.ExtensionContext): void {
  // ==========================================================================
  // 0. DEPURAÇÃO NATIVA E RUN & DEBUG (DAP - Debug Adapter Protocol)
  // ==========================================================================
  context.subscriptions.push(
    vscode.debug.registerDebugConfigurationProvider('thz', new ThzDebugConfigurationProvider()),
    vscode.debug.registerDebugAdapterDescriptorFactory('thz', new ThzDebugAdapterDescriptorFactory())
  );
  const config = vscode.workspace.getConfiguration('thz-lang');
  const customJar = config.get<string>('lspJarPath');

  const candidatos = [
    customJar,
    // Empacotado (vsix): servidor JAR embutido na pasta server
    context.asAbsolutePath(path.join('server', 'thz-lsp-2.3.0.jar')),
    context.asAbsolutePath(path.join('server', 'thz-lsp.jar')),
    // Desenvolvimento no repositório JVM:
    path.resolve(context.extensionPath, '..', '..', 'JVM', 'thz-lsp-jvm', 'build', 'libs', 'thz-lsp-2.3.0.jar'),
    path.resolve(context.extensionPath, '..', '..', 'JVM', 'thz-lsp-jvm', 'target', 'thz-lsp-2.3.0.jar'),
  ].filter((c): c is string => typeof c === 'string' && c.trim().length > 0);

  let jarPath: string | undefined;
  for (const cand of candidatos) {
    if (fs.existsSync(cand)) {
      jarPath = cand;
      break;
    }
  }

  if (!jarPath) {
    vscode.window.showWarningMessage(
      'THZ-LANG: Servidor LSP Java (thz-lsp-2.3.0.jar) não foi encontrado. Execute "./gradlew :thz-lsp-jvm:jar" para compilá-lo.'
    );
    jarPath = candidatos[candidatos.length - 1];
  }

  const javaExecutable: Executable = {
    command: 'java',
    args: ['-jar', jarPath, '--stdio'],
    options: {
      env: process.env,
    },
  };

  const serverOptions: ServerOptions = {
    run: javaExecutable,
    debug: javaExecutable,
  };

  const clientOptions: LanguageClientOptions = {
    documentSelector: [{ scheme: 'file', language: 'thz' }],
    synchronize: {
      fileEvents: vscode.workspace.createFileSystemWatcher('**/*.thz'),
    },
  };

  client = new LanguageClient('thz-lang', 'THZ-LANG Language Server', serverOptions, clientOptions);
  client.start();
  context.subscriptions.push({ dispose: () => client?.stop() } as vscode.Disposable);

  // ==========================================================================
  // 1. EXECUÇÃO NO TERMINAL INTEGRADO
  // ==========================================================================

  function resolverComandoCli(filePath?: string, argsExtras: string = ''): { comando: string; cwd?: string } {
    const customCli = vscode.workspace.getConfiguration('thz-lang').get<string>('cliPath');
    const ed = vscode.window.activeTextEditor;
    const workspaceFolder = ed ? vscode.workspace.getWorkspaceFolder(ed.document.uri) : (vscode.workspace.workspaceFolders?.[0]);
    const root = workspaceFolder ? workspaceFolder.uri.fsPath : undefined;
    const isWin = process.platform === 'win32';

    const targetArg = filePath ? `"${filePath}"` : '';
    const fullArgs = [argsExtras, targetArg].filter(s => s.trim().length > 0).join(' ');

    if (customCli && customCli.trim().length > 0) {
      if (customCli.endsWith('.jar')) {
        return { comando: `java -jar "${customCli}" ${fullArgs}`, cwd: root };
      }
      return { comando: `"${customCli}" ${fullArgs}`, cwd: root };
    }

    if (root) {
      const thzCmd = path.join(root, 'thz.cmd');
      const thzPs1 = path.join(root, 'thz.ps1');
      const gradlewBat = path.join(root, 'gradlew.bat');
      const gradlewSh = path.join(root, 'gradlew');
      const cliJar = path.join(root, 'JVM', 'thz-cli-jvm', 'build', 'libs', 'thz-jvm-2.3.3.jar');
      const cliShadowJar = path.join(root, 'JVM', 'thz-cli-jvm', 'build', 'libs', 'thz-cli-jvm-2.3.3-all.jar');

      if (fs.existsSync(cliJar)) {
        return { comando: `java -jar "${cliJar}" ${fullArgs}`, cwd: root };
      }
      if (fs.existsSync(cliShadowJar)) {
        return { comando: `java -jar "${cliShadowJar}" ${fullArgs}`, cwd: root };
      }
      if (isWin && fs.existsSync(thzCmd)) {
        return { comando: `"${thzCmd}" ${fullArgs}`, cwd: root };
      }
      if (isWin && fs.existsSync(thzPs1)) {
        return { comando: `powershell -ExecutionPolicy Bypass -File "${thzPs1}" ${fullArgs}`, cwd: root };
      }
      if (isWin && fs.existsSync(gradlewBat)) {
        return { comando: `"${gradlewBat}" :thz-cli-jvm:run --args="${fullArgs.replace(/"/g, '\\"')}"`, cwd: root };
      }
      if (!isWin && fs.existsSync(gradlewSh)) {
        return { comando: `"${gradlewSh}" :thz-cli-jvm:run --args="${fullArgs.replace(/"/g, '\\"')}"`, cwd: root };
      }
    }

    return { comando: `thz ${fullArgs}`, cwd: root };
  }

  function executarNoTerminal(acao: 'run' | 'check', principal?: string): void {
    const ed = vscode.window.activeTextEditor;
    if (!ed) {
      vscode.window.showInformationMessage('Abra um arquivo .thz para executar.');
      return;
    }

    if (ed.document.isDirty) {
      ed.document.save();
    }

    const filePath = ed.document.uri.fsPath;
    const args = principal ? `${acao} --principal "${principal}"` : acao;
    const { comando, cwd } = resolverComandoCli(filePath, args);

    if (!thzTerminal || thzTerminal.exitStatus !== undefined) {
      thzTerminal = vscode.window.createTerminal({
        name: 'THZ-LANG',
        cwd: cwd,
      });
    }

    thzTerminal.show(true);
    thzTerminal.sendText(comando);
  }

  // Comandos de Execução
  context.subscriptions.push(
    vscode.commands.registerCommand('thz.run', () => executarNoTerminal('run')),
    vscode.commands.registerCommand('thz.check', () => executarNoTerminal('check')),
    vscode.commands.registerCommand('thz.runTarget', (target: string) => executarNoTerminal('run', target)),

    vscode.commands.registerCommand('thz.openRepl', () => {
      const { comando, cwd } = resolverComandoCli(undefined, 'repl');
      const replTerm = vscode.window.createTerminal({ name: 'THZ REPL', cwd: cwd });
      replTerm.show(true);
      replTerm.sendText(comando);
    }),

    vscode.commands.registerCommand('thz.openGui', () => {
      const ed = vscode.window.activeTextEditor;
      const filePath = ed ? ed.document.uri.fsPath : undefined;
      const { comando, cwd } = resolverComandoCli(filePath, 'gui');
      const guiTerm = vscode.window.createTerminal({ name: 'THZ Desktop IDE', cwd: cwd });
      guiTerm.show(true);
      guiTerm.sendText(comando);
    })
  );

  // ==========================================================================
  // 2. CODELENS PROVIDER
  // ==========================================================================

  class ThzCodeLensProvider implements vscode.CodeLensProvider {
    provideCodeLenses(document: vscode.TextDocument): vscode.CodeLens[] {
      const lenses: vscode.CodeLens[] = [];
      const isEnabled = vscode.workspace.getConfiguration('thz-lang').get<boolean>('codeLens', true);
      if (!isEnabled) return lenses;

      const text = document.getText();
      const lines = text.split(/\r?\n/);

      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];

        // Regra de Negócio
        const matchRegra = line.match(/^\s*REGRA_NEGOCIO\s+([A-Za-z0-9_]+)/);
        if (matchRegra) {
          const nomeRegra = matchRegra[1];
          const range = new vscode.Range(i, 0, i, line.length);
          lenses.push(
            new vscode.CodeLens(range, {
              title: `▶ Executar Regra (${nomeRegra})`,
              command: 'thz.runTarget',
              arguments: [nomeRegra],
            }),
            new vscode.CodeLens(range, {
              title: '🛡️ Auditar Governança',
              command: 'thz.showAudit',
            }),
            new vscode.CodeLens(range, {
              title: '📐 Ver Arquitetura',
              command: 'thz.previewArchitecture',
            })
          );
        }

        // Operação isolada
        const matchOp = line.match(/^\s*OPERACAO\s+([A-Za-z0-9_]+)/);
        if (matchOp) {
          const nomeOp = matchOp[1];
          const range = new vscode.Range(i, 0, i, line.length);
          lenses.push(
            new vscode.CodeLens(range, {
              title: `▶ Executar Operação: ${nomeOp}()`,
              command: 'thz.runTarget',
              arguments: [nomeOp],
            })
          );
        }

        // Procedimento
        const matchProc = line.match(/^\s*PROCEDIMENTO\s+([A-Za-z0-9_]+)/);
        if (matchProc) {
          const nomeProc = matchProc[1];
          const range = new vscode.Range(i, 0, i, line.length);
          lenses.push(
            new vscode.CodeLens(range, {
              title: `▶ Executar Procedimento: ${nomeProc}()`,
              command: 'thz.runTarget',
              arguments: [nomeProc],
            })
          );
        }
      }

      return lenses;
    }
  }

  context.subscriptions.push(
    vscode.languages.registerCodeLensProvider({ language: 'thz', scheme: 'file' }, new ThzCodeLensProvider())
  );

  // ==========================================================================
  // 3. LIVE PREVIEW DE ARQUITETURA VIVA & DIAGRAMAS MERMAID
  // ==========================================================================

  let architecturePanel: vscode.WebviewPanel | undefined;

  function gerarDiagramaMermaid(fonte: string): { mermaid: string; metadadosHtml: string } {
    let nomeModulo = 'Programa THZ';
    const matchMod = fonte.match(/(?:PROGRAMA(?:\s+VISUAL|\s+NEGOCIO|\s+ARQUITETURA)?|BIBLIOTECA|EXTENSAO|FERRAMENTA|TESTE)\s+([A-Za-z0-9_]+)/);
    if (matchMod) nomeModulo = matchMod[1];

    let dominio = 'N/A', camada = 'N/A', slo = 'N/A', autor = 'N/A', criticidade = 'N/A', versao = '1.0.0';
    const matchDom = fonte.match(/DOMINIO\s*:\s*"([^"]+)"/);
    if (matchDom) dominio = matchDom[1];
    const matchCam = fonte.match(/CAMADA\s*:\s*"([^"]+)"/);
    if (matchCam) camada = matchCam[1];
    const matchSlo = fonte.match(/SLO_LATENCIA_MAXIMA\s*:\s*"([^"]+)"/);
    if (matchSlo) slo = matchSlo[1];
    const matchAut = fonte.match(/AUTOR\s*:\s*"([^"]+)"/);
    if (matchAut) autor = matchAut[1];
    const matchCrit = fonte.match(/CRITICIDADE\s*:\s*"([^"]+)"/);
    if (matchCrit) criticidade = matchCrit[1];
    const matchVer = fonte.match(/VERSAO\s*:\s*"([^"]+)"/);
    if (matchVer) versao = matchVer[1];

    // Extrai regras, operações e estruturas
    const regras: { nome: string; req?: string; br?: string; ops: string[] }[] = [];
    const estruturas: string[] = [];
    const procedimentos: string[] = [];

    const linhas = fonte.split(/\r?\n/);
    let regraAtual: { nome: string; req?: string; br?: string; ops: string[] } | null = null;

    for (const l of linhas) {
      const mEstrutura = l.match(/^\s*ESTRUTURA\s+([A-Za-z0-9_]+)/);
      if (mEstrutura) estruturas.push(mEstrutura[1]);

      const mProc = l.match(/^\s*PROCEDIMENTO\s+([A-Za-z0-9_]+)/);
      if (mProc) procedimentos.push(mProc[1]);

      const mRegra = l.match(/^\s*REGRA_NEGOCIO\s+([A-Za-z0-9_]+)/);
      if (mRegra) {
        regraAtual = { nome: mRegra[1], ops: [] };
        regras.push(regraAtual);
      }
      if (regraAtual) {
        const mBr = l.match(/IDENTIFICADOR_REGRA\s*:\s*"([^"]+)"/);
        if (mBr) regraAtual.br = mBr[1];
        const mReq = l.match(/RASTREIO_REQUISITO\s*:\s*"([^"]+)"/);
        if (mReq) regraAtual.req = mReq[1];
        const mOp = l.match(/^\s*OPERACAO\s+([A-Za-z0-9_]+)/);
        if (mOp) regraAtual.ops.push(mOp[1]);
      }
      if (l.match(/^\s*FIM_REGRA_NEGOCIO/)) {
        regraAtual = null;
      }
    }

    let mm = 'graph TD\n';
    mm += `    subgraph Modulo["🏛️ ${nomeModulo} (${camada})"]\n`;
    mm += `        direction TB\n`;

    if (estruturas.length > 0) {
      mm += `        subgraph Entidades["📦 Entidades & Estruturas"]\n`;
      for (const e of estruturas) {
        mm += `            E_${e}["${e}"]\n`;
      }
      mm += `        end\n`;
    }

    if (regras.length > 0) {
      mm += `        subgraph Regras["🛡️ Regras de Negócio (DDD)"]\n`;
      for (const r of regras) {
        const tituloRegra = r.br ? `[${r.br}] ${r.nome}` : r.nome;
        mm += `            R_${r.nome}["${tituloRegra}"]\n`;
        if (r.req) {
          mm += `            REQ_${r.nome}["📋 ${r.req}"] --> R_${r.nome}\n`;
        }
        for (const op of r.ops) {
          mm += `            R_${r.nome} --> OP_${r.nome}_${op}["⚡ ${op}()"]\n`;
        }
      }
      mm += `        end\n`;
    }

    if (procedimentos.length > 0) {
      mm += `        subgraph Execucao["🚀 Procedimentos"]\n`;
      for (const p of procedimentos) {
        mm += `            P_${p}["${p}()"]\n`;
      }
      mm += `        end\n`;
    }

    mm += `    end\n`;

    const metadadosHtml = `
      <div class="meta-grid">
        <div class="meta-card"><div class="label">Módulo</div><div class="val">${nomeModulo}</div></div>
        <div class="meta-card"><div class="label">Domínio</div><div class="val">${dominio}</div></div>
        <div class="meta-card"><div class="label">Camada</div><div class="val">${camada}</div></div>
        <div class="meta-card"><div class="label">SLO Máximo</div><div class="val highlight">${slo}</div></div>
        <div class="meta-card"><div class="label">Criticidade</div><div class="val">${criticidade}</div></div>
        <div class="meta-card"><div class="label">Versão</div><div class="val">${versao}</div></div>
      </div>
    `;

    return { mermaid: mm, metadadosHtml };
  }

  function renderizarHtmlArquitetura(fonte: string): string {
    const { mermaid, metadadosHtml } = gerarDiagramaMermaid(fonte);
    return `<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>THZ Living Architecture</title>
  <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
  <style>
    :root {
      --bg: #0d1117;
      --card-bg: rgba(22, 27, 34, 0.85);
      --border: rgba(240, 246, 252, 0.1);
      --text: #e6edf3;
      --muted: #8b949e;
      --primary: #58a6ff;
      --accent: #238636;
      --font: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: var(--font); }
    body { background: var(--bg); color: var(--text); padding: 20px; line-height: 1.5; }
    header { margin-bottom: 20px; border-bottom: 1px solid var(--border); padding-bottom: 15px; }
    header h1 { font-size: 1.3rem; display: flex; align-items: center; gap: 8px; color: var(--primary); }
    header p { font-size: 0.85rem; color: var(--muted); margin-top: 4px; }
    .meta-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 12px; margin-bottom: 24px; }
    .meta-card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px; padding: 10px 14px; backdrop-filter: blur(8px); }
    .meta-card .label { font-size: 0.72rem; text-transform: uppercase; color: var(--muted); letter-spacing: 0.5px; }
    .meta-card .val { font-size: 0.95rem; font-weight: 600; margin-top: 2px; color: var(--text); }
    .meta-card .val.highlight { color: #f0883e; }
    .diagram-container { background: var(--card-bg); border: 1px solid var(--border); border-radius: 10px; padding: 20px; overflow-x: auto; display: flex; justify-content: center; }
    .footer { margin-top: 20px; font-size: 0.75rem; color: var(--muted); text-align: center; }
  </style>
</head>
<body>
  <header>
    <h1>📐 THZ-LANG — Arquitetura Viva & DDD</h1>
    <p>Visualização em tempo real de contratos, regras de negócio e fluxo do sistema</p>
  </header>
  ${metadadosHtml}
  <div class="diagram-container">
    <pre class="mermaid">
${mermaid}
    </pre>
  </div>
  <div class="footer">Sincronizado dinamicamente com o buffer ativo do editor THZ</div>
  <script>
    mermaid.initialize({ startOnLoad: true, theme: 'dark', securityLevel: 'loose' });
  </script>
</body>
</html>`;
  }

  context.subscriptions.push(
    vscode.commands.registerCommand('thz.previewArchitecture', () => {
      const ed = vscode.window.activeTextEditor;
      if (!ed) {
        vscode.window.showInformationMessage('Abra um arquivo .thz para visualizar a arquitetura.');
        return;
      }

      if (architecturePanel) {
        architecturePanel.reveal(vscode.ViewColumn.Beside);
      } else {
        architecturePanel = vscode.window.createWebviewPanel(
          'thzArchitecture',
          'THZ Arquitetura Viva',
          vscode.ViewColumn.Beside,
          { enableScripts: true, retainContextWhenHidden: true }
        );
        architecturePanel.onDidDispose(() => {
          architecturePanel = undefined;
        });
      }

      architecturePanel.webview.html = renderizarHtmlArquitetura(ed.document.getText());
    })
  );

  // Atualização com debounce do Live Preview de Arquitetura
  let timerAtualizacaoPreview: NodeJS.Timeout | undefined;
  vscode.workspace.onDidChangeTextDocument(e => {
    if (architecturePanel && e.document === vscode.window.activeTextEditor?.document && e.document.languageId === 'thz') {
      if (timerAtualizacaoPreview) clearTimeout(timerAtualizacaoPreview);
      timerAtualizacaoPreview = setTimeout(() => {
        if (architecturePanel) {
          architecturePanel.webview.html = renderizarHtmlArquitetura(e.document.getText());
        }
      }, 400);
    }
  });

  // ==========================================================================
  // 4. LIVE PREVIEW DE TELAS DECLARATIVAS (.thzui / PROGRAMA VISUAL)
  // ==========================================================================

  let uiPanel: vscode.WebviewPanel | undefined;

  function renderizarHtmlTelaDeclarativa(fonte: string): string {
    let titulo = 'Interface Declarativa';
    const mNome = fonte.match(/(?:PROGRAMA VISUAL|TELA|PROGRAMA)\s+([A-Za-z0-9_]+)/);
    if (mNome) titulo = mNome[1];

    const botoes: string[] = [];
    const campos: { nome: string; tipo: string }[] = [];

    const linhas = fonte.split(/\r?\n/);
    for (const l of linhas) {
      const mBtn = l.match(/^\s*(?:BOTAO|botao|btn)\s*\(\s*"([^"]+)"/i) || l.match(/PROCEDIMENTO\s+([A-Za-z0-9_]+)/);
      if (mBtn && !botoes.includes(mBtn[1])) botoes.push(mBtn[1]);

      const mCampo = l.match(/^\s*([A-Za-z0-9_]+)\s*:\s*([A-Za-z0-9_()]+)/);
      if (mCampo && !l.includes('DOMINIO') && !l.includes('CAMADA') && !l.includes('SLO') && !l.includes('VERSAO')) {
        campos.push({ nome: mCampo[1], tipo: mCampo[2] });
      }
    }

    return `<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>THZ UI Live Preview</title>
  <style>
    :root {
      --bg: #090d16;
      --card: rgba(22, 31, 49, 0.85);
      --border: rgba(255, 255, 255, 0.12);
      --primary: #3b82f6;
      --text: #f8fafc;
      --muted: #94a3b8;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Segoe UI', Inter, sans-serif; }
    body { background: var(--bg); color: var(--text); padding: 24px; min-height: 100vh; display: flex; flex-direction: column; align-items: center; }
    .window-card { width: 100%; max-width: 600px; background: var(--card); border: 1px solid var(--border); border-radius: 12px; backdrop-filter: blur(16px); box-shadow: 0 20px 40px rgba(0,0,0,0.5); overflow: hidden; }
    .window-header { padding: 14px 20px; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; background: rgba(15, 23, 42, 0.6); }
    .window-header h2 { font-size: 1rem; font-weight: 600; color: var(--text); }
    .badge { font-size: 0.7rem; padding: 2px 8px; border-radius: 99px; background: rgba(59, 130, 246, 0.2); color: #60a5fa; border: 1px solid rgba(59, 130, 246, 0.3); }
    .window-body { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
    .form-group { display: flex; flex-direction: column; gap: 6px; }
    .form-group label { font-size: 0.85rem; color: var(--muted); font-weight: 500; }
    .form-input { padding: 10px 14px; background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border); border-radius: 8px; color: var(--text); font-size: 0.9rem; outline: none; }
    .form-input:focus { border-color: var(--primary); }
    .btn-group { display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
    .btn { padding: 10px 18px; border-radius: 8px; font-weight: 600; font-size: 0.9rem; border: none; cursor: pointer; transition: all 0.2s ease; }
    .btn-primary { background: var(--primary); color: white; }
    .btn-primary:hover { background: #2563eb; }
    .btn-secondary { background: rgba(255, 255, 255, 0.08); color: var(--text); border: 1px solid var(--border); }
    .output-log { margin-top: 20px; width: 100%; max-width: 600px; padding: 12px; background: rgba(0,0,0,0.4); border: 1px solid var(--border); border-radius: 8px; font-family: monospace; font-size: 0.8rem; color: var(--muted); }
  </style>
</head>
<body>
  <div class="window-card">
    <div class="window-header">
      <h2>🖥️ ${titulo}</h2>
      <span class="badge">Live Preview Glassmorphism</span>
    </div>
    <div class="window-body">
      ${campos.length > 0 ? campos.map(c => `
        <div class="form-group">
          <label>${c.nome} <span style="font-size:0.75rem; color:var(--muted)">(${c.tipo})</span></label>
          <input class="form-input" placeholder="Informe ${c.nome}..." />
        </div>
      `).join('') : '<p style="color:var(--muted); font-size:0.9rem">Nenhum campo de entrada declarado.</p>'}
      
      <div class="btn-group">
        ${botoes.length > 0 ? botoes.map((b, idx) => `
          <button class="btn ${idx === 0 ? 'btn-primary' : 'btn-secondary'}" onclick="registrarClique('${b}')">▶ ${b}</button>
        `).join('') : '<button class="btn btn-primary">▶ Executar Ação</button>'}
      </div>
    </div>
  </div>
  <div class="output-log" id="log">Console Interativo: Pronto.</div>
  <script>
    function registrarClique(nome) {
      document.getElementById('log').textContent = 'Evento disparado: ' + nome + ' em ' + new Date().toLocaleTimeString();
    }
  </script>
</body>
</html>`;
  }

  context.subscriptions.push(
    vscode.commands.registerCommand('thz.previewUi', () => {
      const ed = vscode.window.activeTextEditor;
      if (!ed) {
        vscode.window.showInformationMessage('Abra um arquivo .thz ou .thzui para visualizar a tela declarativa.');
        return;
      }

      if (uiPanel) {
        uiPanel.reveal(vscode.ViewColumn.Beside);
      } else {
        uiPanel = vscode.window.createWebviewPanel(
          'thzUiPreview',
          'THZ UI Preview',
          vscode.ViewColumn.Beside,
          { enableScripts: true, retainContextWhenHidden: true }
        );
        uiPanel.onDidDispose(() => {
          uiPanel = undefined;
        });
      }

      uiPanel.webview.html = renderizarHtmlTelaDeclarativa(ed.document.getText());
    })
  );

  vscode.workspace.onDidChangeTextDocument(e => {
    if (uiPanel && e.document === vscode.window.activeTextEditor?.document) {
      uiPanel.webview.html = renderizarHtmlTelaDeclarativa(e.document.getText());
    }
  });

  // ==========================================================================
  // 5. SIDEBAR DE GOVERNANÇA & ARQUITETURA (ACTIVITY BAR)
  // ==========================================================================

  class ThzTreeItem extends vscode.TreeItem {
    constructor(
      public readonly label: string,
      public readonly collapsibleState: vscode.TreeItemCollapsibleState,
      public readonly description?: string,
      public readonly children?: ThzTreeItem[]
    ) {
      super(label, collapsibleState);
      this.description = description;
    }
  }

  class ThzGovernanceTreeDataProvider implements vscode.TreeDataProvider<ThzTreeItem> {
    private _onDidChangeTreeData: vscode.EventEmitter<ThzTreeItem | undefined | null | void> = new vscode.EventEmitter<ThzTreeItem | undefined | null | void>();
    readonly onDidChangeTreeData: vscode.Event<ThzTreeItem | undefined | null | void> = this._onDidChangeTreeData.event;

    refresh(): void {
      this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: ThzTreeItem): vscode.TreeItem {
      return element;
    }

    getChildren(element?: ThzTreeItem): Thenable<ThzTreeItem[]> {
      if (element) {
        return Promise.resolve(element.children || []);
      }

      const ed = vscode.window.activeTextEditor;
      if (!ed || ed.document.languageId !== 'thz') {
        return Promise.resolve([new ThzTreeItem('Abra um arquivo .thz para ver governança', vscode.TreeItemCollapsibleState.None)]);
      }

      const fonte = ed.document.getText();
      const items: ThzTreeItem[] = [];

      // 1. Metadados
      const metaChildren: ThzTreeItem[] = [];
      const mDom = fonte.match(/DOMINIO\s*:\s*"([^"]+)"/);
      if (mDom) metaChildren.push(new ThzTreeItem('Domínio', vscode.TreeItemCollapsibleState.None, mDom[1]));
      const mSlo = fonte.match(/SLO_LATENCIA_MAXIMA\s*:\s*"([^"]+)"/);
      if (mSlo) metaChildren.push(new ThzTreeItem('SLO de Latência', vscode.TreeItemCollapsibleState.None, mSlo[1]));
      const mCrit = fonte.match(/CRITICIDADE\s*:\s*"([^"]+)"/);
      if (mCrit) metaChildren.push(new ThzTreeItem('Criticidade', vscode.TreeItemCollapsibleState.None, mCrit[1]));

      items.push(new ThzTreeItem('🏛️ Metadados de Arquitetura', vscode.TreeItemCollapsibleState.Expanded, undefined, metaChildren));

      // 2. Regras e Contratos
      const regrasChildren: ThzTreeItem[] = [];
      const linhas = fonte.split(/\r?\n/);
      let regraAtual: string | null = null;
      let reqAtual: string | null = null;
      let contratos: string[] = [];

      for (const l of linhas) {
        const mR = l.match(/^\s*REGRA_NEGOCIO\s+([A-Za-z0-9_]+)/);
        if (mR) {
          regraAtual = mR[1];
          reqAtual = null;
          contratos = [];
        }
        const mReq = l.match(/RASTREIO_REQUISITO\s*:\s*"([^"]+)"/);
        if (mReq) reqAtual = mReq[1];
        const mExige = l.match(/^\s*EXIGE\s+(.+)/);
        if (mExige) contratos.push(`EXIGE: ${mExige[1]}`);
        const mGarante = l.match(/^\s*GARANTE\s+(.+)/);
        if (mGarante) contratos.push(`GARANTE: ${mGarante[1]}`);

        if (l.match(/^\s*FIM_REGRA_NEGOCIO/) && regraAtual) {
          const cItems = contratos.map(c => new ThzTreeItem(`  🛡️ ${c}`, vscode.TreeItemCollapsibleState.None));
          regrasChildren.push(new ThzTreeItem(`⚖️ ${regraAtual}`, cItems.length > 0 ? vscode.TreeItemCollapsibleState.Expanded : vscode.TreeItemCollapsibleState.None, reqAtual ? `[${reqAtual}]` : undefined, cItems));
          regraAtual = null;
        }
      }

      if (regrasChildren.length > 0) {
        items.push(new ThzTreeItem('📋 Regras & Contratos DDD', vscode.TreeItemCollapsibleState.Expanded, `${regrasChildren.length} regra(s)`, regrasChildren));
      }

      // 3. Estruturas
      const estrChildren: ThzTreeItem[] = [];
      for (const l of linhas) {
        const mE = l.match(/^\s*ESTRUTURA\s+([A-Za-z0-9_]+)(?:\s+(LAYOUT_COLUNAR))?/);
        if (mE) {
          estrChildren.push(new ThzTreeItem(`📦 ${mE[1]}`, vscode.TreeItemCollapsibleState.None, mE[2] ? 'SIMD / SoA' : 'Padrão'));
        }
      }
      if (estrChildren.length > 0) {
        items.push(new ThzTreeItem('📦 Estruturas de Dados', vscode.TreeItemCollapsibleState.Collapsed, `${estrChildren.length} estrutura(s)`, estrChildren));
      }

      return Promise.resolve(items);
    }
  }

  const governanceProvider = new ThzGovernanceTreeDataProvider();
  vscode.window.registerTreeDataProvider('thz-governance-view', governanceProvider);
  vscode.window.onDidChangeActiveTextEditor(() => governanceProvider.refresh());
  vscode.workspace.onDidChangeTextDocument(e => {
    if (e.document === vscode.window.activeTextEditor?.document) {
      governanceProvider.refresh();
    }
  });

  // ==========================================================================
  // 6. ENDPOINTS CUSTOMIZADOS LSP (Auditoria / IR / LLVM)
  // ==========================================================================

  context.subscriptions.push(
    vscode.commands.registerCommand('thz.showAudit', async () => {
      const ed = vscode.window.activeTextEditor;
      if (!ed) {
        vscode.window.showInformationMessage('Abra um arquivo .thz para auditar.');
        return;
      }
      try {
        const result: any = await client!.sendRequest('thz/audit', { uri: ed.document.uri.toString() });
        if (result?.markdown) {
          const doc = await vscode.workspace.openTextDocument({ language: 'markdown', content: result.markdown });
          await vscode.window.showTextDocument(doc, { preview: true });
        } else if (result?.error) {
          vscode.window.showErrorMessage(result.error);
        }
      } catch (e) {
        vscode.window.showErrorMessage('Falha na auditoria: ' + (e as Error).message);
      }
    }),
    vscode.commands.registerCommand('thz.showIr', async () => {
      const ed = vscode.window.activeTextEditor;
      if (!ed) return;
      try {
        const res: any = await client!.sendRequest('thz/ir', { uri: ed.document.uri.toString() });
        if (res?.text) {
          const doc = await vscode.workspace.openTextDocument({ language: 'json', content: res.text });
          await vscode.window.showTextDocument(doc, { preview: true });
        } else if (res?.error) {
          vscode.window.showErrorMessage(res.error);
        }
      } catch (e) {
        vscode.window.showErrorMessage('Falha ao gerar IR: ' + (e as Error).message);
      }
    }),
    vscode.commands.registerCommand('thz.showLlvm', async () => {
      const ed = vscode.window.activeTextEditor;
      if (!ed) return;
      try {
        const res: any = await client!.sendRequest('thz/llvm', { uri: ed.document.uri.toString() });
        if (res?.text) {
          const doc = await vscode.workspace.openTextDocument({ language: 'llvm', content: res.text });
          await vscode.window.showTextDocument(doc, { preview: true });
        } else if (res?.error) {
          vscode.window.showErrorMessage(res.error);
        }
      } catch (e) {
        vscode.window.showErrorMessage('Falha ao gerar LLVM IR: ' + (e as Error).message);
      }
    })
  );
}

export function deactivate(): Thenable<void> | undefined {
  return client?.stop();
}
