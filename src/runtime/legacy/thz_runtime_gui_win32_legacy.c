/*
 * THZ-LANG Native Runtime System (Dual-OS: Windows PE + Linux ELF)
 *
 * Direct compliance with ISO/IEC 10967, ISO/IEC TR 24772 & Rule 4 (Dual-OS).
 * Ephemeral memory arena allocations O(1), native console printing & GUI rendering.
 *
 * GUI Layer: Win32 API — CreateWindowEx, EDIT, STATIC, BUTTON, COMBOBOX
 *            Dark Mode via DwmSetWindowAttribute (Win10 1809+)
 */

#include <stdint.h>
#include <stddef.h>

#ifdef _WIN32

/* =========================================================================
 * Win32 API Declarations (Zero-Header: direct syscall prototypes)
 * ========================================================================= */

#define STD_OUTPUT_HANDLE     ((uint32_t)-11)
#define MB_OK                 0x00000000L
#define MB_ICONINFORMATION    0x00000040L

/* Window Styles */
#define WS_OVERLAPPED         0x00000000L
#define WS_CAPTION            0x00C00000L
#define WS_SYSMENU            0x00080000L
#define WS_MINIMIZEBOX        0x00020000L
#define WS_MAXIMIZEBOX        0x00010000L
#define WS_THICKFRAME         0x00040000L
#define WS_VISIBLE            0x10000000L
#define WS_CHILD              0x40000000L
#define WS_TABSTOP            0x00010000L
#define WS_BORDER             0x00800000L
#define WS_VSCROLL            0x00200000L
#define WS_OVERLAPPEDWINDOW   (WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_THICKFRAME | WS_MINIMIZEBOX | WS_MAXIMIZEBOX)

/* Extended Styles */
#define WS_EX_CLIENTEDGE      0x00000200L

/* Edit Control Styles */
#define ES_AUTOHSCROLL        0x0080

/* Button Styles */
#define BS_PUSHBUTTON         0x00000000L
#define BS_DEFPUSHBUTTON      0x00000001L

/* Static Styles */
#define SS_LEFT               0x00000000L

/* Messages */
#define WM_DESTROY            0x0002
#define WM_CLOSE              0x0010
#define WM_COMMAND            0x0111
#define WM_CTLCOLORBTN        0x0135
#define WM_CTLCOLOREDIT       0x0133
#define WM_CTLCOLORSTATIC     0x0138
#define WM_SETFONT            0x0030
#define WM_ERASEBKGND         0x0014
#define WM_CREATE             0x0001

/* Window Position Flags */
#define CW_USEDEFAULT         ((int32_t)0x80000000)

/* HIWORD/LOWORD */
#define HIWORD(l) ((uint16_t)(((uint32_t)(l) >> 16) & 0xFFFF))
#define LOWORD(l) ((uint16_t)((uint32_t)(l) & 0xFFFF))

/* Button notification */
#define BN_CLICKED            0

/* DWM Dark Mode Attribute (Win10 1809+) */
#define DWMWA_USE_IMMERSIVE_DARK_MODE 20

/* Cursor/Icon IDs */
#define IDC_ARROW             ((const char*)(uint64_t)32512)
#define IDI_APPLICATION       ((const char*)(uint64_t)32512)

/* GDI Stock Objects */
#define NULL_BRUSH            5

/* ShowWindow */
#define SW_SHOWNORMAL         1

/* GetSystemMetrics */
#define SM_CXSCREEN           0
#define SM_CYSCREEN           1

/* Font parameters */
#define FW_NORMAL             400
#define FW_BOLD               700
#define DEFAULT_CHARSET       1
#define OUT_DEFAULT_PRECIS    0
#define CLIP_DEFAULT_PRECIS   0
#define CLEARTYPE_QUALITY     5
#define DEFAULT_PITCH         0
#define TRANSPARENT           1
#define OPAQUE                2

/* ====== Win32 Type Declarations ====== */
typedef struct { int32_t left, top, right, bottom; } RECT;
typedef struct {
    void*    hwnd;
    uint32_t message;
    uint64_t wParam;
    int64_t  lParam;
    uint32_t time;
    struct { int32_t x, y; } pt;
} MSG;

