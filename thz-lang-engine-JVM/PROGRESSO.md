# PROGRESSO DE IMPLEMENTAÇÃO — THZ-LANG Engine JVM25

> Registro consolidado do estado da implementação. Atualizado em **2026-08-23**.
> Espelho Node/TS de referência: `../thz-lang-engine` (v2.3 "Núcleo Generalista", 159 testes).

---

## 1. Visão geral

| Item | Valor |
|---|---|
| Artefato | `target/thz-jvm25-2.3.0.jar` (shaded, ~1.1 MB, FlatLaf embutido) |
| Plataforma | Java 25 (sem flags — nenhum recurso preview em uso), Maven canônico |
| Testes | `mvn verify` — **30/30 verde** (ParidadeTest, GuiPaletaTest, InterpretadorTest, DecimalMonetarioTest, ContratosInvariantesTest) |

| Pacotes | `thz.lang.{lexico, sintatico, ast, semantico, runtime, interpretador, diagnostico*, formato, cli, repl, gui}` |
| Entradas | CLI `ThzCli` (`check/ast/fmt/run/repl/gui`) · GUI `ThzGui` · REPL multi-linha |

Ambiente (Windows/scoop): `JAVA_HOME` aponta para openjdk17 por padrão —
**sempre exportar antes do Maven:**
```powershell
$env:JAVA_HOME="C:\Users\lucas\scoop\apps\openjdk25\current"; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```

Execução:
```bat
java -jar target\thz-jvm25-2.3.0.jar check exemplos\faturamento.thz
java -jar target\thz-jvm25-2.3.0.jar run   exemplos\faturamento.thz
java -jar target\thz-jvm25-2.3.0.jar gui          :: IDE Swing
```
O manifesto embute `Enable-Native-Access: ALL-UNNAMED` → sem warnings de método
restrito do JDK 25 ao abrir a GUI (FlatLaf/NativeLibrary).

### Troubleshooting de IDE
* **"package thz.lang.* does not exist" no IntelliJ** — causa: dois sistemas de build
  ativos (`pom.xml` + `build.gradle.kts`) importados sobre a mesma raiz; o compilador
  da IDE recebe módulos parciais. Correção aplicada: Gradle desativado
  (`build.gradle.kts.desativado`, receita preservada) e reimport **somente** via pom.xml.
  AGENTS.md: Maven é o canônico.
* **"preview features not enabled" / flags manuais** — eliminado: o código usa apenas
  recursos finais do Java 21+; `--enable-preview` removido de pom (compiler+surefire)
  e dos comandos de execução.

---

## 2. Fases do motor — TODAS CONCLUÍDAS ✅

| Fase | Escopo | Arquivos-chave | Status |
|---|---|---|---|
| F0 | Fundações | `pom.xml` (release 25 + preview; surefire argLine; jar/shade mainClass `ThzCli`; shade finalName `thz-jvm25-*`), esqueleto 10 pacotes, docs | ✅ |
| F1 | Léxico | `TokenType` (63 tokens v2.3), `Token`, `CategoriaPalavra`, `PalavrasReservadas` (`VERSAO_LINGUAGEM_ATUAL="2.3.0"`, tabela Map.ofEntries), `ThzLexer` (port de `lexer.ts`) | ✅ |
| F2 | AST+Parser | `ExprAst` sealed (12 nós) / `ComandoAst` sealed (12 nós) / `ProgramaAst` + 9 records; `ThzParser` port integral (`textoCanonicoDe`) | ✅ |
| F3 | Runtime | `DecimalFixo` (BigInteger escalado, half-even), `Monetario` ISO4217×7, `DataThz/DataHoraThz` (algoritmos Hinnant), `ArenaMemoria` O(1) | ✅ |
| F4 | Semântica | `TipoThz/Tipos/CategoriaTipo`, `AnalisadorSemantico` (~720 linhas; SIG_STDLIB 28 assinaturas; contratos EXIGE/GARANTE quantificados ∀ sobre FATIA; lint estrito) | ✅ |
| F5 | Interpretador | `ValorThz` sealed (12 registros), `Escopo`, sinais `SinalRetorne/SinalFalhar`, `InterpretadorThz` (~1100 linhas, stdlib TEXTO×9/MATEMATICA×7/DATA×12) | ✅ |
| F6 | Tooling | `Formatador` canônico idempotente, `JsonEscritor` AST determinístico, `ThzCli` (check/ast/fmt --check/--escrever/--saida/run --principal/--arg/repl/gui), `Repl` (.ajuda/.codigo/.limpar/.sair) | ✅ |
| F7 | Paridade | JUnit golden: lexer/parser/decimal/data/exemplos canônicos/fmt idempotente | ✅ |

