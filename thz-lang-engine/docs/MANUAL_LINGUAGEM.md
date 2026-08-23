# Manual da Linguagem THZ-LANG (v2.3)
## Guia Oficial, Referência de Sintaxe, Arquitetura Viva e Governança

---

## 1. Visão Geral e Filosofia

O **THZ-LANG** (`.thz`) é uma Linguagem Corporativa de Sistemas voltada para **Governança de Negócio**, **Arquitetura Viva**, **Precisão Fiscal/Monetária** e **Processamento de Alta Performance** , **Programação Generalistica** também é possivel.

### Princípios Fundamentais:
1. **Sintaxe em Português:** O código é legível por engenheiros de software, analistas de negócio, auditores de compliance e órgãos reguladores sem necessidade de tradução de termos de domínio.
2. **Design by Contract (DbC) Rigoroso:** Toda operação corporativa pode declarar pré-condições (`EXIGE`), pós-condições (`GARANTE`) e invariantes (`INVARIANTE`) validados pelo compilador/interpretador.
3. **Aritmética Financeira Determinística (ISO/IEC 10967):** É terminantemente proibido o uso de ponto flutuante binário IEEE 754 (`float`/`double`) para dinheiro. Valores monetários e fiscais usam inteiros escalados com precisão arbitrária (`DecimalFixo`/`Monetario`).
4. **Gerenciamento de Memória em $O(1)$:** Processamentos em lote e escopos temporários utilizam alocação linear contígua em arena (`ArenaMemoria`), liberando toda a memória de uma vez instantaneamente em tempo constante, sem pausas de Garbage Collector.
5. **Interface Gráfica Declarativa Desktop:** Geração automática e moderna de formulários visuais com validação contratual integrada através do módulo `TELA`.

---

## 2. Sintaxe Básica e o Operador de Atribuição `<-`

### O Operador de Atribuição `<-`
No THZ-LANG, a atribuição de valores utiliza a seta para a esquerda: **`<-`**.

```thz
DESTINO <- ORIGEM
```

#### Por que `<-` e não `=` ou `-->`?
* **Direção Real do Fluxo de Dados:** O valor da expressão à direita flui para dentro do identificador/lugar de memória à esquerda.
* **Semântica Clássica Formal:** Segue o padrão ouro de algoritmos formais, **R**, **Portugol** e linguagens matemáticas.
* **Clareza com Operadores de Comparação:** Elimina a confusão comum entre `=` (atribuição) e `=` (comparação de igualdade).

### Declaração de Variáveis
Para declarar uma variável, utilize a palavra-chave `VARIAVEL`, seguida do nome, `:`, do tipo e opcionalmente do valor inicial com `<-`:

```thz
# Declaração com inicialização
VARIAVEL nomeUsuario : TEXTO <- "Lucas Thomaz"
VARIAVEL idade : INTEIRO32 <- 28
VARIAVEL saldo : DECIMAL(12, 2) <- 1500.50
VARIAVEL ativo : LOGICO <- VERDADEIRO

# Declaração sem valor inicial (assume valor padrão do tipo)
VARIAVEL totalAcumulado : DECIMAL(14, 2)
```

### Reatribuição de Valores
Para atualizar o valor de uma variável já declarada:

```thz
saldo <- saldo + 250.00
ativo <- FALSO
nomeUsuario <- "Novo Nome"
```

### Atribuição em Membros de Estruturas e Fatias
```thz
# Atualiza campo de um registro
cliente.saldo <- 3200.00

# Atualiza elemento de uma fatia indexada (índices iniciam em 0)
minhaLista[0] <- "Primeiro Item Atualizado"
```

---

## 3. Operadores da Linguagem

### Operadores Aritméticos
| Operador | Significado | Exemplo |
| :---: | :--- | :--- |
| `+` | Adição / Concatenação de Texto | `a + b`, `"Olá, " + nome` |
| `-` | Subtração / Negação Unária | `a - b`, `-valor` |
| `*` | Multiplicação | `quantidade * precoUnitario` |
| `/` | Divisão Decimal ou Inteira | `total / parcelas` |
| `%` | Módulo (Resto da divisão inteira) | `numero % 2` |

