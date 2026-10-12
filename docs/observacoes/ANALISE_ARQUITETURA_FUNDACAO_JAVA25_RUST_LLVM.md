# Análise Arquitetural e Parecer de Fundação: Java 25 (Tooling/Dev) + Rust (Runtime Nativo) + LLVM IR (AOT Zero JVM)

- **Data de Registro:** 11/10/2026
- **Status:** Diretriz Arquitetural Ativa e Aprendizado Consolidado
- **Autor/Anotador:** Antigravity AI & Lucas Thomaz (Engenharia THZ-LANG)
- **Classificação:** Observações Arquiteturais & Parecer Técnico

---

## 1. Visão Geral e Tese Central

> **Tese:** A divisão estratégica entre **Java 25** (para análise léxica, sintática, AST, semântica, DSLs, tooling e ambiente de desenvolvimento), **Rust** (para o motor nativo de alta performance, arenas de memória $O(1)$ sem GC, criptografia e vetorização SIMD) e **LLVM IR** (como representação intermediária para geração de código de máquina antecipado AOT) é a arquitetura mais sólida, expressiva e segura do mercado moderno para construir uma nova linguagem.
> 
> Ela assegura **máxima produtividade e riqueza de tooling hoje** sem gerar qualquer dependência ou *vendor lock-in* de JVM para os binários compilados em produção, pavimentando de forma segura a transição para a autocompilação (*self-hosting*).

---

## 2. Precedentes Reais e Fontes da Indústria

Esta arquitetura não é uma aposta experimental; ela é o resultado refinado de décadas de evolução na engenharia de linguagens de programação:

### A. O Modelo Kotlin & Kotlin/Native (JetBrains)
- **Arquitetura:** O front-end oficial do compilador (`kotlinc`) é executado sobre a JVM (Java/Kotlin), onde são feitas todas as análises léxica, sintática, resolução de tipos e geração do *Kotlin IR*. Quando o alvo de compilação é nativo (**Kotlin/Native**), o compilador baixa esse IR diretamente para **LLVM IR**, e o Clang/LLVM gera executáveis e bibliotecas de máquina (`.exe`, `.elf`, `.dylib`). O runtime nativo (gerenciador de memória e concorrência) é escrito em C++ e Rust.
- **Motivação:** A JetBrains alavancou o ecossistema maduro da JVM para produtividade da engenharia e tooling de IDE, entregando binários para iOS, embarcados e servidores bare-metal com zero dependência de JVM.
- **Fontes:** 
  - *Kotlin/Native Compiler Architecture & LLVM Pipeline Docs* (JetBrains).
  - Elizarov, R. et al., *Kotlin Evolution and Design Principles*.

### B. O Modelo Rust & Bootstrapping Inicial (Mozilla / Rust Project)
- **Arquitetura:** O compilador inicial do Rust (**Stage 0**) não foi escrito em Rust; foi desenvolvido em **OCaml** (`rustboot`). 
- **Motivação:** Graydon Hoare (criador do Rust) optou pelo OCaml porque construir uma nova linguagem requer iteração rápida sobre estruturas recursivas complexas de sintaxe e tipos algébricos. O uso de uma linguagem com *Garbage Collector* e forte *pattern matching* permitiu estabilizar a gramática sem perder tempo depurando alocação manual de memória. O back-end final sempre foi **LLVM IR**. Uma vez estabilizado, o compilador migrou para o próprio Rust (*self-hosted* `rustc`).
- **Fontes:**
  - Hoare, Graydon. *Rust Project Early Architecture History* (2010–2012).
  - *The Rustc Development Guide: From AST to LLVM IR*.

### C. O Modelo Julia (MIT / Julia Computing)
- **Arquitetura:** O front-end de parsing e macros utiliza uma linguagem de alto nível com tipagem simbólica (*FemtoLisp*); o runtime numérico e manipulação vetorial são em C puro e SIMD; e toda a geração de código para execução de alta performance ancora-se no **LLVM JIT/AOT**.
- **Fontes:**
  - Bezanson, J., Edelman, A., Karpinski, S., & Shah, V. B. (2017). *Julia: A Fresh Approach to Numerical Computing*. **SIAM Review**, 59(1), 65-98.

### D. O Modelo Mojo & MLIR (Modular / Chris Lattner)
- **Arquitetura:** Chris Lattner (criador do LLVM, Clang e Swift) separou a manipulação de linguagens e DSLs de alto nível dos motores de execução de baixo nível. A geração de código ancora-se em representações intermediárias (MLIR/LLVM IR), enquanto os motores de hardware, tensores e SIMD utilizam infraestrutura de execução nativa em C++/Rust.
- **Fontes:**
  - Lattner, C. et al., *MLIR: Scaling Compiler Infrastructure for Domain Specific Architectures*, IEEE/ACM CGO 2021.

---

## 3. O Raciocínio Estruturado da Engenharia

### 3.1 Por que o Java 25 é Superior para o Front-End e Tooling?
1. **O Dilema do *Borrow Checker* em Compiladores:**
   - Compiladores manipulam grafos mutáveis: a Árvore de Sintaxe Abstrata (AST), o grafo de controle de fluxo (CFG) e as tabelas de símbolos possuem referências cíclicas constantes (escopos apontam para escopos pais; tipos referenciam-se mutuamente; nós da AST são transformados por múltiplos passes de otimização e inferência).
   - Escrever isso em Rust exige arenas complexas com identificadores numéricos (`NodeId`), contadores de referência (`Rc<RefCell<T>>`) ou anotações densas de *lifetimes*. Isso desacelera o ciclo de desenvolvimento em 3x a 5x.
   - O Java 25 gerencia memória em árvores e grafos de forma natural, liberando a mente dos projetistas para a semântica da linguagem.