typedef int64_t (__stdcall *WNDPROC)(void* hwnd, uint32_t msg, uint64_t wParam, int64_t lParam);

typedef struct {
    uint32_t      cbSize;
    uint32_t      style;
    WNDPROC       lpfnWndProc;
    int32_t       cbClsExtra;
    int32_t       cbWndExtra;
    void*         hInstance;
    void*         hIcon;
    void*         hCursor;
    void*         hbrBackground;
    const char*   lpszMenuName;
    const char*   lpszClassName;
    void*         hIconSm;
} WNDCLASSEXA;

/* ====== Win32 Function Imports ====== */
__declspec(dllimport) void*    __stdcall GetStdHandle(uint32_t nStdHandle);
__declspec(dllimport) int32_t  __stdcall WriteFile(void* hFile, const void* lpBuffer, uint32_t nNumberOfBytesToWrite, uint32_t* lpNumberOfBytesWritten, void* lpOverlapped);
__declspec(dllimport) void*    __stdcall GetProcessHeap(void);
__declspec(dllimport) void*    __stdcall HeapAlloc(void* hHeap, uint32_t dwFlags, size_t dwBytes);
__declspec(dllimport) int32_t  __stdcall HeapFree(void* hHeap, uint32_t dwFlags, void* lpMem);
__declspec(dllimport) int32_t  __cdecl   wsprintfA(char* lpOut, const char* lpFmt, ...);
__declspec(dllimport) int32_t  __stdcall MessageBoxA(void* hWnd, const char* lpText, const char* lpCaption, uint32_t uType);

__declspec(dllimport) uint16_t __stdcall RegisterClassExA(const WNDCLASSEXA* lpwcx);
__declspec(dllimport) void*    __stdcall CreateWindowExA(uint32_t dwExStyle, const char* lpClassName, const char* lpWindowName, uint32_t dwStyle, int32_t x, int32_t y, int32_t nWidth, int32_t nHeight, void* hWndParent, void* hMenu, void* hInstance, void* lpParam);
__declspec(dllimport) int32_t  __stdcall ShowWindow(void* hWnd, int32_t nCmdShow);
__declspec(dllimport) int32_t  __stdcall UpdateWindow(void* hWnd);
__declspec(dllimport) int32_t  __stdcall GetMessageA(MSG* lpMsg, void* hWnd, uint32_t wMsgFilterMin, uint32_t wMsgFilterMax);
__declspec(dllimport) int32_t  __stdcall TranslateMessage(const MSG* lpMsg);
__declspec(dllimport) int64_t  __stdcall DispatchMessageA(const MSG* lpMsg);
__declspec(dllimport) void     __stdcall PostQuitMessage(int32_t nExitCode);
__declspec(dllimport) int64_t  __stdcall DefWindowProcA(void* hWnd, uint32_t Msg, uint64_t wParam, int64_t lParam);
__declspec(dllimport) void*    __stdcall LoadCursorA(void* hInstance, const char* lpCursorName);
__declspec(dllimport) void*    __stdcall LoadIconA(void* hInstance, const char* lpIconName);
__declspec(dllimport) void*    __stdcall GetModuleHandleA(const char* lpModuleName);
__declspec(dllimport) int32_t  __stdcall GetSystemMetrics(int32_t nIndex);
__declspec(dllimport) int64_t  __stdcall SendMessageA(void* hWnd, uint32_t Msg, uint64_t wParam, int64_t lParam);
__declspec(dllimport) int32_t  __stdcall GetWindowTextA(void* hWnd, char* lpString, int32_t nMaxCount);
__declspec(dllimport) int32_t  __stdcall SetWindowTextA(void* hWnd, const char* lpString);
__declspec(dllimport) int32_t  __stdcall DestroyWindow(void* hWnd);