### Operadores Relacionais (Comparações)
| Operador | Significado | Exemplo |
| :---: | :--- | :--- |
| `=` | Igual a | `status = "APROVADO"` |
| `!=` | Diferente de | `saldo != 0.00` |
| `<` | Menor que | `idade < 18` |
| `<=` | Menor ou igual a | `pontuacao <= 100` |
| `>` | Maior que | `salario > 2500.00` |
| `>=` | Maior ou igual a | `TEXTO.comprimento(senha) >= 8` |

### Operadores Lógicos Verbais
No THZ-LANG, operadores lógicos são **palavras em português** para leitura fluida:

| Operador | Significado | Exemplo |
| :---: | :--- | :--- |
| `E` | Conjunção lógica (AND) | `ativo = VERDADEIRO E saldo > 0.00` |
| `OU` | Disjunção lógica (OR) | `tipo = "ADMIN" OU nivel >= 5` |
| `NAO` | Negação lógica (NOT) | `NAO bloqueado` |

---

## 4. Sistema de Tipagem Estática

### Tipos Primitivos
* **`TEXTO`**: Cadeias de caracteres delimitadas por aspas duplas `""` (ex: `"Serviço Ativo"`). Suporta escapes como `\n`, `\t`, `\"`.
* **`LOGICO`**: Valores booleanos `VERDADEIRO` ou `FALSO`.
* **`INTEIRO` / `INTEIRO32` / `INTEIRO64`**: Inteiros de precisão arbitrária ou delimitados (ex: `100`, `-42`).
* **`NATURAL`**: Inteiros estritamente não-negativos ($\ge 0$).

### Aritmética Decimal e Monetária (Ponto Fixo sem Float)

O THZ-LANG **proíbe ponto flutuante binário (`float`/`double`)** para evitar o clássico bug de arredondamento IEEE 754 (onde `0.1 + 0.2 = 0.30000000000000004`). Em seu lugar, utiliza aritmética decimal exata:

#### Entendendo `DECIMAL(Precisão, Escala)`:
A notação vem do padrão internacional SQL (ISO/IEC 9075):
* **Precisão (1º número):** Total de dígitos permitidos no número (inteiros + decimais).
* **Escala (2º número):** Quantidade de dígitos que ficam estritamente **depois da vírgula** (casas decimais).

| Declaração | Dígitos Totais | Casas Decimais | Limite Inteiro | Exemplos de Uso |
| :--- | :---: | :---: | :--- | :--- |
| `DECIMAL(12, 2)` | 12 | 2 | Até 10 dígitos inteiros | Valores financeiros até `R$ 9.999.999.999,99` (quase 10 bilhões) |
| `DECIMAL(5, 2)` | 5 | 2 | Até 3 dígitos inteiros | Percentuais e margens de `-999,99%` a `+999,99%` |
| `DECIMAL` *(Padrão)* | Generosa | Automática | Sem limite restrito | Uso cotidiano geral sem preocupação com tamanhos de campos |

```thz
# 1. Uso simplificado no dia a dia (sem parênteses)
VARIAVEL precoCusto : DECIMAL <- 199.90
VARIAVEL margemLucro : DECIMAL <- 35.00

# 2. Uso estrito com limites regulatórios / bancários
VARIAVEL saldoBancario : DECIMAL(14, 2) <- 15000000.00
VARIAVEL aliquotaImposto : DECIMAL(5, 4) <- 0.0925 # 9,25% com 4 casas

# 3. Monetário vinculado a moeda ISO-4217
VARIAVEL saldoCarteira : MONETARIO(BRL) <- 2500.00
```

### Tipos Temporais (ISO-8601)
* **`DATA`**: Data civil no formato `AAAA-MM-DD` (ex: `DATA.criar(2026, 8, 23)` ou `DATA.hoje()`).
* **`DATA_HORA`**: Data e hora no formato ISO-8601 (ex: `DATA.agora()`).

### Fatias (Arrays Dinâmicos / Listas)
* **`FATIA[T]`**: Coleção homogênea e dinâmica do tipo `T`.
* Literais de fatia são delimitados por colchetes `[` e `]`:

```thz
VARIAVEL nomes : FATIA[TEXTO] <- ["Alice", "Bob", "Carlos"]
VARIAVEL primeiro : TEXTO <- nomes[0]
```

### Enumerações (`ENUMERACAO`)
Declaram um conjunto fechado e estrito de constantes nomeadas:

```thz
ENUMERACAO AmbienteExecucao
    DESENVOLVIMENTO
    HOMOLOGACAO
    PRODUCAO
FIM_ENUMERACAO

VARIAVEL env : AmbienteExecucao <- PRODUCAO
```

---

## 5. Estruturas de Dados (`ESTRUTURA`)

As estruturas agrupam campos tipados em registros coesos:

```thz
ESTRUTURA ParametroServico
    chave: TEXTO
    valor: TEXTO
    descricao: TEXTO
FIM_ESTRUTURA

ESTRUTURA Usuario
    id: TEXTO
    nome: TEXTO
    ativo: LOGICO
    saldo: DECIMAL(12, 2)
    INVARIANTE saldo >= 0.00
FIM_ESTRUTURA
```

### Instanciação com `CRIAR`
A instanciação de estruturas é canônica e explícita, nomeando cada campo:

```thz
VARIAVEL usr : Usuario <- CRIAR Usuario(
    id: "USR-001",
    nome: "Lucas Thomaz",
    ativo: VERDADEIRO,
    saldo: 500.00
)
```

### Layout Colunar para Alta Performance (SIMD)
Para vetores de alto rendimento com aceleração AVX2/AVX-512:

```thz
ESTRUTURA ItemFaturamento LAYOUT_COLUNAR
    idItem: INTEIRO32
    quantidade: INTEIRO32
    precoUnitario: DECIMAL(12, 2)
    valorTotal: DECIMAL(14, 2)
FIM_ESTRUTURA
```

---

## 6. Controle de Fluxo e Laços

### Condicional `SE ... ENTAO ... SENAO`
```thz
SE saldo >= valorSaque ENTAO
    saldo <- saldo - valorSaque
    EXIBA "Saque realizado com sucesso!"
SENAO
    EXIBA "Saldo insuficiente!"
FIM_SE
```

### Laço `ENQUANTO`
```thz
VARIAVEL contador : INTEIRO32 <- 1
ENQUANTO contador <= 5 FACA
    EXIBA "Passo: " + contador
    contador <- contador + 1
FIM_ENQUANTO
```

### Laço por Intervalo `PARA`
```thz
PARA i DE 1 ATE 10 COM PASSO 2 FACA
    EXIBA "Índice: " + i
FIM_PARA
```

### Laço Vetorizado SIMD `VETORIZAR_PARA`
Executa transformações de alto rendimento sobre estruturas de layout colunar:

```thz
VETORIZAR_PARA i EM itens COM PASSO_SIMD 8 FACA
    itens.valorTotal[i] <- itens.quantidade[i] * itens.precoUnitario[i]
FIM_VETORIZAR
```

---

## 7. Governança e Design by Contract (DbC)

### Metadados Arquiteturais (`METADADOS_ARQUITETURA`)
Obrigatório em todo programa corporativo:

```thz
METADADOS_ARQUITETURA
    DOMINIO: "FaturamentoCorporativo"
    SUBDOMINIO: "ProcessamentoLote"
    CAMADA: "Servico"
    VERSAO: "2.3.0"
    AUTOR: "Lucas Thomaz"
    SLO_LATENCIA_MAXIMA: "15ms"
    CONFORMIDADE: "SOX", "PCI-DSS", "ISO-42010"
FIM_METADADOS
```

### Regras de Negócio e Operações
As regras encapsulam lógica e validam contratos formais:

```thz
REGRA_NEGOCIO MotorCredito
    IDENTIFICADOR_REGRA: "RN-CRED-010"
    RASTREIO_REQUISITO: "REQ-FIN-089"
    DESCRICAO: "Analisa e aprova limite de crédito baseado em score e renda"

    # Pré-condições obrigatórias
    CONTRATO_ENTRADA
        EXIGE scoreSerasa >= 0 E scoreSerasa <= 1000
        EXIGE rendaMensal > 0.00
    FIM_CONTRATO_ENTRADA

    # Pós-condições garantidas na saída (RESULTADO)
    CONTRATO_SAIDA
        GARANTE RESULTADO >= 0.00
    FIM_CONTRATO_SAIDA

    OPERACAO CalcularLimite(scoreSerasa: INTEIRO32, rendaMensal: DECIMAL(12, 2)): DECIMAL(12, 2)
    INICIO
        SE scoreSerasa < 400 ENTAO
            RETORNE 0.00
        SENAO
            RETORNE rendaMensal * 0.40
        FIM_SE
    FIM
FIM_REGRA_NEGOCIO
```

---

## 8. Gerenciamento de Memória em Bloco (`USAR_BLOCO_MEMORIA`)

Permite alocar e descartar recursos efêmeros sem gerar lixo no Heap:

```thz
USAR_BLOCO_MEMORIA(tamanhoBytes: 65536)
    # Toda a alocação criada aqui é descartada em tempo O(1) ao sair do bloco
    VARIAVEL temp : FATIA[INTEIRO32] <- [1, 2, 3, 4, 5]
    EXIBA "Processado em memória contígua isolada"
FIM_BLOCO_MEMORIA
```

---

## 9. Biblioteca Padrão (Stdlib)

### Módulo `TEXTO`
* `TEXTO.comprimento(t: TEXTO): INTEIRO32`
* `TEXTO.maiusculo(t: TEXTO): TEXTO`
* `TEXTO.minusculo(t: TEXTO): TEXTO`
* `TEXTO.aparar(t: TEXTO): TEXTO` (remove espaços nas extremidades)
* `TEXTO.subtexto(t: TEXTO, inicio: INTEIRO32, fim: INTEIRO32): TEXTO`
* `TEXTO.contem(t: TEXTO, trecho: TEXTO): LOGICO`
* `TEXTO.substituir(t: TEXTO, de: TEXTO, para: TEXTO): TEXTO`
* `TEXTO.dividir(t: TEXTO, separador: TEXTO): FATIA[TEXTO]`
* `TEXTO.juntar(fatia: FATIA[TEXTO], separador: TEXTO): TEXTO`

### Módulo `MATEMATICA`
* `MATEMATICA.absoluto(n)`
* `MATEMATICA.minimo(a, b)`
* `MATEMATICA.maximo(a, b)`
* `MATEMATICA.potencia(base, expoente)`
* `MATEMATICA.raizQuadrada(n)`
* `MATEMATICA.piso(decimal)` / `MATEMATICA.teto(decimal)` / `MATEMATICA.arredondar(decimal, casas)`

### Módulo `DATA`
* `DATA.criar(ano, mes, dia): DATA`
* `DATA.hoje(): DATA`
* `DATA.agora(): DATA_HORA`
* `DATA.ano(d)`, `DATA.mes(d)`, `DATA.dia(d)`
* `DATA.somarDias(d: DATA, dias: INTEIRO32): DATA`
* `DATA.diferencaDias(d1: DATA, d2: DATA): INTEIRO32`
* `DATA.texto(d): TEXTO`

### Módulo `FATIA`
* `FATIA.tamanho(f: FATIA[T]): INTEIRO32`
* `FATIA.adicionar(f: FATIA[T], elemento: T): FATIA[T]`
* `FATIA.remover(f: FATIA[T], indice: INTEIRO32): FATIA[T]`

### Módulo `DOCUMENTO` (Exportação Corporativa em PDF, XLSX e DOCX)
Permite gerar documentos corporativos oficiais diretamente a partir de registros e tabelas:
* `DOCUMENTO.exportarPdf(caminho: TEXTO, titulo: TEXTO, dados: REGISTRO | FATIA): TEXTO`  
  Gera relatório corporativo em PDF com layout institucional, paginação e tabelas zebradas.
* `DOCUMENTO.exportarXlsx(caminho: TEXTO, nomePlanilha: TEXTO, dados: REGISTRO | FATIA): TEXTO`  
  Gera planilha Excel (.xlsx) com cabeçalhos estilizados, linhas zebradas e colunas auto-dimensionadas.