Correções pós-port aplicadas no motor:
* `ErroLexico` expõe `linha()/coluna()` (usado pelo realce em tempo real).
* `ThzLexer.tokenize()` descarta BOM UTF-8 (`U+FEFF`) inicial — arquivos salvos por
  Notepad/PowerShell (UTF-8 com BOM) agora lexam normalmente (teste `lexerToleraBomUtf8`).

Paridade preservada com TS (comportamento idêntico nos dois motores):
* `RESULTADO[T,E]` não tem acesso a campos (`r.sucesso` etc.) — apenas formatação
  `SUCESSO(...)/FALHA(...)` via `EXIBA`.
* Acesso indexado seguido de campo (`lote[i].campo`) não é suportado pelo parser;
  padrão canônico: variável temporária (`agenda[i]` em `agenda.thz`).

---

## 3. IDE Desktop Swing — evolução em ondas

### Onda 1 — Base funcional
`ThzGui` JFrame com editor `JTextArea`, botões Abrir/Salvar/Verificar/Executar/
Formatar/AST/Limpar, checkbox Estrito, painel de saída com diagnósticos caret,
SwingWorker para Executar, entrada `LER` via JOptionPane.

### Onda 2 — Realce de sintaxe zero-dependência
* `PaletaThz` — paletas ESCURO/CLARO; mapeia `TokenType`→`AttributeSet` usando
  `PalavrasReservadas.categoriaDe` como fonte da verdade + conjunto espelho dos
  tipos primitivos (Monarch). Comentários `#` pintados por scanner próprio
  (o lexer não emite token de comentário).
* `EditorThz` — `JTextPane`+`StyledDocument`; re-lex completo com debounce
  (`javax.swing.Timer` 300 ms); conversão linha:coluna→offset via
  `computeLineStarts()`; comprimento bruto correto p/ STRING e NUMERO (`_`);
  erro léxico colorido até o ponto da falha; undo/redo (`UndoManager`,
  Ctrl+Z/Y); auto-indent (+4 após abridores) e auto-fechamento de `"`;
  marcação de erros semânticos via `Highlighter`.

### Onda 3 — Visual polido (FlatLaf)
* Dependência `com.formdev:flatlaf:3.5.4` (shaded).
* `PaletaThz` estendida ao chrome: fundoJanela/Painel/Toolbar/Status, corBorda(Suave),
  corAcento(+Fg/Hover), texto Secundário/Título, fundoLinhaAtual.
* `EditorThz`: fonte premium autodetectada (JetBrains Mono→Cascadia→Consolas),
  highlight de linha atual, seleção/caret tematizados.
* `ThzGui`: header com brand ◆ THZ-LANG, pill do arquivo, toggles Tema/Estrito,
  toolbar roundRect com primário ▶ Executar e hover, cards arredondados,
  split pane estilizado, status bar (status + badge Estrito + Ln,Col + versão),
  menu Arquivo/Editar/Ver/Ações, atalhos Ctrl+O/S e F5.
* `aplicarTema()` troca `FlatDarkLaf⇄FlatLightLaf` + `updateComponentTreeUI` e
  re-tinta tudo em runtime.
* Manifesto `Enable-Native-Access: ALL-UNNAMED` no jar/shade (sem warnings).

### Onda 4 — Correção do gutter
Números de linha agora ancorados no documento: `StyledDocument.getDefaultRootElement()`
→ `modelToView2D(startOffset)` por elemento (fallback métrico antes do layout),
baseline centralizada, largura dinâmica por nº de dígitos, repaint sincronizado
a caret/documento. Antes havia desalinhamento acumulado (fonte/métrica própria ≠ View).