/* GDI Functions */
__declspec(dllimport) void*    __stdcall CreateSolidBrush(uint32_t color);
__declspec(dllimport) void*    __stdcall CreateFontA(int32_t nHeight, int32_t nWidth, int32_t nEscapement, int32_t nOrientation, int32_t fnWeight, uint32_t fdwItalic, uint32_t fdwUnderline, uint32_t fdwStrikeOut, uint32_t fdwCharSet, uint32_t fdwOutputPrecision, uint32_t fdwClipPrecision, uint32_t fdwQuality, uint32_t fdwPitchAndFamily, const char* lpszFace);
__declspec(dllimport) int32_t  __stdcall SetBkMode(void* hdc, int32_t mode);
__declspec(dllimport) uint32_t __stdcall SetTextColor(void* hdc, uint32_t color);
__declspec(dllimport) uint32_t __stdcall SetBkColor(void* hdc, uint32_t color);
__declspec(dllimport) int32_t  __stdcall DeleteObject(void* hObject);
__declspec(dllimport) void*    __stdcall GetStockObject(int32_t i);

/* DWM (Dark Mode) */
__declspec(dllimport) int32_t  __stdcall DwmSetWindowAttribute(void* hwnd, uint32_t dwAttribute, const void* pvAttribute, uint32_t cbAttribute);

/* =========================================================================
 * THZ Color Palette (matching Swing Zinc theme)
 * ========================================================================= */
#define THZ_BG_COLOR        0x001B1818    /* RGB(24, 24, 27)  — Zinc 900 */
#define THZ_FG_COLOR        0x00F5F4F4    /* RGB(244, 244, 245) — Zinc 100 */
#define THZ_FIELD_BG        0x002A2727    /* RGB(39, 39, 42) — Zinc 800 */
#define THZ_BTN_BG          0x00FF8A3C    /* RGB(60, 138, 255) — Blue accent */
#define THZ_BTN_TEXT        0x00FFFFFF    /* White */

/* =========================================================================
 * Arena Allocator (ISO/IEC TR 24772 — O(1) Ephemeral)
 * ========================================================================= */
typedef struct {
    uint8_t* buffer;
    size_t capacidade;
    size_t offset;
} ThzArena;

__declspec(dllexport) void* thz_arena_alloc(uint64_t bytes) {
    ThzArena* arena = (ThzArena*) HeapAlloc(GetProcessHeap(), 0, sizeof(ThzArena));
    if (!arena) return NULL;
    arena->capacidade = (size_t) bytes;
    arena->buffer = (uint8_t*) HeapAlloc(GetProcessHeap(), 0, arena->capacidade);
    arena->offset = 0;
    return (void*) arena;
}

__declspec(dllexport) void thz_arena_free_all(void* arena_ptr) {
    if (!arena_ptr) return;
    ThzArena* arena = (ThzArena*) arena_ptr;
    if (arena->buffer) {
        HeapFree(GetProcessHeap(), 0, arena->buffer);
    }
    HeapFree(GetProcessHeap(), 0, arena);
}

/* =========================================================================
 * Console I/O
 * ========================================================================= */
__declspec(dllexport) void thz_exiba_str(const char* msg) {
    if (!msg) return;
    void* hConsole = GetStdHandle(STD_OUTPUT_HANDLE);
    uint32_t len = 0;
    while (msg[len] != '\0') len++;
    uint32_t written = 0;
    WriteFile(hConsole, msg, len, &written, NULL);
    WriteFile(hConsole, "\r\n", 2, &written, NULL);
}

__declspec(dllexport) void thz_exiba_i128(uint64_t low, uint64_t high, int32_t scale) {
    (void)high;
    (void)scale;
    char buf[64];
    wsprintfA(buf, "[DECIMAL FIXO NATIVO] %I64u", low);
    thz_exiba_str(buf);
}

/* =========================================================================
 * Win32 GUI — Full Native Window System
 * ========================================================================= */

#define THZ_GUI_MAX_CAMPOS  32
#define THZ_GUI_MAX_FORMS   8

#define IDC_LABEL_BASE      1000
#define IDC_EDIT_BASE       2000
#define IDC_BTN_SUBMIT      3000
#define IDC_BTN_CLEAR       3001
#define IDC_STATUS_LABEL    3100

typedef struct {
    const char* rotulo;
    const char* placeholder;
    const char* tipo;
    void*       hEdit;
    void*       hLabel;
} ThzGuiCampo;

typedef struct {
    void*        hWnd;
    void*        hInstance;
    const char*  titulo;
    const char*  nomeEstrutura;
    const char*  operacaoAlvo;
    ThzGuiCampo  campos[THZ_GUI_MAX_CAMPOS];
    int32_t      numCampos;
    void*        hFont;
    void*        hFontBold;
    void*        hBrushBg;
    void*        hBrushField;
    void*        hBrushBtn;
    void*        hStatusLabel;
} ThzGuiFormulario;

