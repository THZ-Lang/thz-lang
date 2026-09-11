# CLI & Ecosystem Tooling — THZ-LANG Engine

The **THZ-LANG** command-line interface (`thz` / `thz.exe`) provides a unified developer toolchain for syntax validation, AST inspection, bytecode compilation, documentation generation, architectural Git auditing, and desktop IDE orchestration.

---

## Command Reference

### 1. Syntax & Contract Validation (`check`)
Validates lexical, grammatical, and contract declarations without executing:
```bash
./thz check exemplos/faturamento.thz
```

### 2. Execution Engine (`run`)
Executes THZ source files on the high-performance Java 25 JVM engine:
```bash
./thz run exemplos/faturamento.thz
```

### 3. Desktop IDE Interface (`gui`)
Launches the FlatLaf Swing visual studio with real-time syntax highlighting, AST explorer, and Monaco-style editor:
```bash
./thz gui
```

### 4. Interactive REPL (`repl`)
Starts an interactive evaluation session for quick prototyping:
```bash
./thz repl
```

### 5. Architectural Git Audit (`audit`)
Performs static analysis on Git diffs to detect uncontracted business rules or broken compliance invariants:
```bash
./thz audit --git
```

### 6. Bilingual PDF Manual Compilation (`livro` / `manual`)
Compiles all workspace markdown documents into high-grade bound PDF books:
```bash
# Compile both English and Portuguese editions
./thz livro

# Compile English edition only
./thz livro --en

# Compile Portuguese edition only
./thz livro --pt
```

### 7. Code Formatting & Dialect Translation (`fmt`)
Formats source code and translates canonica between Portuguese and English:
```bash
# Format source file in place
./thz fmt exemplos/faturamento.thz

# Transpile from pt-BR to en-US
./thz fmt exemplos/faturamento.thz --dialeto en-US
```
