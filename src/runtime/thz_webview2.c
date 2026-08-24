/*
 * THZ-LANG WebView2 Native Host — Fase 3
 * Janela Win32 dedicada com WebView2 (Edge Chromium) para UI autônoma sem --app.
 *
 * Compilação: gcc thz_runtime.c thz_webview2.c -o app.exe -lgdi32 -luser32 -lkernel32 -ldwmapi -lole32 -lshlwapi
 * Se WebView2Loader.dll não estiver presente, o host cai para thz_gui nativo (thz_runtime.c) ou msgbox.
 *
 * Nota: requer WebView2 SDK (WebView2Loader.dll) em dist/thz/ ou System32. Para builds
 * sem SDK, o stub compila mas retorna fallback ao runtime --app (LancadorWebviewNativo).
 */

#include <stdint.h>
#include <stddef.h>

#ifdef _WIN32

#define THZ_WEBVIEW_MAX_URL 2048

/* Win32 imports mínimos (evita windows.h para build determinístico) */
__declspec(dllimport) void* __stdcall GetModuleHandleA(const char* lpModuleName);
__declspec(dllimport) void* __stdcall LoadLibraryA(const char* lpLibFileName);
__declspec(dllimport) void* __stdcall GetProcAddress(void* hModule, const char* lpProcName);
__declspec(dllimport) int32_t __stdcall FreeLibrary(void* hLibModule);
__declspec(dllimport) int32_t __stdcall MessageBoxA(void* hWnd, const char* lpText, const char* lpCaption, uint32_t uType);

#define MB_OK 0x00000000L
#define MB_ICONINFORMATION 0x00000040L
#define S_OK 0

/* WebView2Loader CreateCoreWebView2EnvironmentWithOptions */
typedef int32_t (__stdcall *PFN_CreateCoreWebView2EnvironmentWithOptions)(
    const char* browserExecutableFolder,
    const char* userDataFolder,
    void* environmentOptions,
    void* createdEnvironmentCompletedHandler);

/* =========================================================================
 * API pública chamada pelo LLVM IR (thz_gui_loop_mensagens pode delegar)
 * ========================================================================= */

__declspec(dllexport) int32_t thz_webview_is_available(void) {
    void* h = LoadLibraryA("WebView2Loader.dll");
    if (!h) {
        /* tenta em dist/thz/WebView2Loader.dll via PATH relativo — já coberto por LoadLibrary search */
        return 0;
    }
    void* p = GetProcAddress(h, "CreateCoreWebView2EnvironmentWithOptions");
    FreeLibrary(h);
    return p != NULL ? 1 : 0;
}

__declspec(dllexport) int32_t thz_webview_navigate(const char* url, const char* titulo, int32_t largura, int32_t altura) {
    if (!url) return -1;
    (void)titulo; (void)largura; (void)altura;

    if (!thz_webview_is_available()) {
        /* fallback: informa usuário que WebView2 não está bundled, mas o launcher Java --app resolverá */
        return 0;
    }

    /* Fase 3 completa criará janela Win32 + WebView2 controller aqui:
     *   1. CreateWindowExA (classe THZ_WebView2)
     *   2. OleInitialize + CreateCoreWebView2EnvironmentWithOptions(userDataFolder=%TEMP%\\thz_webview_profile)
     *   3. ICoreWebView2Environment::CreateCoreWebView2Controller(hwnd, handler)
     *   4. ICoreWebView2Controller::get_CoreWebView2 + Navigate(url)
     *   5. Message loop + registro de thz_* objetos via AddHostObjectToScript
     *
     * Stub Fase 3.0: apenas valida URL e retorna 0 para deixar LancadorWebviewNativo --app assumir.
     * O link JNA (ThzWebView2ComHost) detectará disponibilidade e migrará para COM nativo em 3.1.
     */
    return 1;
}

__declspec(dllexport) const char* thz_webview_loader_status(void) {
    int32_t avail = thz_webview_is_available();
    return avail ? "WebView2Loader.dll encontrado — host COM Fase 3 pronto (stub ativo, fallback --app)" 
                 : "WebView2Loader.dll não encontrado — usando Edge --app / rundll32 (instale WebView2 Runtime)";
}

#else

/* POSIX stub — WebView2 é Windows-only; no Linux/macOS usa xdg-open/open */

int32_t thz_webview_is_available(void) { return 0; }
int32_t thz_webview_navigate(const char* url, const char* titulo, int32_t largura, int32_t altura) {
    (void)url; (void)titulo; (void)largura; (void)altura;
    return 0;
}
const char* thz_webview_loader_status(void) { return "WebView2 disponível apenas no Windows"; }

#endif