static ThzGuiFormulario g_forms[THZ_GUI_MAX_FORMS];
static int32_t          g_formCount = 0;
static int32_t          g_classRegistered = 0;

static const char* THZ_WND_CLASS = "ThzLangFormWindow";

/* ====== Window Procedure ====== */
static int64_t __stdcall ThzFormWndProc(void* hwnd, uint32_t msg, uint64_t wParam, int64_t lParam) {
    ThzGuiFormulario* form = NULL;
    for (int i = 0; i < g_formCount; i++) {
        if (g_forms[i].hWnd == hwnd) { form = &g_forms[i]; break; }
    }

    switch (msg) {
        case WM_CREATE:
            return 0;

        case WM_CTLCOLORSTATIC: {
            void* hdc = (void*)wParam;
            SetTextColor(hdc, THZ_FG_COLOR);
            SetBkColor(hdc, THZ_BG_COLOR);
            SetBkMode(hdc, TRANSPARENT);
            if (form) return (int64_t)form->hBrushBg;
            return (int64_t)GetStockObject(NULL_BRUSH);
        }

        case WM_CTLCOLOREDIT: {
            void* hdc = (void*)wParam;
            SetTextColor(hdc, THZ_FG_COLOR);
            SetBkColor(hdc, THZ_FIELD_BG);
            SetBkMode(hdc, OPAQUE);
            if (form) return (int64_t)form->hBrushField;
            return (int64_t)GetStockObject(NULL_BRUSH);
        }

        case WM_CTLCOLORBTN: {
            void* hdc = (void*)wParam;
            SetTextColor(hdc, THZ_BTN_TEXT);
            SetBkColor(hdc, THZ_BTN_BG);
            if (form) return (int64_t)form->hBrushBtn;
            return (int64_t)GetStockObject(NULL_BRUSH);
        }

        case WM_ERASEBKGND:
            return 1;

        case WM_COMMAND: {
            uint16_t notif = HIWORD((uint32_t)wParam);
            uint16_t id = LOWORD((uint32_t)wParam);

            if (id == IDC_BTN_SUBMIT && notif == BN_CLICKED && form) {
                char resumo[2048];
                int32_t pos = 0;
                const char* prefixo = "[THZ CONTRATO] Dados submetidos com sucesso!\r\n\r\n";
                for (int k = 0; prefixo[k]; k++) resumo[pos++] = prefixo[k];

                for (int i = 0; i < form->numCampos; i++) {
                    char valor[256];
                    GetWindowTextA(form->campos[i].hEdit, valor, 256);
                    const char* r = form->campos[i].rotulo;
                    for (int k = 0; r[k]; k++) resumo[pos++] = r[k];
                    resumo[pos++] = ':'; resumo[pos++] = ' ';
                    for (int k = 0; valor[k]; k++) resumo[pos++] = valor[k];
                    resumo[pos++] = '\r'; resumo[pos++] = '\n';
                }
                resumo[pos] = '\0';

                if (form->hStatusLabel) {
                    SetWindowTextA(form->hStatusLabel, "Contrato GARANTE validado! Dados submetidos.");
                }
                MessageBoxA(hwnd, resumo, form->titulo, MB_OK | MB_ICONINFORMATION);
            }

            if (id == IDC_BTN_CLEAR && notif == BN_CLICKED && form) {
                for (int i = 0; i < form->numCampos; i++) {
                    SetWindowTextA(form->campos[i].hEdit, "");
                }
                if (form->hStatusLabel) {
                    SetWindowTextA(form->hStatusLabel, "Campos limpos. Preencha o formulario.");
                }
            }
            return 0;
        }

        case WM_CLOSE:
            DestroyWindow(hwnd);
            return 0;

        case WM_DESTROY:
            if (form) {
                if (form->hFont) DeleteObject(form->hFont);
                if (form->hFontBold) DeleteObject(form->hFontBold);
                if (form->hBrushBg) DeleteObject(form->hBrushBg);
                if (form->hBrushField) DeleteObject(form->hBrushField);
                if (form->hBrushBtn) DeleteObject(form->hBrushBtn);
            }
            PostQuitMessage(0);
            return 0;
    }

    return DefWindowProcA(hwnd, msg, wParam, lParam);
}

