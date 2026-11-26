# ADR-011 — Motor de TUIs Dinâmicas e Reativas para THZ-LANG

**Status:** Aceito  
**Data:** 16/10/2026  
**Autores:** Arquitetura THZ-LANG

---

## 🏛️ Contexto

A demanda por ferramentas de linha de comando sofisticadas, dashboards operacionais de DevOps, monitores de infraestrutura e aplicações financeiras de baixa latência em terminal aumentou substancialmente. Sistemas modernos como `lazygit`, `btop`, `k9s` e `htop` demonstraram a viabilidade de interfaces em modo texto extremamente responsivas e visualmente elegantes.

O THZ-LANG já contava com o `thz-agent-jvm` utilizando sequências ANSI pontuais e o `thz-gui-jvm` com interfaces Web/Desktop. Faltava, no entanto, um motor formal de **Terminal User Interface (TUI)** dinâmico, com suporte a desenho de caixas, layouts responsivos em buffer virtual, tabelas paginadas, barras de progresso animadas e TrueColor (RGB 24-bit).

---

## 🎯 Decisão

1. **Abstração Unificada de Buffer de Tela Virtual (`ThzTerminalTui`)**:
   - Implementar no núcleo `thz-core-jvm` uma infraestrutura de TUI em modo texto que opera sobre uma matriz virtual de caracteres e células de atributos (`caractere`, `fgColor`, `bgColor`, `estilo`).
   - Recursos primários:
     - Caixa e bordas elegantes com caracteres Unicode UTF-8 (`┌─┐│└─┘`, `╔═╗║╚═╝`);
     - Títulos centralizados ou alinhados;
     - Barras de progresso com porcentagem e gradientes de cor;
     - Tabelas de dados com cabeçalhos e formatação tabular;
     - Cores ANSI TrueColor (RGB 24-bit) e fallback para paleta de 16 cores;
     - Renderização diferencial (diffing) ou em bloco único para evitar cintilação (*flicker*).

2. **Exposição na Biblioteca Padrão (`TUI.*`)**:
   - Registrar no mapa de funções globais da `BibliotecaPadrao` as primitivas:
     - `TUI.janela(titulo, largura, altura, corBorda)`
     - `TUI.barraProgresso(largura, percentual, cor)`
     - `TUI.tabela(cabecalhos, linhas)`
     - `TUI.renderizar()`
     - `TUI.limpar()`

3. **Ponte de Alta Performance com Rust (`thz-runtime-rs`)**:
   - Para execução AOT nativa de altíssima taxa de quadros (60 FPS em terminal), expor bindings C-ABI para a biblioteca de referência em Rust (**Ratatui** / **Crossterm**), permitindo que binários compilados em C-ABI tirem proveito direto de renderização SIMD de buffers de terminal.

---

## ⚡ Consequências

- **Aplicações de Linha de Comando de Primeira Classe:** Desenvolvedores THZ podem criar dashboards operacionais e ferramentas interativas diretamente em seus programas sem precisar recorrer a frameworks gráficos pesados.
- **Leveza:** O motor funciona em qualquer terminal moderno (Windows Terminal, Linux Bash, macOS Terminal, SSH headless).
