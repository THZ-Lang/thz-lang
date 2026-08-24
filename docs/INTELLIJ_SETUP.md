# Configuração do THZ-LANG no IntelliJ IDEA e IDEs JetBrains

O IntelliJ IDEA e as demais IDEs da JetBrains suportam gramáticas **TextMate** nativamente através do plugin integrado **TextMate Bundles**.

---

## 🚀 Como Ativar o Realce de Sintaxe `.thz` e `.thzui`

1. **Abra as Configurações do IntelliJ IDEA:**
   * No Windows/Linux: `File` → `Settings` (ou `Ctrl + Alt + S`).
   * No macOS: `IntelliJ IDEA` → `Settings` (ou `Cmd + ,`).

2. **Navegue até TextMate Bundles:**
   * Vá em: `Editor` → `TextMate Bundles`.

3. **Adicione a pasta da extensão THZ:**
   * Clique no botão `+` (Add).
   * Selecione o diretório da extensão no repositório:
     ```
     Extensions/thz-lsp-vscode/
     ```
   * O IntelliJ detectará automaticamente:
     - Gramática: `syntaxes/thz.tmLanguage.json` (`source.thz`)
     - Extensões de arquivo associadas: `*.thz`, `*.thzui`
     - Configuração de comentários e delimitadores: `language-configuration.json`

4. **Aplicar e Salvar:**
   * Clique em `Apply` e depois em `OK`.

---

## 🎨 Cores e Escopos Suportados

A gramática mapeia os seguintes escopos padrão:
* `keyword.control.thz`: Palavras-chave estruturais (`PROGRAMA`, `ESTRUTURA`, `REGRA_NEGOCIO`, `PROCEDIMENTO`, `PIPELINE_DADOS`, `FONTE_ENTRADA`, `DESTINO_SAIDA`, `TRANSFORMACAO`, `VETORIZAR_PARA`, `EXIGE`, `GARANTE`, `INVARIANTE`, etc.).
* `keyword.operator.logical.thz`: Conectivos lógicos (`E`, `OU`, `NAO`).
* `constant.language.thz`: Literais lógicos (`VERDADEIRO`, `FALSO`, `NULO`).
* `entity.name.type.thz`: Tipos nativos (`DECIMAL`, `MONETARIO`, `FATIA`, `TEXTO`, `UUID`, `DATA`, `DATA_HORA`, `INTEIRO`, `LOGICO`, etc.).
* `constant.numeric.thz`: Inteiros e decimais de precisão exata.
* `comment.line.number-sign.thz`: Comentários de linha iniciados por `#` ou `//`.
* `string.quoted.double.thz`: Strings literais entre aspas com suporte a sequências de escape.
