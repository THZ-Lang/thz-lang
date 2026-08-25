# Technical Compliance & International Standards Matrix — THZ-LANG (v2.4.0)

This document formalizes the strict compliance of the **THZ-LANG** programming language with global standards for software engineering, language-independent arithmetic, cryptographic security, semantic versioning, and architectural data governance.

---

## 📌 Standard Compliance Matrix

| Standard / Authority | Category | Scope in THZ-LANG Engine | Compliance Status |
| :--- | :--- | :--- | :---: |
| **ISO/IEC 10967 (LIA-1)** | Language Independent Arithmetic | Exact scaled decimal arithmetic (`DECIMAL`) with zero binary float drift | ✅ FULL COMPLIANCE |
| **ISO 4217** | International Currency Codes | Strict validation of alpha-3 currency codes and cross-currency protection | ✅ FULL COMPLIANCE |
| **ISO/IEC/IEEE 42010**| Systems & Software Architecture | Structured `ARCHITECTURE_METADATA` AST node and living documentation | ✅ FULL COMPLIANCE |
| **ISO/IEC TR 24772** | Language Vulnerability Mitigation | Memory bounds checking, contiguous ephemeral arenas, and state safety | ✅ FULL COMPLIANCE |
| **BACEN Res. 4893 & LGPD Art. 46** | Cybersecurity & Data Protection | AES-256-GCM authenticated cipher and PBKDF2 password derivation (310,000 iterations) | ✅ FULL COMPLIANCE |
| **RFC 3629 / ISO 10646** | UTF-8 Encoding & Unicode Escapes | Native UTF-8 parsing, BOM consumption, and 4-digit hexadecimal escapes (`\uXXXX`) | ✅ FULL COMPLIANCE |
| **RFC 4122** | Universally Unique Identifiers (UUID) | Deterministic generation and validation of 128-bit UUID v4 | ✅ FULL COMPLIANCE |
| **RFC 8259** | JSON Data Interchange Format | RFC-compliant UTF-8 AST emission, compiler diagnostics, and audit outputs | ✅ FULL COMPLIANCE |
| **SemVer 2.0.0** | Semantic Versioning Specification | Deterministic parsing, comparison, and precedence ordering | ✅ FULL COMPLIANCE |

---

## 1. ISO/IEC 10967 — Exact Decimal Arithmetic

The **ISO/IEC 10967** standard (*Information technology — Language independent arithmetic*) specifies integer and floating arithmetic requirements.

### THZ-LANG Adherence:
- **Zero IEEE 754 Drift:** Monetary and decimal arithmetic strictly prohibits IEEE 754 binary floating-point representation (`float` / `double`).
- **Half-Even Banker's Rounding:** Division operations apply round-to-nearest-even to eliminate statistical accumulation bias.
- **Hardware Scaled Integers:** Codegen leverages 64-bit and 128-bit integers (`i128` in LLVM AOT and `DecimalFixed` in JVM) guaranteeing exact results across all platforms.

---

## 2. ISO 4217 — Currency Code Safety (BACEN / G10 / LATAM)

- **Currencies with 2 Decimals:** `BRL`, `USD`, `EUR`, `GBP`, `CHF`, `CAD`, `MXN`, `ARS`, `COP`, `PEN`, `UYU`, `CNY`, `AUD`, `NZD`, `INR`, `SGD`, `ZAR`, `SEK`, `NOK`, `DKK`.
- **Currencies with 0 Decimals:** `JPY`, `CLP`, `PYG`, `KRW`.
- **Currencies with 3 Decimals:** `KWD`, `BHD`, `OMR`, `JOD`.
- **Cross-Currency Safety:** Adding or subtracting different currencies (e.g. `BRL` and `USD`) is rejected at compile time without explicit rate conversion.

---

## 3. BACEN Resolution 4,893 & LGPD Art. 46 — Cryptographic Hardening

- **AES-256-GCM:** 256-bit Galois/Counter Mode authenticated encryption with random 96-bit IVs and 128-bit authentication tags.
- **PBKDF2 Password Hashing:** 310,000 iterations of HMAC-SHA256 with 16-byte random salts.
- **Constant-Time Verification:** All cryptographic comparisons use `MessageDigest.isEqual` to prevent side-channel timing attacks.
