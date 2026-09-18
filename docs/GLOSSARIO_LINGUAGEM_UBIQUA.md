# Glossário de Linguagem Ubíqua — THZ-LANG (v2.4.0)

Em **Domain-Driven Design (DDD)**, a **Linguagem Ubíqua** é o vocabulário rigoroso e compartilhado por analistas de negócio, arquitetos de software, engenheiros de dados e desenvolvedores. 

No **THZ-LANG**, esse conceito foi elevado ao nível de código-fonte compilável: os termos de governança, finanças, arquitetura, infraestrutura e pipelines de dados são nativos na sintaxe da linguagem em português.

---

## 📌 Sumário por Categorias
1. [Governança, DDD & Contratos Formais](#1-governança-ddd--contratos-formais)
2. [Aritmética Exata & Domínio Financeiro](#2-aritmética-exata--domínio-financeiro)
3. [Arquitetura & Módulos Corporativos](#3-arquitetura--módulos-corporativos)
4. [Big Data & Pipelines de Ingestão Massiva](#4-big-data--pipelines-de-ingestão-massiva)
5. [Engenharia Orientada a Dados & SIMD (DoD)](#5-engenharia-orientada-a-dados--simd-dod)
6. [Interface Gráfica Declarativa & UIs (`.thzui`)](#6-interface-gráfica-declarativa--uis-thzui)

---

## 1. Governança, DDD & Contratos Formais

### **`REGRA_NEGOCIO` (Regra de Negócio)**
Unidade autônoma e auditável de lógica corporativa. Ao contrário de uma função genérica, uma regra de negócio carrega rastreabilidade de requisitos e contratos executáveis.

### **`EXIGE` (Pré-condição)**
Contrato formal que especifica quais condições de entrada **devem** ser satisfeitas antes da execução do bloco. Se violada, a execução é interrompida com falha explícita de contrato.

### **`GARANTE` (Pós-condição)**
Contrato formal que garante o estado resultante esperado após a execução bem-sucedida de uma regra de negócio.

### **`INVARIANTE` (Invariante de Domínio)**
Condição de verdade absoluta que uma entidade ou estrutura de dados deve manter durante todo o seu ciclo de vida.

### **`RASTREIO_REQUISITO` (Rastreabilidade de Requisitos)**
Vínculo declarativo que conecta um trecho de código diretamente ao identificador do requisito funcional de negócio (ex: `"REQ-FIN-2026-001"`).

### **`METADADOS_ARQUITETURA` (Documentação Viva de Arquitetura)**
Bloco obrigatório em programas corporativos que expõe propriedades como `SISTEMA`, `MODULO`, `DOMINIO`, `CAMADA`, `RESPONSAVEL`, `CONFORMIDADE` e `CRITICIDADE`.

### **`SLO_LATENCIA_MS` (Objetivo de Nível de Serviço)**
Declaração explícita do limite máximo de tempo aceitável em milissegundos para a execução daquele componente.

---

## 2. Aritmética Exata & Domínio Financeiro

### **`DECIMAL(P, S)` (Decimal Fixo de Precisão Exata)**
Tipo numérico exato baseado na norma ISO/IEC 10967. Proíbe qualquer uso de ponto flutuante binário IEEE 754 (`float`/`double`), impedindo erros de arredondamento em cálculos fiscais ou monetários.

### **`MONETARIO(Moeda)` (Valor Monetário Tipado com ISO 4217)**
Tipo especializado para valores monetários associados obrigatoriamente a um código de moeda válido de 3 letras (ex: `1500.00 BRL`, `99.99 USD`). Proíbe operações aritméticas diretas entre moedas distintas sem conversão explícita.

### **Arredondamento Bancário (*Half-Even*)**
Algoritmo de arredondamento para a casa par mais próxima quando o dígito seguinte é exatamente 5, evitando viés estatístico acumulado em balanços financeiros.

---

## 3. Arquitetura & Módulos Corporativos

### **Arquétipo de Módulo**
Classificação nativa do propósito arquitetural de um arquivo fonte no THZ-LANG:
- **`PROGRAMA NEGOCIO`**: Serviços backend e processamento de negócios.
- **`PROGRAMA VISUAL`**: Aplicação gráfica desktop ou web.
- **`PROGRAMA ARQUITETURA`**: Mapeamento de componentes e sistemas.
- **`PIPELINE_DADOS`**: Ingestão e processamento massivo de Big Data.
- **`BIBLIOTECA`**: Utilitários reutilizáveis sem efeito colateral global.
- **`EXTENSAO`**: Módulos de expansão do ecossistema.
- **`FERRAMENTA`**: Scripts e utilitários CLI.
- **`TESTE`**: Suíte declarativa de testes de regressão.
- **`TELA`**: Definição declarativa de interface visual (`.thzui`).

### **Terminador Pareado**
Regra sintática onde o encerramento de um módulo exige a sintaxe exata correspondente ao seu arquétipo (`FIM_PROGRAMA`, `FIM_PIPELINE`, `FIM_BIBLIOTECA`, `FIM_FERRAMENTA`, `FIM_TESTE`, `FIM_TELA`).

### **`RESULTADO` / `FALHAR_COM` (Canal Seguro de Retorno)**
Padrão idiomático de tratamento de erro que substitui o lançamento incondicional de exceções runtime por um canal tipado de `SUCESSO` ou `ERRO`.

### **`CASO_RESULTADO` (Desempacotamento de Resultado)**
Estrutura declarativa de *pattern matching* para tratar os canais `SUCESSO` e `ERRO` de um `RESULTADO`.

---

## 4. Big Data & Pipelines de Ingestão Massiva

### **`PIPELINE_DADOS`**
Arquétipo de módulo focado no fluxo contínuo ou em lote de dados em larga escala.

### **`FONTE_ENTRADA` / `DESTINO_SAIDA`**
Declaração de conectores externos (PostgreSQL, MySQL, MongoDB, JSONB, CSV, Kafka) para ingestão e exportação de dados.

### **`STREAMING` & `LOTE`**
Modos de operação: processamento evento a evento em tempo real (*Streaming*) ou agregação em lotes temporais (*Batch*).

### **`TRANSFORMACAO`**
Etapa de processamento de dados contendo contratos de integridade e cálculos vetorizados.

---

## 5. Engenharia Orientada a Dados & SIMD (DoD)

### **`LAYOUT_COLUNAR` (Structure of Arrays — SoA)**
Modificador de `ESTRUTURA` que orienta a disposição dos dados na memória de forma colunar contígua em vez de um array de objetos tradicionais (AoS), viabilizando vetorização de alto rendimento na CPU.

### **`VETORIZAR_PARA` / `PASSO_SIMD`**
Laço de repetição especializado que instrui o compilador a emitir instruções SIMD (Single Instruction, Multiple Data, como AVX2 / AVX-512) para processar blocos contíguos em paralelo.

### **`USAR_BLOCO_MEMORIA` (Arena de Memória Contígua)**
Padrão de alocação de memória em arena (*Arena Memory Allocation*), permitindo alocação e descarte contíguo de grandes volumes de dados em complexidade $O(1)$ sem pausar a aplicação.

---

## 6. Interface Gráfica Declarativa & UIs (`.thzui`)

### **`.thzui` (Extensão de Interface Gráfica)**
Extensão oficial de arquivo para componentes e telas visuais desenvolvidas com o arquétipo `TELA`.

### **`ThzUiMaker` (Criador Declarativo de UI)**
Engine e Fluent Builder interno que transforma código declarativo THZ-LANG em componentes nativos (Swing FlatLaf) ou páginas HTML5 semânticas com CSS Glassmorphism e JavaScript Bridge (`window.thz`).

### **MetricaCard (KPI Card)**
Componente visual especializado para exibição de indicadores operacionais e financeiros (título, valor formatado, moeda e emblema de tendência).
