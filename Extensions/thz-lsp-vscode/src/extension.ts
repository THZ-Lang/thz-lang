import * as fs from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';
import {
  LanguageClient,
  LanguageClientOptions,
  ServerOptions,
  Executable,
} from 'vscode-languageclient/node';

let client: LanguageClient | undefined;

export function activate(context: vscode.ExtensionContext): void {
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
      'THZ-LANG: Servidor LSP Java (thz-lsp-2.3.0.jar) não foi encontrado. Execute "npm run lsp:jar" para compilá-lo.'
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
        } else if (result?.error) {
          vscode.window.showErrorMessage(result.error);
        }
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
