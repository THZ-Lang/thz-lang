# ROADMAP ESTRATÉGICO — THZ-LANG (v2.6.0 ~ v3.0.0)

Este documento define os 5 grandes pilares de inovação técnica e arquitetural do **THZ-LANG** para consolidar a linguagem como o padrão em sistemas corporativos de missão crítica, inteligência artificial soberana e alta performance.

---

```
┌───────────────────────────────────────────────────────────────────────────────────┐
│                             ROADMAPPING THZ-LANG                                  │
└────────┬──────────────┬───────────────┬──────────────────┬────────────────┬───────┘
         │              │               │                  │                │
         ▼              ▼               ▼                  ▼                ▼
   ┌───────────┐  ┌───────────┐   ┌───────────┐      ┌───────────┐    ┌───────────┐
   │ PILAR 1   │  │ PILAR 2   │   │ PILAR 3   │      │ PILAR 4   │    │ PILAR 5   │
   │ IA & RAG  │  │ LINQ / QL │   │ Streaming │      │ WebAssm   │    │ Debugger  │
   │ On-Device │  │  Tipado   │   │    EDA    │      │   WASM    │    │    DAP    │
   └───────────┘  └───────────┘   └───────────┘      └───────────┘    └───────────┘
```

---

## 🏛️ PILAR 1: IA Nativa & RAG On-Device (Zero Python)

### Objetivo
Permitir que sistemas corporativos executem modelos de **Machine Learning Clássico** (Random Forest, K-Means, SVM), **Embeddings** (BERT, BGE) e **Modelos de Linguagem** (GGUF / Llama 3, Phi-3, Mistral) diretamente dentro do binário nativo THZ, com latência em sub-milissegundos, 100% de privacidade LGPD e zero custos de nuvem.

### Stack Técnica:
- **Rust Runtime (`src/runtime_rs`):**
  - `Candle` (Hugging Face pure-Rust ML) para LLMs e tensores quantizados.
  - `ONNX Runtime` (`ort` / `tract`) para execução universal de modelos `.onnx`.
  - `Linfa` para algoritmos de Machine Learning clássico.
- **Java 25 (`JVM/thz-core-jvm`):**
  - `Project Panama` (`java.lang.foreign`) para FFI nativo Zero-Copy entre JVM e Rust.
  - `Project Loom` (Virtual Threads) para atender dezenas de milhares de predições concorrentes.
  - `Vector API` (`jdk.incubator.vector`) para vetorização SIMD de tensores na memória.
- **Sintaxe da Linguagem:**
  - Stdlib `IA.*` (`IA.embedding`, `IA.completar`) e `ML.*` (`ML.classificar`, `ML.predizer`).
  - Arquétipo declarativo `BASE_CONHECIMENTO` para RAG automático.

---

## ⚡ PILAR 2: Consultas Tipadas Nativas (Type-Safe Query DSL / LINQ)

### Objetivo
Permitir a escrita de consultas ricas a bancos de dados (SQLite, PostgreSQL, MySQL) e coleções em memória diretamente na gramática da linguagem, com validação semântica estática em tempo de compilação, eliminando 100% dos riscos de *SQL Injection* e erros de tipagem em tempo de execução.

### Sintaxe Canônica:
```thz
VARIAVEL clientesEspeciais <- CONSULTAR Cliente
                              ONDE limite_credito > 50000.00 E status = ATIVO
                              ORDENAR_POR nome ASC
                              LIMITE 20
```

### Componentes:
- **Gramática & AST:** Palavras-chave `CONSULTAR`, `DE`, `ONDE`, `ORDENAR_POR`, `AGRUPAR_POR`, `LIMITE`, `PULAR`.
- **Análise Semântica:** Validação dos campos e operadores em relação aos tipos da `ESTRUTURA`.
- **Motor Dual:**
  - *Em memória:* Pipeline vetorizado com predicados compilados.
  - *Em banco:* Compilação determinística para SQL parametrizado (`PreparedStatements`).

---

## 🔗 PILAR 3: Mensageria & Arquitetura Reativa (Streaming / EDA)

### Objetivo
Disponibilizar primitivas nativas de comunicação assíncrona orientada a eventos para sistemas distribuídos, com suporte a *RingBuffers* locais de alta taxa de transferência (LMAX Disruptor) e corretores corporativos (Kafka, RabbitMQ, MQTT).

### Sintaxe Canônica:
```thz
CONSUMIDOR_EVENTO ProcessadorFatura
    TOPICO: "pedidos.aprovados"
    GRUPO: "faturamento-lote"
    GARANTE_ORDEM: VERDADEIRO

    AO_RECEBER(evento: EventoPedido)
        # Execução reativa protegida por contratos
    FIM_AO_RECEBER
FIM_CONSUMIDOR
```

### Componentes:
- Módulo `MENSAGERIA.*` (`publicar`, `consumir`, `confirmar_ack`).
- Gerenciamento de canal por Virtual Threads dedicadas no Java 25.

---

## 🌐 PILAR 4: Compilação para WebAssembly (WASM)

### Objetivo
Compilar o motor de regras de negócio, validadores fiscais e interfaces visuais do THZ para binários WebAssembly (`.wasm`), permitindo execução universal com isolamento de memória e inicialização instantânea no navegador e em Edge Workers (Cloudflare Workers, Vercel Edge).

### Componentes:
- Backend `wasm32-unknown-unknown` no runtime Rust (`src/runtime_rs`).
- Comando CLI `thz build --target=wasm`.
- Bindings TypeScript automáticos para o Monaco Editor e Playground Web.

---

## 🛠️ PILAR 5: Depuração Nativa na IDE (Debug Adapter Protocol - DAP)

### Objetivo
Oferecer uma experiência de desenvolvimento e diagnóstico completa de nível industrial, permitindo depuração interativa passo a passo tanto na extensão do VS Code quanto na Desktop IDE FlatLaf Swing (`thz-gui-jvm`).

### Capacidades:
- **Pontos de Interrupção (Breakpoints):** Por linha de código e condicionais (ex: parar apenas se `total > 10000.00`).
- **Controle de Execução:** Avançar passo a passo (*Step In*, *Step Over*, *Step Out*, *Continue*).
- **Inspeção de Estado:** Visualização de variáveis locais, campos de estruturas, pilha de chamadas (*Call Stack*) e estado de arenas.
- **Servidor DAP:** Implementação do protocolo da Microsoft via TCP ou stdio.

---

## 📅 Matriz de Fases e Marcos

| Fase | Foco Principal | Prazo Estimado | Módulos Impactados |
| :--- | :--- | :--- | :--- |
| **Fase 1** | **IA & RAG On-Device (Panama + ONNX + Candle)** | v2.6.0 | `src/runtime_rs`, `thz-core-jvm`, `BibliotecaPadrao` |
| **Fase 2** | **Consultas Tipadas Nativas (LINQ / Query DSL)** | v2.7.0 | `lexer`, `parser`, `analisador`, `interpretador` |
| **Fase 3** | **Mensageria Reativa & Eventos (EDA)** | v2.8.0 | `thz-core-jvm`, `thz-api-jvm`, `MENSAGERIA` |
| **Fase 4** | **Debugger Nativo (DAP - Debug Adapter Protocol)** | v2.9.0 | `thz-lsp-jvm`, `thz-gui-jvm`, `Extensions/thz-lsp-vscode` |
| **Fase 5** | **Compilação WebAssembly (WASM)** | v3.0.0 | `compilador/`, `src/runtime_rs`, `playground/` |
