# Modernização e Interoperabilidade com COBOL no THZ-LANG

O THZ-LANG oferece suporte integrado e nativo para compreensão, ingestão e execução de códigos **COBOL** através do módulo [`thz-lang-cobol`](https://github.com/THZ-Lang/thz-lang-cobol).

---

## 🏛️ As 3 Variações de Modernização de COBOL

O ecossistema THZ-LANG atende a três estratégias distintas de modernização, da coexistência cirúrgica à execução autônoma de legados inteiros:

| Variação | Modalidade | Descrição | Caso de Uso Principal |
| :--- | :--- | :--- | :--- |
| **Variação 1** | **COBOL Embutido (`BLOCO_COBOL`)** | Blocos de código COBOL declarados dentro de arquivos `.thz`. | Migração pontual de sub-rotinas sem alterar a estrutura do programa THZ. |
| **Variação 2** | **Contratos e Copybooks (`.cpy`)** | Transpilação de definições de dados de mainframe diretamente para `ESTRUTURA` THZ nativas. | Reuso de modelos de dados, schemas bancários e tabelas em lote sem recriar tipos. |
| **Variação 3** | **THZ-COBOL Standalone (Puro)** | Execução e compilação direta de arquivos `.cbl` 100% puros sob o runtime do THZ, sem misturar sintaxes (análogo a **Jython**, **IronPython** e **Cython**). | "Drop-in replacement" para rodar programas legados em massa com arenas $O(1)$ e compilação AOT nativa via LLVM IR (Zero JVM). |

---

## 🚀 Capacidades Técnicas Detalhadas

1. **Importação de Copybooks (`.cpy`)**:
   Converte definições de dados de mainframe (inclusive `COMP-3`, `PIC X`, `PIC 9`, condições de nível `88`) em `ESTRUTURA` tipadas nativas do THZ-LANG.
2. **COBOL Embutido (`BLOCO_COBOL ... FIM_BLOCO_COBOL`)**:
   Permite embutir código COBOL diretamente dentro de programas `.thz`.
3. **THZ-COBOL Autônomo (Variação 3)**:
   O compilador aceita arquivos `.cbl` puros, executa os parágrafos da `PROCEDURE DIVISION` e emite LLVM IR nativo completo com a função `@main` do sistema operacional.
4. **Compilação Nativa AOT (Zero JVM)**:
   Emissão direta de representação intermediária **LLVM IR** (`.ll`), gerando binários `.exe` (Windows) e `.elf` (Linux) que não exigem JVM instalada em produção.

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
