# Manual de Diretrizes de Verificação, Validação e Qualidade (V&V) — THZ-LANG (v2.4.0)

Este manual estabelece as **Diretrizes Institucionais de Verificação, Validação e Conformidade Técnica (Checagem 1 a 1)** para todo o ciclo de vida de desenvolvimento do ecossistema **THZ-LANG**.

---

## 📌 Principais Objetivos do Ciclo de Vida V&V

1. **Integridade de Software:** Garantir que todo código Java, especificação e documentação cumpra sem exceções os requisitos das normas **ISO/IEC/IEEE**, **JSRs** e **RFCs**.
2. **Auditabilidade de Código:** Rastreabilidade completa entre requisitos (`RASTREIO_REQUISITO`), metadados de arquitetura (`METADADOS_ARQUITETURA`) e testes automatizados.
3. **Padrão Ouro de Engenharia:** Código limpo (*Clean Code*), sem comentários ruidosos, com indentação consistente (4 espaços em Java) e 100% de aprovação na suíte de testes.

---

## 📐 Checklist de Verificação 1 a 1 por Norma

### 1. ISO/IEC 10967 — Aritmética Exata
- [x] Nenhuma operação fiscal/monetária utiliza o tipo `float` ou `double` (IEEE 754 binário).
- [x] O tipo `DECIMAL(P, S)` utiliza inteiros escalados (`DecimalFixo` / `BigInteger`).
- [x] Divisões aplicam o método de arredondamento bancário meio-par (*Half-Even*).

### 2. ISO 4217 — Códigos de Moeda
- [x] Todos os valores monetários (`MONETARIO`) exigem tag ISO 4217 válida (ex: `BRL`, `USD`, `EUR`, `JPY`).
- [x] O compilador/analisador rejeita soma/subtração direta de valores em moedas distintas sem conversão explícita.

### 3. ISO/IEC/IEEE 42010 — Arquitetura Viva & Metadados
- [x] O bloco `METADADOS_ARQUITETURA` é mantido como nó de primeira classe na AST (`ProgramaAst`).
- [x] O utilitário `ThzDocGen` extrai diagramas C4 e Mermaid diretamente dos metadados.

### 4. ISO/IEC TR 24772 — Mitigação de Vulnerabilidades
- [x] Alocação contígua em arena (`USAR_BLOCO_MEMORIA`) com checagem rigorosa de limites.
- [x] Erros operacionais são representados de forma segura através do tipo `RESULTADO[T, E]`.

### 5. JSR 305 / JSR 380 — Anotações de Integridade em Java
- [x] Métodos públicos do núcleo realizam checagem de nulidade em parâmetros (`Objects.requireNonNull`).
- [x] Ausência de códigos `@version` ou `@author` ruidosos; histórico rastreado no Git.

### 6. RFC 4122 / RFC 8259 / SemVer 2.0.0 — Padrões Universais
- [x] UUIDs validados pelo padrão de 128-bits da RFC 4122.
- [x] Emissão de JSONs em UTF-8 estrito conforme a RFC 8259.
- [x] Parsing e comparação de versões seguindo SemVer 2.0.0.

---

## 🧪 Processo Obrigatório de Validação de PRs / Code Review

Cada alteração ou nova funcionalidade inserida no repositório **deve obrigatoriamente**:
1. Executar `./gradlew test` e obter 100% de aprovação.
2. Manter a árvore de trabalho limpa (`working tree clean`).
3. Passar na validação da suíte `ValidadorConformidadeNormasTest.java`.
