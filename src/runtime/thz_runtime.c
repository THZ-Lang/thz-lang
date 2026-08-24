/*
 * THZ-LANG Native Runtime System
 * TODO: Validar efetividade do codigo, pois ele ainda nao foi testado
 */

#include <stdint.h>
#include <stddef.h>

#ifdef _WIN32

#define STD_OUTPUT_HANDLE     ((uint32_t)-11)
#define MB_OK                 0x00000000L
#define MB_ICONINFORMATION    0x00000040L

#define GENERIC_READ          0x80000000L
#define GENERIC_WRITE         0x40000000L
#define FILE_SHARE_READ       0x00000001L
#define OPEN_EXISTING         3L
#define CREATE_ALWAYS         2L
#define FILE_ATTRIBUTE_NORMAL 0x00000080L
#define INVALID_HANDLE_VALUE  ((void*)(intptr_t)-1)

__declspec(dllimport) void*    __stdcall GetStdHandle(uint32_t nStdHandle);
__declspec(dllimport) int32_t  __stdcall WriteFile(void* hFile, const void* lpBuffer, uint32_t nNumberOfBytesToWrite, uint32_t* lpNumberOfBytesWritten, void* lpOverlapped);
__declspec(dllimport) void*    __stdcall CreateFileA(const char* lpFileName, uint32_t dwDesiredAccess, uint32_t dwShareMode, void* lpSecurityAttributes, uint32_t dwCreationDisposition, uint32_t dwFlagsAndAttributes, void* hTemplateFile);
__declspec(dllimport) uint32_t __stdcall GetFileSize(void* hFile, uint32_t* lpFileSizeHigh);
__declspec(dllimport) int32_t  __stdcall ReadFile(void* hFile, void* lpBuffer, uint32_t nNumberOfBytesToRead, uint32_t* lpNumberOfBytesRead, void* lpOverlapped);
__declspec(dllimport) int32_t  __stdcall CloseHandle(void* hObject);
__declspec(dllimport) void*    __stdcall GetProcessHeap(void);
__declspec(dllimport) void*    __stdcall HeapAlloc(void* hHeap, uint32_t dwFlags, size_t dwBytes);
__declspec(dllimport) int32_t  __stdcall HeapFree(void* hHeap, uint32_t dwFlags, void* lpMem);
__declspec(dllimport) int32_t  __stdcall MessageBoxA(void* hWnd, const char* lpText, const char* lpCaption, uint32_t uType);
__declspec(dllimport) int32_t  __cdecl   wsprintfA(char* lpOut, const char* lpFmt, ...);
__declspec(dllimport) uint32_t __stdcall WinExec(const char* lpCmdLine, uint32_t uCmdShow);

/* Arena */
typedef struct { uint8_t* buffer; size_t capacidade; size_t offset; } ThzArena;

__declspec(dllexport) void* thz_arena_alloc(uint64_t bytes) {
    ThzArena* arena = (ThzArena*) HeapAlloc(GetProcessHeap(), 0, sizeof(ThzArena));
    if (!arena) return (void*)0;
    arena->capacidade = (size_t) bytes;
    arena->buffer = (uint8_t*) HeapAlloc(GetProcessHeap(), 0, arena->capacidade);
    arena->offset = 0;
    return (void*) arena;
}
__declspec(dllexport) void thz_arena_free_all(void* arena_ptr) {
    if (!arena_ptr) return;
    ThzArena* arena = (ThzArena*) arena_ptr;
    if (arena->buffer) HeapFree(GetProcessHeap(), 0, arena->buffer);
    HeapFree(GetProcessHeap(), 0, arena);
}

/* I/O de Arquivos e Strings Nativas */
__declspec(dllexport) int32_t thz_tamanho_str(const char* s) {
    if (!s) return 0;
    int32_t len = 0;
    while (s[len] != '\0') len++;
    return len;
}

__declspec(dllexport) int32_t thz_char_at(const char* s, int32_t idx) {
    if (!s || idx < 0) return 0;
    int32_t i = 0;
    while (s[i] != '\0') {
        if (i == idx) return (int32_t)(uint8_t)s[i];
        i++;
    }
    return 0;
}

__declspec(dllexport) const char* thz_substring(const char* s, int32_t inicio, int32_t len) {
    if (!s || inicio < 0 || len <= 0) return "";
    int32_t strLen = thz_tamanho_str(s);
    if (inicio >= strLen) return "";
    if (inicio + len > strLen) len = strLen - inicio;

    char* sub = (char*) HeapAlloc(GetProcessHeap(), 0, len + 1);
    if (!sub) return "";
    for (int32_t i = 0; i < len; i++) {
        sub[i] = s[inicio + i];
    }
    sub[len] = '\0';
    return sub;
}

