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

---

## 🧠 Gerenciamento de Memória Transparente (Zero-Copy) e o Papel de JNI / FFI

### 1. Como fica o JNI ou FFI nisso tudo?
- **No Ambiente de Produção Nativa (LLVM AOT / Zero JVM):**
  - **Não existe JNI e não há camada intermediária de FFI.** 
  - O código THZ compilado e o código COBOL compilado residem no **mesmo espaço de endereçamento de processo**, no mesmo binário (`.exe` ou `.elf`).
  - Chamadas entre THZ e COBOL obedecem à convenção de chamada nativa C padrão (Microsoft x64 ou System V AMD64). Para o processador, a invocação é uma simples instrução de máquina `call`, sem overhead de transição de contexto.
- **No Ambiente de Desenvolvimento (Java 25 Dev-Host):**
  - **O antigo JNI foi 100% descartado.**
  - Utilizamos a **Project Panama (Foreign Function & Memory API - `java.lang.foreign.*`)**:
    - Alocação de memória nativa contígua fora da heap da JVM (*off-heap*) via `MemorySegment` e `Arena.ofConfined()`.
    - Os bytes alocados têm exatamente o mesmo layout físico e offsets que o COBOL e o LLVM IR esperam, sem cópias ou serializações intermediárias.

### 2. Memória Compartilhada e Paridade Absoluta com a Máquina
Em um mainframe clássico, os programas COBOL não realizam alocações dinâmicas dispersas; eles operam sobre blocos contíguos de bytes (`WORKING-STORAGE` e `LINKAGE SECTION`), passando ponteiros por referência (`BY REFERENCE`).

No THZ-LANG, essa mecânica é espelhada com perfeição:
1. **Layout Contíguo Unificado (`CobolMemoryLayout`):** Calcula os offsets exatos em bytes para cada campo (Alfanumérico, Display e COMP-3 Packed Decimal).
2. **Buffer Compartilhado Zero-Copy (`CobolSharedMemorySegment`):**
   - O THZ aloca a memória e escreve nos offsets dos campos.
   - O COBOL recebe o ponteiro do buffer compartilhado, executa instruções da `PROCEDURE DIVISION` (`MOVE`, `COMPUTE`, `ADD`) e **muta os bytes in-place**.
   - O THZ lê imediatamente do **mesmo endereço de memória**. Não há áreas reservadas exclusivas onde o outro não tenha conhecimento.
3. **No LLVM IR Nativo:**
   - O acesso a qualquer campo ocorre via instrução `getelementptr inbounds i8, ptr %bufferCompartilhado, i64 %offsetByte`.
   - Para o otimizador LLVM, para o linker LLD e para a CPU, **THZ-LANG e COBOL são indistinguíveis**: são apenas instruções `load` e `store` operando cooperativamente sobre os mesmos registradores e o mesmo bloco de memória.

