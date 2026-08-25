# ADR-005 — Swing+FlatLaf vs WebView2 para TELA

**Data:** 2025-08-25 · **Status:** Aceito · **Contexto:** `TELA`/`thz-gui-jvm` vs `thz_webview2.c` + `LancadorWebviewNativo.java`.

## Contexto

`thz_runtime.c:144` Win32 `thz_gui_*` gerava janela feia/truncada — `scripts/build-llvm.ps1:1` DEPRECIADO Fase 3. Alternativas: Swing (FlatLaf Glassmorphism, `ThzGui.java`, `RenderizadorFormularioSwing.java`), WebView2 (Edge Chromium, `thz_webview2.c:42` `LoadLibraryA WebView2Loader.dll`), Electron, JavaFX.

## Decisão

**Dois renderizadores, mesmo fonte (`TELA.*`):**
- **Swing+FlatLaf** para `thz gui` IDE (`JVM/thz-gui-jvm`, `ThzGui.java`, `EditorThz.java`, `Gutter.java`) — `jpackage` padrão.
- **WebView/HTML5** para `thz run`/`thz ui --html` (`LancadorWebviewNativo.java:65`, `ThzUiHtmlEmitter`, `thz_webview2.c:52` `thz_webview_navigate`) — `thz_webview2.c` linkado via `build-llvm.ps1:79`, fallback `Edge --app` com `userDataDir=%TEMP%\thz_webview_profile` (`LancadorWebviewNativo.java:74`).

## Consequências

- **Prós:** `TELA.renderizarFormulario` (`TELA_THZUI.md:5`) funciona nos dois; HTML é `scratch` Docker-friendly; Swing é desktop nativo sem Edge.
- **Contras:** `thz_runtime.c` GUI legada mantida como stub (`MessageBoxA`) para não quebrar `declare @thz_gui_*`; `thz_webview2.c:52` Fase 3 ainda stub (retorna 1 sem `CreateWindowExA`).
- **Regra:** `build-llvm.ps1:38` bloqueia `*_gui.thz` sem `-ForceLegado` — `TELA` sempre via WebView/jpackage.