__declspec(dllexport) const char* thz_ler_arquivo(const char* caminho) {
    if (!caminho) return "";
    void* hFile = CreateFileA(caminho, GENERIC_READ, FILE_SHARE_READ, (void*)0, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, (void*)0);
    if (hFile == INVALID_HANDLE_VALUE) return "";

    uint32_t size = GetFileSize(hFile, (uint32_t*)0);
    if (size == 0 || size == 0xFFFFFFFF) {
        CloseHandle(hFile);
        return "";
    }

    char* buffer = (char*) HeapAlloc(GetProcessHeap(), 0, size + 1);
    if (!buffer) {
        CloseHandle(hFile);
        return "";
    }

    uint32_t readBytes = 0;
    ReadFile(hFile, buffer, size, &readBytes, (void*)0);
    buffer[readBytes] = '\0';
    CloseHandle(hFile);
    return buffer;
}

__declspec(dllexport) int32_t thz_escrever_arquivo(const char* caminho, const char* conteudo) {
    if (!caminho || !conteudo) return 0;
    void* hFile = CreateFileA(caminho, GENERIC_WRITE, 0, (void*)0, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, (void*)0);
    if (hFile == INVALID_HANDLE_VALUE) return 0;

    uint32_t len = (uint32_t) thz_tamanho_str(conteudo);
    uint32_t written = 0;
    int32_t ok = WriteFile(hFile, conteudo, len, &written, (void*)0);
    CloseHandle(hFile);
    return (ok && written == len) ? 1 : 0;
}

__declspec(dllexport) int32_t thz_executar_comando(const char* cmd) {
    if (!cmd) return -1;
    return (int32_t) WinExec(cmd, 1); // SW_SHOWNORMAL
}

/* Console */
__declspec(dllexport) void thz_exiba_str(const char* msg) {
    if (!msg) return;
    void* hConsole = GetStdHandle(STD_OUTPUT_HANDLE);
    uint32_t len = 0; while (msg[len] != '\0') len++;
    uint32_t written = 0;
    WriteFile(hConsole, msg, len, &written, (void*)0);
    WriteFile(hConsole, "\r\n", 2, &written, (void*)0);
}
__declspec(dllexport) void thz_exiba_i128(uint64_t low, uint64_t high, int32_t scale) {
    (void)high; (void)scale;
    char buf[64];
    wsprintfA(buf, "[DECIMAL FIXO NATIVO] %I64u", low);
    thz_exiba_str(buf);
}

/* GUI STUBS — legado arquivado, nao cria janela Win32 truncada */
#define THZ_GUI_MAX_FORMS 8

static const char* g_titulos[THZ_GUI_MAX_FORMS];
static const char* g_estruturas[THZ_GUI_MAX_FORMS];
static const char* g_operacoes[THZ_GUI_MAX_FORMS];
static int32_t g_formCount = 0;

__declspec(dllexport) int32_t thz_gui_iniciar(const char* titulo, const char* nomeEstrutura) {
    if (g_formCount >= THZ_GUI_MAX_FORMS) return -1;
    int32_t idx = g_formCount++;
    g_titulos[idx] = titulo ? titulo : "THZ-LANG";
    g_estruturas[idx] = nomeEstrutura ? nomeEstrutura : "Formulario";
    g_operacoes[idx] = "Salvar";
    thz_exiba_str("[THZ GUI] Win32 legado descontinuado — use thz.exe WebView (thz run / thz gui)");
    return idx;
}
__declspec(dllexport) void thz_gui_adicionar_campo(int32_t formIdx, const char* rotulo, const char* valorPadrao, const char* tipo) {
    (void)formIdx; (void)tipo;
    char buf[512];
    wsprintfA(buf, "  [CAMPO] %s: %s", rotulo ? rotulo : "?", valorPadrao ? valorPadrao : "");
    thz_exiba_str(buf);
}
__declspec(dllexport) void thz_gui_set_operacao(int32_t formIdx, const char* operacao) {
    if (formIdx < 0 || formIdx >= g_formCount) return;
    g_operacoes[formIdx] = operacao ? operacao : "Salvar";
}
__declspec(dllexport) void thz_gui_exibir(int32_t formIdx) {
    if (formIdx < 0 || formIdx >= g_formCount) return;
    char msg[1024];
    wsprintfA(msg, "[THZ GUI LEGADO] Formulario '%s' (%s) -> Operacao: %s\nUse: thz run <arquivo> ou thz gui (WebView) para UI sem truncamento.",
        g_titulos[formIdx], g_estruturas[formIdx], g_operacoes[formIdx]);
    thz_exiba_str(msg);
    MessageBoxA((void*)0, msg, g_titulos[formIdx], MB_OK | MB_ICONINFORMATION);
}
__declspec(dllexport) void thz_gui_loop_mensagens(void) {
    thz_exiba_str("[THZ] Pressione Enter para sair (stub)...");
    // nao bloqueia message loop Win32 legado
}
__declspec(dllexport) void thz_renderizar_tela(const char* titulo, const char* conteudo) {
    if (!titulo) titulo = "THZ-LANG";
    if (!conteudo) conteudo = "Conteudo";
    thz_exiba_str(conteudo);
    MessageBoxA((void*)0, conteudo, titulo, MB_OK | MB_ICONINFORMATION);
}

