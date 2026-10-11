# Modernização e Interoperabilidade com COBOL no THZ-LANG

O THZ-LANG oferece suporte integrado e nativo para compreensão, ingestão e execução de códigos **COBOL** através do módulo [`thz-lang-cobol`](https://github.com/THZ-Lang/thz-lang-cobol).

---

## 🚀 Capacidades Principais

1. **Importação de Copybooks (`.cpy`)**:
   Converte definições de dados de mainframe (inclusive `COMP-3`, `PIC X`, `PIC 9`, condições de nível `88`) em `ESTRUTURA` tipadas nativas do THZ-LANG.
2. **COBOL Embutido (`BLOCO_COBOL ... FIM_BLOCO_COBOL`)**:
   Permite embutir código COBOL diretamente dentro de programas `.thz`.
3. **Transpilação de Lógica**:
   Converte rotinas da `PROCEDURE DIVISION` em `REGRA_NEGOCIO` do THZ-LANG, gerando contratos `CONTRATO_ENTRADA` (`EXIGE`) e `CONTRATO_SAIDA` (`GARANTE`).
4. **Compilação Nativa AOT (Zero JVM)**:
   Emissão direta de representação intermediária **LLVM IR** (`.ll`), permitindo ligar rotinas nativas de COBOL compiladas via C-ABI em binários `.exe` (Windows) e `.elf` (Linux) que não exigem JVM instalada em produção.

---

## 📖 Exemplo: Copybook para ESTRUTURA THZ

### Copybook Original (`CLIENTE.CPY`):
```cobol
01  REGISTRO-CLIENTE.
    05  CLI-CODIGO           PIC 9(8).
    05  CLI-NOME             PIC X(30).
    05  CLI-SALDO            PIC S9(13)V99 COMP-3.
```

### Código THZ-LANG Gerado:
```thz
ESTRUTURA RegistroCliente
    cli_codigo : INTEIRO32 # COBOL: PIC 9(8) DISPLAY
    cli_nome   : TEXTO     # COBOL: PIC X(30) DISPLAY
    cli_saldo  : DECIMAL   # COBOL: PIC S9(13)V99 COMP-3
FIM_ESTRUTURA
```

---

## 🛠️ Exemplo: Execução e Compilação Nativa LLVM IR

No pipeline AOT, o compilador emite tipos compostos em LLVM e conversores embutidos:

```llvm
; Layout nativo LLVM
%struct.Cobol_REGISTRO_CLIENTE = type { i32, [30 x i8], [8 x i8] }

; Conversores matematicos de alta performance COMP-3 (BCD) <-> i128 (DECIMAL THZ)
declare i128 @thz_cobol_comp3_to_i128(ptr %bytes, i32 %totalDigits, i32 %scale)
declare void @thz_cobol_i128_to_comp3(i128 %val, i32 %totalDigits, i32 %scale, ptr %dst)

; Ligacao direta C-ABI
declare i32 @cob_call(ptr %nomePrograma, i32 %totalArgs, ptr %ponteirosArgs)
```