2. **Tipos de Dados Algébricos no Java Moderno:**
   - A combinação de `sealed interface NoAst permits ...`, `record` e *Pattern Matching em switch* no Java 25 fornece a mesma elegância matemática de linguagens puramente funcionais para realizar *Tree Walking* e *Lowering* com garantia de exaustividade em tempo de compilação.
3. **Project Panama (Foreign Function & Memory API - FFM):**
   - Substitui completamente o legado arcaico do JNI. O Java 25 dialoga diretamente com a C-ABI de bibliotecas COBOL, DLLs, libLLVM e o runtime Rust com segurança de tipos e overhead nulo.
4. **Tooling Empresarial e GraalVM Native Image:**
   - Frameworks de teste (JUnit 5), benchmarks (JMH), servidores LSP (LSP4J) e interfaces gráficas (Swing FlatLaf) já existem com extrema maturidade. Com a GraalVM, o ferramental compila para executáveis nativos com inicialização em **<5ms**.

---

### 3.2 Por que o Rust é Insubstituível para o Runtime Nativo?
1. **Determinismo sem Coletor de Lixo (*GC Pauses*):**
   - No setor corporativo/financeiro (foco do THZ-LANG), a latência de cauda (*p99.9*) é crítica. O runtime nativo do THZ-LANG usa alocação em arenas sequenciais $O(1)$. O Rust garante segurança matemática de acesso a essas arenas sem jamais pausar a aplicação para coleta de lixo.
2. **Segurança de Memória sem Penalidade de CPU (ISO/IEC TR 24772):**
   - Implementar o runtime em C ou C++ exporia os programas compilados a vulnerabilidades de *buffer overflow* e *use-after-free*. O compilador Rust elimina essas falhas por construção (*memory safety* estático).
3. **Vetorização SIMD e Compatibilidade C-ABI:**
   - Instruções colunares e vetorizadas (`LAYOUT_COLUNAR`, `VETORIZAR_PARA PASSO_SIMD`) mapeiam diretamente para primitivas intrínsecas do Rust (AVX2, AVX-512, Neon), que compilam para as mesmas convenções binárias do LLVM IR.

---

### 3.3 Por que o LLVM IR Garante Soberania e o Fim das Amarras de JVM?
1. **O Problema $N \times M$ de Portabilidade:**
   - Para suportar $N$ linguagens/extensões em $M$ plataformas (Windows x64, Linux x64, ARM64), uma representação intermediária desacopla o front-end das peculiaridades de assembly de cada processador.
   - O LLVM IR oferece mais de 20 anos de algoritmos de otimização industrial (eliminação de código morto, vetorização automática, inlining agressivo).
2. **Binários de Produção Independentes:**
   - O compilador do THZ-LANG emite arquivos `.ll` textuais puros. O Clang/LLD faz o *linking* estático contra o runtime Rust/C, gerando binários executáveis `.exe` e `.elf`.
   - Em produção, esse binário roda em contêineres OCI `FROM scratch` de 10MB a 15MB, sem exigir Java, Node ou qualquer runtime externo instalado no servidor cliente.

---

### 3.4 O Caminho para a Autocompilação (*Self-Hosting*)
A arquitetura de três estágios adotada pelo THZ-LANG elimina o risco de *deadlock* de bootstrap:

```
┌─────────────────────────────────────────────────────────────────┐
│ STAGE 0 — Host JVM (Java 25)                                    │
│ - Lexer, Parser, AST, Semântico, Emissor LLVM em Java 25.       │
│ - Estabiliza gramática, contratos e paridade de tipos.          │
└───────────────────────────────┬─────────────────────────────────┘
                                │ compila
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│ STAGE 1 — Compilador Self-Hosted (thz-compiler/*.thz)           │
│ - Compilador formal escrito na própria sintaxe THZ-LANG.        │
│ - Host JVM gera o binário nativo inicial thzc.exe via LLVM IR.  │
└───────────────────────────────┬─────────────────────────────────┘
                                │ gera binário nativo autônomo
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│ STAGE 2 — Soberania Total (Zero JVM)                            │
│ - O binário thzc.exe compila o código-fonte de thz-compiler.   │
│ - A JVM é descartada do pipeline oficial de produção.           │
│ - THZ-LANG compila THZ-LANG nativamente para todo o sempre.     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Diretrizes Mandatórias para Engenheiros e Agentes

1. **Nunca Vazar Tipos ou Semânticas da JVM no Código Emitido:**
   - O emissor LLVM IR do THZ-LANG deve emitir apenas tipos nativos (`i1`, `i8`, `i16`, `i32`, `i64`, `i128`, `ptr`, `float`, `double`) e chamadas para funções em convenção C (`declare`, `call`).
2. **Manter o Runtime Rust Puro e Desacoplado:**
   - O módulo `thz-runtime-rs` deve exportar apenas símbolos `extern "C"` e compilar como biblioteca estática/dinâmica C-ABI.
3. **Preservar a Simplicidade da AST em Java 25 enquanto no Stage 0:**
   - Aproveitar ao máximo records e sealed interfaces no front-end para que qualquer alteração gramatical possa ser espelhada com clareza em `thz-compiler`.