### Onda 5 — Ações de arquivo, execução parametrizada e consistência de tema
* **Ações completas de arquivo:** Novo Arquivo (`📄 Novo`, Ctrl+N) resetando estado e template; Salvar Como... (Ctrl+Shift+S); Salvar (Ctrl+S); Abrir (Ctrl+O).
* **Execução parametrizada e paridade com CLI:** injeção do lote demo (`LOTE`) com validação de invariantes para operações que recebem `FATIA[Estrutura]` (como `faturamento.thz` com `ProcessarVetorizado`); diálogo interativo via `JOptionPane` para parâmetros primitivos em `Principal` e operações.
* **Ciclo completo de temas (Escuro ⇄ Claro):** re-estilização em tempo real de `pillArquivo`, bordas dos cards, cabeçalhos, toolbar, status bar, toggles e botões secundários/primários sem reter resíduos visuais do tema anterior.

Testes GUI: `GuiPaletaTest` — cobertura completa de TokenType nas duas paletas + teste de ciclo de inicialização.

---


## 4. Coleção de exemplos + galeria na IDE

Pasta `exemplos/colecao/` — 10 programas autossuficientes, todos com
`check` ✓ e `run` ✓ (10 usa stdin):

| # | Programa | Construtos demonstrados |
|---|---|---|
| 01 | ola-mundo | mínimo executável, METADADOS, Principal |
| 02 | tipos-estruturas | primitivos, CRIAR, mutação de campo, INVARIANTE |
| 03 | enumeracoes | ENUMERACAO, comparações verbais, E |
| 04 | controle-fluxo | SE/SENAO aninhado, ENQUANTO, PARA..ATE PASSO |
| 05 | decimal-financeiro | DECIMAL exato, half-even (`1.005→1.00`, `1.015→1.02`), abs/min/max/raiz/potencia |
| 06 | texto-datas | stdlib TEXTO.* e DATA.* completa, indexação de resultado |
| 07 | resultado-ddd | RESULTADO[T,E], FALHAR_COM, EXIGE/GARANTE ∀ sobre FATIA |
| 08 | vetorizado-simd | LAYOUT_COLUNAR (SoA), PASSO_SIMD, acumulador DECIMAL(18,4) |
| 09 | bloco-memoria | USAR_BLOCO_MEMORIA, escopo arena, acesso canônico indexado |
| 10 | entrada-interativa | LER (stdin/GUI), NATURAL32, contagem |

Índice didático em `exemplos/README.md` (tabela por exemplo + convenções da linguagem).

**Galeria na IDE:** menu **Exemplos** (`montarMenuExemplos()`) lista seção
"Coleção de partida" + separador + "Canônicos (paridade TS ⇄ JVM)"
(faturamento/pedidos/agenda da raiz `exemplos/`). Rótulos amigáveis
(`01-ola-mundo`→`ola mundo`), tooltip com caminho absoluto; clique carrega no
editor com realce, limpa marcações e status; fallback gracioso sem a pasta.

**Regressão:** `ParidadeTest.galeriaExemplosValida()` — exige ≥10 `.thz` em
`exemplos/colecao`, valida lexer+parser+semântica e idempotência do fmt para cada um.

Descobertas registradas durante a criação (iguais no motor TS):
1. `GARANTE` sobre parâmetro FATIA quantifica sobre TODO elemento (∀) — pós-condição
   deve ser invariante de domínio, não estado final de itens não processados (ex. 07).
2. `lote[i].campo` requer temporária (`VARIAVEL l <- lote[i]`) — limitação do parser.
3. Campos `UUID` não são populáveis via literal `CRIAR` (TEXTO≠UUID; NULO≠UUID) —
   o lote canônico de `faturamento.thz` é injetado pela CLI (LOTE demo). Exemplo 08
   omite o campo e documenta o fato.
4. Chamada de OPERACAO dentro de PROCEDIMENTO é direta (`Op(p)`), sem qualificador
   da regra (`R.Op(p)` → "Chamada desconhecida").

---

## 5. Testes (inventário atual)

