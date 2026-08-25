# Self-Hosting Compiler Specification — THZ-LANG

This document outlines the self-hosting compiler written in THZ-LANG itself (`compilador/`).

---

## 1. Compiler Architecture

- **`tokens.thz` & `lexer.thz`:** Lexical scanner generating token streams.
- **`ast.thz` & `parser.thz`:** Recursive descent parser generating AST nodes.
- **`codegen.thz` & `driver.thz`:** Code generation targeting LLVM IR and executable linking.
