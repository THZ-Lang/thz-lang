import * as fs from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';
import {
  LanguageClient,
  LanguageClientOptions,
  ServerOptions,
  TransportKind,
} from 'vscode-languageclient/node';

let client: LanguageClient | undefined;

export function activate(context: vscode.ExtensionContext): void {
  const candidatos = [
    context.asAbsolutePath(path.join('server', 'lsp', 'server.js')),
    context.asAbsolutePath(path.join('server', 'server.js')),
    context.asAbsolutePath(path.join('dist', 'lsp', 'server.js')),
    context.asAbsolutePath(path.join('dist', 'server', 'server.js')),
    context.asAbsolutePath(path.join('..', 'dist', 'lsp', 'server.js')),
    context.asAbsolutePath(path.join('..', 'dist', 'src', 'lsp', 'server.js')),
  ];

  let serverModule = candidatos[0];
  for (const cand of candidatos) {
    if (fs.existsSync(cand)) {
      serverModule = cand;
      break;
    }
  }

  const serverOptions: ServerOptions = {
    run: { module: serverModule, transport: TransportKind.ipc },
    debug: {
      module: serverModule,
      transport: TransportKind.ipc,
      options: { execArgv: ['--nolazy', '--inspect=6009'] },
    },
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

  // G4 — comando de auditoria (thz/audit)
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
        } else if (result?.error) vscode.window.showErrorMessage(result.error);
      } catch (e) {
        vscode.window.showErrorMessage('Falha na auditoria: ' + (e as Error).message);
      }
    })
  );
  // G5 — IR / LLVM preview
  context.subscriptions.push(
    vscode.commands.registerCommand('thz.showIr', async () => {
      const ed = vscode.window.activeTextEditor;
      if (!ed) return;
      const res: any = await client!.sendRequest('thz/ir', { uri: ed.document.uri.toString() });
      if (res?.text) {
        const doc = await vscode.workspace.openTextDocument({ language: 'json', content: res.text });
        await vscode.window.showTextDocument(doc, { preview: true });
      } else if (res?.error) vscode.window.showErrorMessage(res.error);
    }),
    vscode.commands.registerCommand('thz.showLlvm', async () => {
      const ed = vscode.window.activeTextEditor;
      if (!ed) return;
      const res: any = await client!.sendRequest('thz/llvm', { uri: ed.document.uri.toString() });
      if (res?.text) {
        const doc = await vscode.workspace.openTextDocument({ language: 'llvm', content: res.text });
        await vscode.window.showTextDocument(doc, { preview: true });
      } else if (res?.error) vscode.window.showErrorMessage(res.error);
    })
  );
}

export function deactivate(): Thenable<void> | undefined {
  return client?.stop();
}