`src/test/java/thz/lang/` — **58 testes JUnit 5 (100% verdes)**:
* `ArenaMemoriaTest` (4): alocação linear contígua, descarte instantâneo em tempo constante $O(1)$, validação de limites e representação textual.
* `ConfiguracaoDesktopTest` (4): serialização e desserialização JSON, persistência atômica, histórico de arquivos recentes e tolerância a dados corrompidos/omitidos.
* `ContratosInvariantesTest` (4): validação de `EXIGE` (pré-condições), `GARANTE` (pós-condições), `INVARIANTE` em estruturas e invariantes pós-mutação.
* `DecimalMonetarioTest` (10): aritmética bancária half-even ISO/IEC 10967, precisão decimal sem float, multiplicação de monetário por decimal, conversão e parsing.
* `DetectorJvmTest` (5): detecção automática de JVMs no sistema (Scoop, SDKMAN, Program Files, JAVA_HOME), validação de executáveis java, inspeção de versão/vendor e persistência de caminho personalizado.
* `DocGenTest` (2): geração de documentação técnica em Markdown com diagramas de classe e fluxo Mermaid.js.
* `GovernancaTest` (4): matriz de rastreabilidade, relatórios Markdown e JSON, auditoria de conformidade e detecção de pendências.
* `GuiPaletaTest` (4): paletas ESCURO e CLARO cobrem todos os 63 `TokenType`, atributos de destaque, cores de fundo e ciclo de vida da interface Swing.
* `IdempotenciaTest` (5): idempotência de larga escala (Léxico, Sintático, `RegistroIdempotencia` em runtime O(1), Interpretador, Auditoria G4, THZ-IR G5, Formatador canônico e DocGen).
* `InterpretadorTest` (4): execução de operações com argumentos, controle de fluxo (SE, ENQUANTO, PARA), fatias e chamadas de funções stdlib.
* `IrSimdTest` (4): representação intermediária `thz-ir/1`, validação de regras SIMD R1 a R5, emissão de LLVM IR textual e detecção de I/O impuro.
* `ParidadeTest` (8): lexer básico, parser mínimo, datas Hinnant, exemplos canônicos (`faturamento.thz`, `pedidos.thz`), formatador idempotente, validação de todos os ≥11 exemplos da galeria e tolerância a BOM UTF-8.





Comando:
```powershell
$env:JAVA_HOME="C:\Users\lucas\scoop\apps\openjdk25\current"; mvn test
```

---

## 6. Problemas conhecidos / pendências

| Item | Situação |
|---|---|
| `pedidos.thz` com LOTE genérico da CLI falha ([Erro Decimal] 'PROD-SKU-901') | **Paridade:** falha idêntica no motor TS (o schema do LOTE demo é de faturamento). Uso correto: `--principal Classificar --arg pedido=...` ou GUI com entrada interativa / fatia literal |
| G4 Governança (`audit`) | **Concluído ✅:** Pacote `thz.lang.governanca` com `AuditorGovernanca` (matriz RASTREIO→Regra→Contrato, Markdown e JSON), CLI `thz audit` e botão `🛡️ Auditoria` na IDE Desktop |
| DocGen (`doc`) | **Concluído ✅:** Pacote `thz.lang.docgen` com `ThzDocGen` (Markdown + diagramas Mermaid de classes e fluxo), CLI `thz doc` e botão `📘 Doc` na IDE Desktop |
| G5 IR & SIMD (`ir`) | **Concluído ✅:** Pacotes `thz.lang.ir` e `thz.lang.simd` com `thz-ir/1`, validador SIMD regras R1–R5, emissão LLVM IR preliminar, CLI `thz ir` e botão `🧩 IR` na IDE Desktop |
| Empacotamento Autônomo (`jpackage`) | **Concluído ✅:** Scripts `scripts/build-package.ps1` e `scripts/build-package.cmd` geram a pasta portátil `dist/thz/` com executáveis `thz.exe`, `thz-gui.exe` e JRE 25 embutido |
| Compilação Nativa AOT (`GraalVM`) | **Configurado ✅:** Perfil `<profile id="native">` no `pom.xml` e scripts `scripts/build-native.ps1` / `scripts/build-native.cmd` para gerar `dist/bin/thz.exe` |
| Gradle | **Alternativa futura:** `build.gradle.kts.placeholder` pronto (código 100% desacoplado de APIs do Maven) |
| RESULTADO sem campos acessíveis | **Por Design:** Paridade estrita com o motor TS — candidate a evolução futura da gramática |
| UUID via CRIAR | **Por Design:** Idem acima; hoje instanciado via injeção CLI/GUI ou stdlib |

---

## 7. Próximos passos sugeridos (ordem de prioridade)

1. **LSP stdio & Tooling:**
   - Suporte ao protocolo Language Server Protocol (LSP stdio) para integração com editores modernos.




