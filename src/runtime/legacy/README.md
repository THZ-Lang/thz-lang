# Legacy — Win32 GUI (thz_runtime.c)

**Arquivado em 2026-08-24 — Fase 3**

O runtime Win32 completo (`CreateWindowEx` / `EDIT` / `BUTTON` / `DwmSetWindowAttribute`) que gerava `dist/bin/*_gui.exe` foi **arquivado aqui** por gerar janelas truncadas/feias (ver screenshot `ShowcaseWidgetsGui`).

- **Arquivo original:** `src/runtime/thz_runtime.c` (655 linhas) → copiado para `thz_runtime_gui_win32_legacy.c`
- **Motivo:** layout fixo `fieldW=440`, footer `80px` cortado, `wsprintfA` com `—` (UTF-8 `E2 80 94` → `â€` em `CP1252`), `WS_OVERLAPPEDWINDOW` sem `AdjustWindowRect`.
- **Substituto:** `src/runtime/thz_runtime.c` minimal (arena + console + stubs `thz_gui_*` → `MessageBoxA` simples) + `src/runtime/thz_webview2.c` (host WebView2 stub)
- **Fluxo recomendado:** `thz.exe` WebView (`JVM/thz-core-jvm/src/main/java/thz/lang/webview/LancadorWebviewNativo.java`) + `jpackage` (`JVM/thz-cli-jvm/scripts/build-package.ps1` → `dist/thz/thz.exe`)
  - `thz gui` → IDE WebView (padrão)
  - `thz run <arquivo_gui.thz>` → `TELA.*` via Edge/WebView2 `--app`
- **Build legado:** `scripts/build-llvm.ps1 -ForceLegado` ainda compila usando este arquivo se necessário para debug.

Não editar este diretório — mantido apenas para histórico/referência. Para reativar, copie de volta para `src/runtime/thz_runtime.c`.
