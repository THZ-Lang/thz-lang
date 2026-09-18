# Matriz de Conformidade Técnica e Normas Internacionais — THZ-LANG (v2.4.0)

Este documento especifica a adesão estrita do **THZ-LANG** às normas internacionais de engenharia de software, aritmética independente, segurança, versionamento e governança de dados.

---

## 📌 Normas e Padrões Atendidos

| Norma / Padrão | Categoria | Escopo de Aplicação no THZ-LANG | Status |
| :--- | :--- | :--- | :---: |
| **ISO/IEC 10967** | Aritmética Independente de Linguagem | Aritmética decimal exata (`DECIMAL`) sem aproximação binária flutuante | ✅ CONFORME |
| **ISO 4217** | Códigos de Moedas Internacionais | Validação estrita de catálogo de moedas BACEN/G10/LATAM (`MONETARIO`) | ✅ CONFORME |
| **ISO/IEC/IEEE 42010**| Descrição de Arquitetura de Software | Estruturação do bloco `METADADOS_ARQUITETURA` e documentação viva | ✅ CONFORME |
| **ISO/IEC TR 24772** | Prevenção de Vulnerabilidades em Linguagens | Verificação de limites, alocação contígua em arena e segurança de memória | ✅ CONFORME |
| **BACEN Res. 4.893 & LGPD Art. 46** | Segurança Cibernética e Proteção de Dados | Criptografia AES-256-GCM e hash de senhas com PBKDF2 (310.000 iterações) | ✅ CONFORME |
| **RFC 3629 / ISO 10646** | Codificação e Escapes UTF-8 | Suporte nativo a UTF-8 puro e escapes hexadecimais Unicode `\uXXXX` | ✅ CONFORME |
| **RFC 4122** | Padrão Universally Unique Identifier | Formatação e parsing rigoroso de UUIDs v4 de 128-bits | ✅ CONFORME |
| **RFC 8259** | Formato de Intercâmbio de Dados JSON | Serialização e desserialização rigorosa de AST e relatórios de governança | ✅ CONFORME |
| **SemVer 2.0.0** | Versionamento Semântico | Parsing, validação e ordenação de versões de linguagem e programas | ✅ CONFORME |

---

## 1. ISO/IEC 10967 — Aritmética Decimal Exata

A norma **ISO/IEC 10967** (*Information technology — Language independent arithmetic*) define os requisitos para operações aritméticas numéricas sem perda de precisão.

### Implementação em THZ-LANG:
- Proibição estrita de literais de ponto flutuante IEEE 754 (`float` / `double`).
- Os tipos `DECIMAL(P, S)` utilizam inteiros de 64-bits / `BigInteger` escalados com representação decimal exata em runtime (`DecimalFixo`).
- Arredondamento contábil meio-par (*Half-Even / Banker's Rounding*) aplicado em divisões para neutralizar viés estatístico.

---

## 2. ISO 4217 — Códigos de Moedas Padrão (BACEN / G10 / LATAM)

A norma **ISO 4217** padroniza a representação de moedas através de códigos alfabéticos de 3 letras.

### Implementação em THZ-LANG:
- Validação estrita dos símbolos monetários do Banco Central do Brasil e moedas globais:
  - **2 Casas Decimais:** `BRL`, `USD`, `EUR`, `GBP`, `CHF`, `CAD`, `MXN`, `ARS`, `COP`, `PEN`, `UYU`, `CNY`, `AUD`, `NZD`, `INR`, `SGD`, `ZAR`, `SEK`, `NOK`, `DKK`.
  - **0 Casas Decimais:** `JPY`, `CLP`, `PYG`, `KRW`.
  - **3 Casas Decimais:** `KWD`, `BHD`, `OMR`, `JOD`.
- Proibição de operações aritméticas diretas entre moedas distintas sem conversão prévia explícita.

---

## 3. BACEN Res. 4.893 & LGPD Art. 46 — Segurança Criptográfica

Em conformidade com as diretrizes do **Banco Central do Brasil (BACEN)** para segurança cibernética em instituições financeiras e o Art. 46 da **LGPD**:
- **Criptografia Autenticada:** Cifra de alta performance `AES-256-GCM` com IV aleatório de 96 bits e tag de autenticação de 128 bits.
- **Derivação de Senhas:** Algoritmo `PBKDF2WithHmacSHA256` operando com **310.000 iterações** (padrão OWASP 2026/2027) com salt criptográfico de 16 bytes.
- **Prevenção de Timing Attacks:** Comparações criptográficas de hashes via `MessageDigest.isEqual` em tempo constante.

---

## 4. RFC 3629 / ISO 10646 — Codificação UTF-8 e Escapes Unicode

- **UTF-8 Nativo:** O compilador THZ-LANG rejeita sequências inválidas de bytes e consome transparentemente marcas de ordem de byte (BOM UTF-8 `U+FEFF`).
- **Escapes Padronizados:** Suporte completo a caracteres de escape de controle (`\n`, `\t`, `\r`, `\"`, `\\`) e escapes hexadecimais Unicode de 4 dígitos (`\u00A9` $\rightarrow$ `©`).

---

## 5. ISO/IEC/IEEE 42010 — Arquitetura Viva e Metadados

A norma **ISO/IEC/IEEE 42010** define os conceitos e termos para a descrição de arquiteturas de sistemas e software.
- O bloco `METADADOS_ARQUITETURA` (ou `ARCHITECTURE_METADATA` no dialeto `en-US`) é nativo na sintaxe e compõe a AST.
- Extração de diagramas C4 e documentação Markdown automatizada via CLI (`thz doc`).

---

## 6. ISO/IEC TR 24772 — Prevenção de Vulnerabilidades

O relatório técnico **ISO/IEC TR 24772** fornece orientação para evitar vulnerabilidades de segurança na especificação e uso de linguagens de programação.
- **Alocação de Memória Segura (`USAR_BLOCO_MEMORIA` / `USE_MEMORY_BLOCK`):** Prevenção de *buffer overflow* e *use-after-free* utilizando Arenas contíguas com verificação de limites.
- **Tratamento Seguro de Erros (`RESULTADO` / `RESULT`):** Prevenção de vazamento de estado e falhas não capturadas por meio do padrão explícito `SUCESSO` (`SUCCESS`) ou `ERRO` (`ERROR`).

---

## 7. RFC 4122 & RFC 8259 — UUID & JSON Standard

- **RFC 4122:** Geração determinística e validação de `UUID` v4 de 128-bits para transações de alta integridade.
- **RFC 8259:** Conformidade integral na emissão de ASTs, diagnósticos e relatórios de auditoria em JSON estruturado com codificação UTF-8 pura.