* `DOCUMENTO.exportarDocx(caminho: TEXTO, titulo: TEXTO, dados: REGISTRO | FATIA): TEXTO`  
  Gera documento Word (.docx) com hierarquia de títulos, seções e tabelas formatadas.
* `DOCUMENTO.exportar(caminho: TEXTO, titulo: TEXTO, dados: REGISTRO | FATIA): TEXTO`  
  Detecta automaticamente o formato pela extensão do arquivo (`.pdf`, `.xlsx`, `.docx`).

### Entrada e Saída
* `EXIBA valor1, valor2, ...`: Exibe valores na saída padrão / console com quebra de linha.
* `LER(): TEXTO`: Lê uma linha de texto da entrada padrão.
* `RETORNE expressao`: Retorna o valor de uma `OPERACAO` ou encerra um `PROCEDIMENTO`.

---

## 10. Módulo de Interface Gráfica Desktop (`TELA`)

O THZ-LANG possui um subsistema gráfico nativo Swing que renderiza formulários automaticamente a partir de registros:

### Renderização Automática de Formulários
```thz
# 1. Cria o registro que representa a tela
VARIAVEL form : MeuFormulario <- CRIAR MeuFormulario(...)

# 2. Renderiza a interface e vincula o clique do botão à Operação de Negócio
VARIAVEL status : TEXTO <- TELA.renderizarFormulario(form, "RegraNegocio.MinhaOperacao")
```

### Mapeamento Automático de Widgets:
* **Senhas (`senha`, `token`, `secret`):** Renderiza `JPasswordField` com botão "Ver/Ocultar".
* **Cores (`corHex`, `paleta`, `tema`):** Renderiza amostra visual e seletor `JColorChooser`.
* **Arquivos/Caminhos (`caminho`, `arquivo`, `pasta`):** Renderiza campo com botão `JFileChooser` ("Procurar...").
* **Sliders (`slider`, `prioridade`, `nivel`, `escala`):** Renderiza `JSlider` com percentual numérico.
* **Spinners (`spinner`, `quantidade`, `conexoes`):** Renderiza `JSpinner` com botões de incremento.
* **Radios (`AmbienteExecucao` com $\le 4$ membros):** Renderiza grupo de `JRadioButton`.
* **Combos (Enumerações gerais):** Renderiza `JComboBox` suspensa.
* **Switches e Checkboxes (`LOGICO`):** Renderiza botões toggle modernos ou caixas de seleção.
* **Grades de Dados (`FATIA[Estrutura]`):** Renderiza `JTable` dinâmica com botões "+ Linha" e "- Remover".
* **Listas de Seleção Múltipla (`FATIA[TEXTO]`):** Renderiza `JList` com seleção múltipla.
* **Áreas de Texto Longo (`detalhes`, `parecer`, `notas`):** Renderiza `JTextArea` rolável com quebra de linha automática.

### Diálogos Modais Rápidos:
* `TELA.alerta(titulo: TEXTO, mensagem: TEXTO): TEXTO`
* `TELA.confirmar(titulo: TEXTO, mensagem: TEXTO): LOGICO`
* `TELA.pedirTexto(titulo: TEXTO, prompt: TEXTO): TEXTO`

---

## 11. Guia do Tooling e Linha de Comando (CLI)

### Comandos do CLI (`thz` / `thz.exe` / `thz-jvm.jar`):

```bash
# 1. Verificar sintaxe, semântica e contratos estritos
thz check caminho/arquivo.thz

# 2. Executar programa
thz run caminho/arquivo.thz

# 3. Formatar código canonicamente (idempotente)
thz fmt caminho/arquivo.thz --escrever

# 4. Auditoria de Governança e Matriz de Rastreabilidade (G4)
thz audit caminho/arquivo.thz

# 5. Emitir THZ-IR e código LLVM (G5)
thz ir caminho/arquivo.thz --llvm

# 6. Gerar documentação técnica em Markdown + Mermaid
thz doc caminho/arquivo.thz

# 7. Iniciar console REPL interativo multi-linha
thz repl

# 8. Iniciar Ambiente de Desenvolvimento Integrado Gráfico (IDE Desktop)
thz gui
```