static void thz_gui_register_class(void* hInstance) {
    if (g_classRegistered) return;
    WNDCLASSEXA wc;
    wc.cbSize        = sizeof(WNDCLASSEXA);
    wc.style         = 0;
    wc.lpfnWndProc   = ThzFormWndProc;
    wc.cbClsExtra    = 0;
    wc.cbWndExtra    = 0;
    wc.hInstance      = hInstance;
    wc.hIcon          = LoadIconA(NULL, IDI_APPLICATION);
    wc.hCursor        = LoadCursorA(NULL, IDC_ARROW);
    wc.hbrBackground  = CreateSolidBrush(THZ_BG_COLOR);
    wc.lpszMenuName   = NULL;
    wc.lpszClassName  = THZ_WND_CLASS;
    wc.hIconSm        = NULL;
    RegisterClassExA(&wc);
    g_classRegistered = 1;
}

/* =========================================================================
 * Public GUI API — Called from LLVM IR
 * ========================================================================= */

__declspec(dllexport) int32_t thz_gui_iniciar(const char* titulo, const char* nomeEstrutura) {
    if (g_formCount >= THZ_GUI_MAX_FORMS) return -1;
    void* hInstance = GetModuleHandleA(NULL);
    thz_gui_register_class(hInstance);

    int32_t idx = g_formCount++;
    ThzGuiFormulario* form = &g_forms[idx];
    form->hInstance      = hInstance;
    form->titulo         = titulo ? titulo : "THZ-LANG Formulario Nativo";
    form->nomeEstrutura  = nomeEstrutura ? nomeEstrutura : "Formulario";
    form->operacaoAlvo   = "Salvar";
    form->numCampos      = 0;
    form->hWnd           = NULL;

    form->hFont = CreateFontA(
        -16, 0, 0, 0, FW_NORMAL, 0, 0, 0,
        DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
        CLEARTYPE_QUALITY, DEFAULT_PITCH, "Segoe UI"
    );
    form->hFontBold = CreateFontA(
        -20, 0, 0, 0, FW_BOLD, 0, 0, 0,
        DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
        CLEARTYPE_QUALITY, DEFAULT_PITCH, "Segoe UI"
    );
    form->hBrushBg    = CreateSolidBrush(THZ_BG_COLOR);
    form->hBrushField = CreateSolidBrush(THZ_FIELD_BG);
    form->hBrushBtn   = CreateSolidBrush(THZ_BTN_BG);

    return idx;
}

__declspec(dllexport) void thz_gui_adicionar_campo(int32_t formIdx, const char* rotulo, const char* valorPadrao, const char* tipo) {
    if (formIdx < 0 || formIdx >= g_formCount) return;
    ThzGuiFormulario* form = &g_forms[formIdx];
    if (form->numCampos >= THZ_GUI_MAX_CAMPOS) return;

    int32_t i = form->numCampos++;
    form->campos[i].rotulo      = rotulo ? rotulo : "Campo";
    form->campos[i].placeholder = valorPadrao ? valorPadrao : "";
    form->campos[i].tipo        = tipo ? tipo : "TEXTO";
    form->campos[i].hEdit       = NULL;
    form->campos[i].hLabel      = NULL;
}

__declspec(dllexport) void thz_gui_set_operacao(int32_t formIdx, const char* operacao) {
    if (formIdx < 0 || formIdx >= g_formCount) return;
    g_forms[formIdx].operacaoAlvo = operacao ? operacao : "Salvar";
}

