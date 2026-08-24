# Matriz de Conformidade Técnica e Normas Internacionais — THZ-LANG (v2.4.0)

Este documento especifica a adesão estrita do **THZ-LANG** às normas internacionais de engenharia de software, aritmética independente, segurança, versionamento e governança de dados.

---

## 📌 Normas e Padrões Atendidos

| Norma / Padrão | Categoria | Escopo de Aplicação no THZ-LANG | Status |
| :--- | :--- | :--- | :---: |
| **ISO/IEC 10967** | Aritmética Independente de Linguagem | Aritmética decimal exata (`DECIMAL`) sem aproximação binária flutuante | ✅ CONFORME |
| **ISO 4217** | Códigos de Moedas Internacionais | Validação estrita de códigos alfa-3 em valores monetários (`MONETARIO(BRL)`) | ✅ CONFORME |
| **ISO/IEC/IEEE 42010**| Descrição de Arquitetura de Software | Estruturação do bloco `METADADOS_ARQUITETURA` e documentação viva | ✅ CONFORME |
| **ISO/IEC TR 24772** | Prevenção de Vulnerabilidades em Linguagens | Verificação de limites, alocação contígua em arena e segurança de memória | ✅ CONFORME |
| **JSR 305 / JSR 380** | Nulidade e Validação de Contratos (Java) | Anotações formais de integridade `@NotNull` e validação de contratos | ✅ CONFORME |
| **RFC 4122** | Padrão Universally Unique Identifier | Formatação e parsing rigoroso de UUIDs v4 | ✅ CONFORME |
| **RFC 8259** | Formato de Intercâmbio de Dados JSON | Serialização e desserialização rigorosa de AST e relatórios de governança | ✅ CONFORME |
| **SemVer 2.0.0** | Versionamento Semântico | Parsing, validação e ordenação de versões de linguagem e programas | ✅ CONFORME |

---

## 1. ISO/IEC 10967 — Aritmética Decimal Exata

A norma **ISO/IEC 10967** (*Information technology — Language independent arithmetic*) define os requisitos para operações aritméticas numéricas sem perda de precisão.

### Implementação em THZ-LANG:
- Proibição estrita de literais de ponto flutuante IEEE 754 (`float` / `double`).
- Os tipos `DECIMAL(P, S)` utilizam inteiros de 64-bits / `BigInteger` escalados com representação decimal exata em runtime (`DecimalFixo`).
- Arredondamento contábil meio-par (*Half-Even / Banker's Rounding*) aplicado em divisões para neutralizar viés estatístico.

```thz
// Aritmética 100% exata sem perda de precisão
VARIAVEL item_a: DECIMAL(12, 2) <- 0.10
VARIAVEL item_b: DECIMAL(12, 2) <- 0.20
VARIAVEL total: DECIMAL(12, 2)  <- item_a + item_b // Exatamente 0.30 (nunca 0.30000000000000004)
```

---

## 2. ISO 4217 — Códigos de Moedas Padrão

A norma **ISO 4217** padroniza a representação de moedas através de códigos alfabéticos de 3 letras.

### Implementação em THZ-LANG:
- Validação estrita dos símbolos monetários (`BRL`, `USD`, `EUR`, `JPY`, `GBP`, `CHF`, etc.).
- Proibição de operações aritméticas diretas entre moedas distintas sem conversão prévia.

```thz
VARIAVEL conta_br: MONETARIO(BRL) <- 1000.00 BRL
VARIAVEL conta_us: MONETARIO(USD) <- 500.00 USD

// Compilador / Analisador semântico rejeita adição direta de BRL com USD
```

---

## 3. ISO/IEC/IEEE 42010 — Arquitetura Viva e Metadados

A norma **ISO/IEC/IEEE 42010** define os conceitos e termos para a descrição de arquiteturas de sistemas e software.

### Implementação em THZ-LANG:
- O bloco `METADADOS_ARQUITETURA` é nativo na sintaxe e compõe a Árvore Sintática Abstrata (`ProgramaAst`).
- Extração de diagramas C4 e documentação Markdown automatizada via CLI (`thz doc`).

```thz
METADADOS_ARQUITETURA
    SISTEMA: "FaturamentoCore"
    MODULO: "MotorCalculo"
    DOMINIO: "Financeiro"
    SLO_LATENCIA_MS: 50
    CRITICIDADE: "ALTA"
FIM_METADADOS
```

---

## 4. ISO/IEC TR 24772 — Prevenção de Vulnerabilidades

O relatório técnico **ISO/IEC TR 24772** fornece orientação para evitar vulnerabilidades de segurança na especificação e uso de linguagens de programação.

### Implementação em THZ-LANG:
- **Alocação de Memória Segura (`USAR_BLOCO_MEMORIA`):** Prevenção de *buffer overflow* e *use-after-free* utilizando Arenas contíguas com verificação de limites.
- **Tratamento Seguro de Erros (`RESULTADO`):** Prevenção de vazamento de estado e falhas não capturadas por meio do padrão explícito `SUCESSO` ou `ERRO`.

---

## 5. RFC 4122 & RFC 8259 — UUID & JSON Standard

- **RFC 4122:** Geração determinística e validação de `UUID` de 128-bits para transações de alta integridade.
- **RFC 8259:** Conformidade integral na emissão de ASTs, diagnósticos e relatórios de auditoria em JSON estruturado com codificação UTF-8 pura.

---

## 6. JSR 305 / JSR 380 — Integridade & Contratos em Java

No motor JVM (`thz-core-jvm` e submódulos):
- **JSR 305 (Annotations for Software Defect Detection):** Documentação de nulidade em parâmetros e métodos.
- **Clean Code & Javadoc:** 100% dos membros públicos documentados em Javadoc com marcas de parâmetro `@param`, retorno `@return` e exceção `@throws`.