### Atalhos da IDE THZ-LANG Desktop:
* `Ctrl + N`: Novo arquivo
* `Ctrl + O`: Abrir arquivo `.thz`
* `Ctrl + S`: Salvar arquivo
* `Ctrl + Shift + S`: Salvar Como
* `F7`: Verificar sintaxe e semântica
* `F5`: Executar `Principal()` / primeira `OPERACAO`
* `Ctrl + Z` / `Ctrl + Y`: Desfazer / Refazer
* `☀ Claro / ☾ Escuro`: Alternar tema visual instantaneamente

---

## 12. Exemplo Canônico Completo de Ponta a Ponta

```thz
VERSAO_LINGUAGEM "2.3"
PROGRAMA ExemploCompleto

# --- METADADOS ARQUITETURAIS ---
METADADOS_ARQUITETURA
    DOMINIO: "Logistica"
    SUBDOMINIO: "GestaoFrotas"
    CAMADA: "Servico"
    VERSAO: "2.3.0"
    AUTOR: "Lucas Thomaz"
    SLO_LATENCIA_MAXIMA: "20ms"
    CONFORMIDADE: "ISO-9001", "ISO-27001"
FIM_METADADOS

# --- ENUMERAÇÃO ---
ENUMERACAO StatusVeiculo
    DISPONIVEL
    EM_TRANSITO
    MANUTENCAO
FIM_ENUMERACAO

# --- ESTRUTURA ---
ESTRUTURA Veiculo
    placa: TEXTO
    capacidadeCargaKg: INTEIRO32
    custoKmRodado: DECIMAL(10, 2)
    status: StatusVeiculo
    INVARIANTE capacidadeCargaKg > 0
FIM_ESTRUTURA

# --- REGRA DE NEGÓCIO COM CONTRATOS ---
REGRA_NEGOCIO DespachoCarga
    IDENTIFICADOR_REGRA: "RN-LOG-100"
    RASTREIO_REQUISITO: "REQ-LOG-042"
    DESCRICAO: "Calcula custo estimado de viagem e valida capacidade de carga"

    CONTRATO_ENTRADA
        EXIGE distanciaKm > 0.00
        EXIGE pesoCargaKg > 0
    FIM_CONTRATO_ENTRADA

    CONTRATO_SAIDA
        GARANTE RESULTADO > 0.00
    FIM_CONTRATO_SAIDA

    OPERACAO CalcularFrete(distanciaKm: DECIMAL(10, 2), pesoCargaKg: INTEIRO32, custoBaseKm: DECIMAL(10, 2)): DECIMAL(10, 2)
    INICIO
        VARIAVEL custoDistancia : DECIMAL(10, 2) <- distanciaKm * custoBaseKm
        VARIAVEL adicionalPeso : DECIMAL(10, 2) <- 0.00

        SE pesoCargaKg > 5000 ENTAO
            adicionalPeso <- 250.00
        FIM_SE

        RETORNE custoDistancia + adicionalPeso
    FIM
FIM_REGRA_NEGOCIO

# --- PONTO DE ENTRADA DO SISTEMA ---
PROCEDIMENTO Principal()
INICIO
    VARIAVEL caminhao : Veiculo <- CRIAR Veiculo(
        placa: "BRA2E19",
        capacidadeCargaKg: 12000,
        custoKmRodado: 4.80,
        status: DISPONIVEL
    )

    VARIAVEL distancia : DECIMAL(10, 2) <- 350.00
    VARIAVEL cargaKg : INTEIRO32 <- 6500

    VARIAVEL freteFinal : DECIMAL(10, 2) <- DespachoCarga.CalcularFrete(distancia, cargaKg, caminhao.custoKmRodado)

    EXIBA "=============================================="
    EXIBA "  THZ-LANG — Sistema de Despacho de Cargas"
    EXIBA "=============================================="
    EXIBA "Veículo Placa: " + caminhao.placa + " | Status: " + caminhao.status
    EXIBA "Distância da Rota: " + distancia + " km"
    EXIBA "Peso da Carga: " + cargaKg + " kg"
    EXIBA "Valor Total do Frete: R$ " + freteFinal
    EXIBA "=============================================="
FIM

FIM_PROGRAMA
```