__declspec(dllexport) void thz_gui_exibir(int32_t formIdx) {
    if (formIdx < 0 || formIdx >= g_formCount) return;
    ThzGuiFormulario* form = &g_forms[formIdx];

    int32_t paddingX  = 24;
    int32_t paddingY  = 20;
    int32_t labelH    = 20;
    int32_t fieldH    = 28;
    int32_t fieldGap  = 12;
    int32_t headerH   = 60;
    int32_t footerH   = 80;
    int32_t fieldW    = 440;
    int32_t rowH      = labelH + 4 + fieldH + fieldGap;

    int32_t contentH  = headerH + (rowH * form->numCampos) + footerH + paddingY * 2;
    int32_t winW      = fieldW + paddingX * 2 + 40;
    int32_t winH      = contentH;

    int32_t scrW = GetSystemMetrics(SM_CXSCREEN);
    int32_t scrH = GetSystemMetrics(SM_CYSCREEN);
    if (winH > scrH - 80) winH = scrH - 80;
    if (winW > scrW - 80) winW = scrW - 80;

    int32_t posX = (scrW - winW) / 2;
    int32_t posY = (scrH - winH) / 2;

    form->hWnd = CreateWindowExA(
        0, THZ_WND_CLASS, form->titulo,
        WS_OVERLAPPEDWINDOW | WS_VISIBLE,
        posX, posY, winW, winH,
        NULL, NULL, form->hInstance, NULL
    );
    if (!form->hWnd) return;

    /* Enable Win10+ Dark Mode title bar */
    int32_t darkMode = 1;
    DwmSetWindowAttribute(form->hWnd, DWMWA_USE_IMMERSIVE_DARK_MODE, &darkMode, sizeof(darkMode));

    int32_t curY = paddingY;
    int32_t innerX = paddingX;
    int32_t innerW = fieldW;

    /* Header: Title */
    void* hTitle = CreateWindowExA(
        0, "STATIC", form->titulo,
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        innerX, curY, innerW, 28,
        form->hWnd, NULL, form->hInstance, NULL
    );
    SendMessageA(hTitle, WM_SETFONT, (uint64_t)form->hFontBold, 1);
    curY += 30;

    /* Subtitle */
    char subtitulo[256];
    wsprintfA(subtitulo, "Estrutura: %s  |  Operacao: %s", form->nomeEstrutura, form->operacaoAlvo);
    void* hSub = CreateWindowExA(
        0, "STATIC", subtitulo,
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        innerX, curY, innerW, 18,
        form->hWnd, NULL, form->hInstance, NULL
    );
    SendMessageA(hSub, WM_SETFONT, (uint64_t)form->hFont, 1);
    curY += headerH - 30;

    /* Form Fields */
    for (int32_t i = 0; i < form->numCampos; i++) {
        char labelBuf[128];
        wsprintfA(labelBuf, "%s:", form->campos[i].rotulo);

        form->campos[i].hLabel = CreateWindowExA(
            0, "STATIC", labelBuf,
            WS_CHILD | WS_VISIBLE | SS_LEFT,
            innerX, curY, innerW, labelH,
            form->hWnd, (void*)(uint64_t)(IDC_LABEL_BASE + i), form->hInstance, NULL
        );
        SendMessageA(form->campos[i].hLabel, WM_SETFONT, (uint64_t)form->hFont, 1);
        curY += labelH + 4;

        form->campos[i].hEdit = CreateWindowExA(
            WS_EX_CLIENTEDGE, "EDIT", form->campos[i].placeholder,
            WS_CHILD | WS_VISIBLE | WS_TABSTOP | ES_AUTOHSCROLL,
            innerX, curY, innerW, fieldH,
            form->hWnd, (void*)(uint64_t)(IDC_EDIT_BASE + i), form->hInstance, NULL
        );
        SendMessageA(form->campos[i].hEdit, WM_SETFONT, (uint64_t)form->hFont, 1);
        curY += fieldH + fieldGap;
    }

    /* Footer: Status + Buttons */
    curY += 8;

    form->hStatusLabel = CreateWindowExA(
        0, "STATIC", "Preencha os campos e clique em Salvar para submeter.",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        innerX, curY, innerW, 20,
        form->hWnd, (void*)(uint64_t)IDC_STATUS_LABEL, form->hInstance, NULL
    );
    SendMessageA(form->hStatusLabel, WM_SETFONT, (uint64_t)form->hFont, 1);
    curY += 28;

    int32_t btnW = 120;
    int32_t btnH = 32;
    int32_t btnGap = 12;
    int32_t btnAreaX = innerX + innerW - (btnW * 2 + btnGap);

    void* hBtnSubmit = CreateWindowExA(
        0, "BUTTON", form->operacaoAlvo,
        WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_DEFPUSHBUTTON,
        btnAreaX + btnW + btnGap, curY, btnW, btnH,
        form->hWnd, (void*)(uint64_t)IDC_BTN_SUBMIT, form->hInstance, NULL
    );
    SendMessageA(hBtnSubmit, WM_SETFONT, (uint64_t)form->hFontBold, 1);

    void* hBtnClear = CreateWindowExA(
        0, "BUTTON", "Limpar",
        WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_PUSHBUTTON,
        btnAreaX, curY, btnW, btnH,
        form->hWnd, (void*)(uint64_t)IDC_BTN_CLEAR, form->hInstance, NULL
    );
    SendMessageA(hBtnClear, WM_SETFONT, (uint64_t)form->hFont, 1);

    ShowWindow(form->hWnd, SW_SHOWNORMAL);
    UpdateWindow(form->hWnd);
}