#else /* POSIX — mantem console + stubs */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
typedef struct { uint8_t* buffer; size_t capacidade; size_t offset; } ThzArena;
void* thz_arena_alloc(uint64_t bytes) { ThzArena* a=(ThzArena*)malloc(sizeof(ThzArena)); if(!a) return 0; a->capacidade=(size_t)bytes; a->buffer=(uint8_t*)malloc(a->capacidade); a->offset=0; return a; }
void thz_arena_free_all(void* p) { if(!p) return; ThzArena* a=(ThzArena*)p; if(a->buffer) free(a->buffer); free(a); }
int32_t thz_tamanho_str(const char* s) { return s ? (int32_t)strlen(s) : 0; }
int32_t thz_char_at(const char* s, int32_t idx) { if(!s || idx < 0 || (size_t)idx >= strlen(s)) return 0; return (int32_t)(uint8_t)s[idx]; }
const char* thz_substring(const char* s, int32_t inicio, int32_t len) {
    if(!s || inicio < 0 || len <= 0) return "";
    int32_t sl = (int32_t)strlen(s);
    if(inicio >= sl) return "";
    if(inicio + len > sl) len = sl - inicio;
    char* sub = (char*)malloc(len + 1);
    if(!sub) return "";
    memcpy(sub, s + inicio, len);
    sub[len] = '\0';
    return sub;
}
const char* thz_ler_arquivo(const char* caminho) {
    if(!caminho) return "";
    FILE* f = fopen(caminho, "rb");
    if(!f) return "";
    fseek(f, 0, SEEK_END);
    long sz = ftell(f);
    fseek(f, 0, SEEK_SET);
    if(sz <= 0) { fclose(f); return ""; }
    char* buf = (char*)malloc(sz + 1);
    if(!buf) { fclose(f); return ""; }
    size_t rd = fread(buf, 1, sz, f);
    buf[rd] = '\0';
    fclose(f);
    return buf;
}
int32_t thz_escrever_arquivo(const char* caminho, const char* conteudo) {
    if(!caminho || !conteudo) return 0;
    FILE* f = fopen(caminho, "wb");
    if(!f) return 0;
    size_t len = strlen(conteudo);
    size_t wr = fwrite(conteudo, 1, len, f);
    fclose(f);
    return (wr == len) ? 1 : 0;
}
int32_t thz_executar_comando(const char* cmd) { return cmd ? system(cmd) : -1; }
void thz_exiba_str(const char* msg) { if(msg) printf("%s\n", msg); }
void thz_exiba_i128(uint64_t low, uint64_t high, int32_t scale) { (void)high;(void)scale; printf("[DECIMAL] %lu\n",(unsigned long)low); }
static const char* g_t[8]; static const char* g_e[8]; static const char* g_o[8]; static int32_t g_c=0;
int32_t thz_gui_iniciar(const char* t,const char* e){ if(g_c>=8) return -1; int i=g_c++; g_t[i]=t?t:"THZ"; g_e[i]=e?e:"Form"; g_o[i]="Salvar"; printf("[THZ GUI legado] %s (%s)\n", t, e); return i; }
void thz_gui_adicionar_campo(int32_t f,const char* r,const char* v,const char* tp){ (void)f; printf("  [CAMPO] %s: %s (%s)\n", r?r:"?", v?v:"", tp?tp:"TEXTO"); }
void thz_gui_set_operacao(int32_t f,const char* o){ if(f>=0&&f<g_c) g_o[f]=o?o:"Salvar"; }
void thz_gui_exibir(int32_t f){ if(f<0||f>=g_c) return; printf("[GUI legado] %s -> %s — use thz gui (WebView)\n", g_t[f], g_o[f]); }
void thz_gui_loop_mensagens(void){ printf("[THZ] Enter para sair\n"); getchar(); }
void thz_renderizar_tela(const char* t,const char* c){ printf("[GUI] %s: %s\n", t?t:"THZ", c?c:""); }

#endif