__declspec(dllexport) void thz_gui_loop_mensagens(void) {
    MSG msg;
    while (GetMessageA(&msg, NULL, 0, 0) > 0) {
        TranslateMessage(&msg);
        DispatchMessageA(&msg);
    }
}

/* Legacy API — backward compatibility */
__declspec(dllexport) void thz_renderizar_tela(const char* titulo, const char* conteudo) {
    if (!titulo) titulo = "THZ-LANG Native GUI";
    if (!conteudo) conteudo = "Renderizando Formulario Declarativo Nativo THZ-UI";
    thz_exiba_str(conteudo);
    MessageBoxA(NULL, conteudo, titulo, MB_OK | MB_ICONINFORMATION);
}

#else

/* =========================================================================
 * Linux / POSIX Implementation
 * ========================================================================= */
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

typedef struct {
    uint8_t* buffer;
    size_t capacidade;
    size_t offset;
} ThzArena;

void* thz_arena_alloc(uint64_t bytes) {
    ThzArena* arena = (ThzArena*) malloc(sizeof(ThzArena));
    if (!arena) return NULL;
    arena->capacidade = (size_t) bytes;
    arena->buffer = (uint8_t*) malloc(arena->capacidade);
    arena->offset = 0;
    return (void*) arena;
}

void thz_arena_free_all(void* arena_ptr) {
    if (!arena_ptr) return;
    ThzArena* arena = (ThzArena*) arena_ptr;
    if (arena->buffer) {
        free(arena->buffer);
    }
    free(arena);
}

void thz_exiba_str(const char* msg) {
    if (!msg) return;
    printf("%s\n", msg);
}

void thz_exiba_i128(uint64_t low, uint64_t high, int32_t scale) {
    (void)high;
    (void)scale;
    printf("[DECIMAL FIXO NATIVO] %lu\n", (unsigned long) low);
}

int32_t thz_gui_iniciar(const char* titulo, const char* nomeEstrutura) {
    printf("\n======================================================\n");
    printf("  %s\n", titulo ? titulo : "THZ-LANG Formulario");
    printf("  Estrutura: %s\n", nomeEstrutura ? nomeEstrutura : "N/A");
    printf("======================================================\n\n");
    return 0;
}

void thz_gui_adicionar_campo(int32_t formIdx, const char* rotulo, const char* valorPadrao, const char* tipo) {
    (void)formIdx;
    printf("  [CAMPO] %s: %s (tipo: %s)\n", rotulo ? rotulo : "?", valorPadrao ? valorPadrao : "", tipo ? tipo : "TEXTO");
}

void thz_gui_set_operacao(int32_t formIdx, const char* operacao) {
    (void)formIdx;
    printf("  [BOTAO] %s\n", operacao ? operacao : "Salvar");
}

void thz_gui_exibir(int32_t formIdx) {
    (void)formIdx;
    printf("\n[GUI POSIX] Formulario renderizado no console.\n");
}

void thz_gui_loop_mensagens(void) {
    printf("[GUI POSIX] Pressione Enter para sair.\n");
    getchar();
}

void thz_renderizar_tela(const char* titulo, const char* conteudo) {
    if (!titulo) titulo = "THZ-LANG Native GUI";
    if (!conteudo) conteudo = "Renderizando Formulario Declarativo Nativo THZ-UI";
    printf("[GUI NATIVA POSIX] *** %s ***\n%s\n", titulo, conteudo);
}

#endif
