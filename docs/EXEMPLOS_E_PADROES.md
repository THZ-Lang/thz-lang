# Guia de Exemplos e Padrões — THZ-LANG (v2.4.0)

> **18 programas reais.** Este guia expande de 5 para 12 receitas cobrindo todos os arquétipos (`PROGRAMA`, `PIPELINE_DADOS`, `TELA`, `BIBLIOTECA`, `TESTE`) com código extraído de `exemplos/*.thz` e `exemplos/*.thzui`, prontos para `thz check|run|ir|audit`.

Referências: `exemplos/` (18 arquivos), [`MANUAL_LINGUAGEM.md`](MANUAL_LINGUAGEM.md), [`PIPELINE_DADOS.md`](PIPELINE_DADOS.md), [`TELA_THZUI.md`](TELA_THZUI.md).

---

## Índice

1. [DDD: Cadastro & Validação](#1-padrão-ddd-cadastro--validação-de-clientes) — `exemplos/cadastro_cliente_gui.thz` vibes
2. [SoA / SIMD Lote](#2-processamento-financeiro-de-alto-desempenho-soa--simd) — `processamento_simd_arena.thz`
3. [RESULTADO sem exceção](#3-tratamento-seguro-de-erros-com-resultado) — `gestao_contratos_resultado.thz`
4. [TELA .thzui](#4-interface-gráfica-declarativa-thzui) — `faturamento_dashboard.thzui`
5. [TELA + REGRA_NEGOCIO](#5-tela--regra_negocio--formulário-reativo) — `cadastro_produto_gui.thz`
6. [PIPELINE_DADOS](#6-pipeline_dados--etl-streaming) — `pipeline_etl_telemetria.thz`
7. [BIBLIOTECA](#7-biblioteca--código-reutilizável) — `biblioteca_utilitarios_fintech.thz`
8. [Arquivos & Relatórios](#8-arquivos--relatórios-pdfxlsxdocx) — `automacao_arquivos_relatorios.thz`
9. [Criptografia & Auditoria](#9-segurança--criptografia-e-auditoria) — `seguranca_criptografia_auditoria.thz`
10. [Temporal / Folha](#10-temporal--folha-de-pagamento) — `calculo_temporal_folha.thz`
11. [Arena O(1)](#11-arena-o1--usar_bloco_memoria) — `processamento_simd_arena.thz`
12. [Teste Integrado](#12-teste-integrado--teste) — `suite_testes_integrados.thz`

---

## 1. Padrão DDD: Cadastro & Validação de Clientes

`exemplos/cadastro_cliente_gui.thz` + `exemplos/pedidos.thz` — `ENUMERACAO`, `ESTRUTURA` com `INVARIANTE`, `REGRA_NEGOCIO` com `EXIGE/GARANTE`:

```thz
PROGRAMA NEGOCIO GestaoClientes

METADADOS_ARQUITETURA
    SISTEMA: "CRM"
    DOMINIO: "Vendas"
    SLO_LATENCIA_MS: 30
FIM_METADADOS

ENUMERACAO NivelRisco
    BAIXO,
    MEDIO,
    ALTO
FIM_ENUMERACAO

ESTRUTURA Cliente
    id: INTEIRO
    nome: TEXTO
    documento: TEXTO
    limite_credito: DECIMAL(12, 2)
    risco: NivelRisco
    INVARIANTE limite_credito >= 0.00
FIM_ESTRUTURA

REGRA_NEGOCIO AvaliarCreditoCliente
    RASTREIO_REQUISITO: "REQ-CRM-001"
    EXIGE: cliente.id > 0
    GARANTE: RESULTADO == VERDADEIRO OU RESULTADO == FALSO
    INICIO
        SE cliente.risco == NivelRisco.ALTO ENTAO RETORNAR FALSO
        SENAO RETORNAR VERDADEIRO FIM_SE
    FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA
```

```bash
./gradlew :thz-cli-jvm:run --args="check exemplos/pedidos.thz --estrito"
./gradlew :thz-cli-jvm:run --args="audit exemplos/pedidos.thz --json" | jq .violacoes
```

---

## 2. Processamento Financeiro de Alto Desempenho (SoA / SIMD)

`exemplos/processamento_simd_arena.thz` + `exemplos/faturamento.thz:15` — `LAYOUT_COLUNAR` + `VETORIZAR_PARA PASSO_SIMD 8`:

```thz
PROGRAMA NEGOCIO FaturamentoVetorizado
METADADOS_ARQUITETURA SISTEMA: "MotorCalculo" DOMINIO: "Financeiro" SLO_LATENCIA_MS: 15 FIM_METADADOS

ESTRUTURA LoteItem LAYOUT_COLUNAR
    id: INTEIRO
    quantidade: INTEIRO32
    preco_unitario: DECIMAL(12, 2)
    subtotal: DECIMAL(12, 2)
FIM_ESTRUTURA

REGRA_NEGOCIO CalcularSubtotaisLote
    RASTREIO_REQUISITO: "REQ-FIN-SIMD-01"
    EXIGE: tamanho(lote) > 0
    INICIO
        VETORIZAR_PARA i DE 0 ATE tamanho(lote)-1 PASSO_SIMD 8
            lote.subtotal[i] <- lote.quantidade[i] * lote.preco_unitario[i]
        FIM_VETORIZAR
    FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA
```

Ver `GUIA_PERFORMANCE.md:2` para quando usar `8` vs `16`.

---

## 3. Tratamento Seguro de Erros com `RESULTADO`

`exemplos/gestao_contratos_resultado.thz` — `FALHAR_COM` + `CASO_RESULTADO`:

```thz
PROGRAMA NEGOCIO TransferenciaBancaria
PROCEDIMENTO ExecutarTransferencia(origem_id: INTEIRO, destino_id: INTEIRO, valor: MONETARIO(BRL))
INICIO
    SE valor <= 0.00 BRL ENTAO FALHAR_COM("Valor deve ser positivo")
    SENAO RETORNAR RESULTADO("Transferencia de " + valor + " realizada") FIM_SE
FIM
PROCEDIMENTO Processar()
INICIO
    VARIAVEL res <- ExecutarTransferencia(1001, 2002, 1500.00 BRL)
    CASO_RESULTADO res
        SUCESSO mensagem => EXIBA("[OK] " + mensagem)
        ERRO erro_msg    => EXIBA("[FALHA] " + erro_msg)
    FIM_CASO
FIM
FIM_PROGRAMA
```

---

## 4. Interface Gráfica Declarativa `.thzui`

`exemplos/faturamento_dashboard.thzui:1` — `TELA` pura (ver `TELA_THZUI.md:2`):

```thz
TELA DashboardVendas
METADADOS_ARQUITETURA DOMINIO: "Vendas" CAMADA: "Apresentacao" FIM_METADADOS
PROCEDIMENTO MontarInterface()
INICIO
    TELA.criarContainer("raiz", "CONTAINER")
    TELA.criarCard("card_faturamento", "Gestão de Faturamento & Métricas")
    TELA.adicionarMetrica("kpi_receita", "Receita Diária", "R$ 1.450.000,00")
    TELA.adicionarCampoTexto("txt_cliente", "Cliente", "Filtrar...")
    TELA.adicionarCampoMoeda("txt_valor_min", "Valor Mínimo", "BRL")
    TELA.adicionarBotao("btn_filtrar", "Filtrar", "AplicarFiltro")
    TELA.exibir("DashboardVendas")
FIM
FIM_TELA
```

```bash
./gradlew :thz-cli-jvm:run --args="ui exemplos/faturamento_dashboard.thzui --html" > dash.html
```

---

## 5. TELA + REGRA_NEGOCIO — Formulário Reativo

`exemplos/cadastro_produto_gui.thz:1` (115 linhas) — `TELA.renderizarFormulario` gera campos de `ESTRUTURA` + valida `INVARIANTE`:

```thz
PROGRAMA NEGOCIO CadastroProdutoGui
ESTRUTURA Produto
    sku: TEXTO
    nome: TEXTO
    preco: DECIMAL(12, 2)
    INVARIANTE preco >= 0.00
FIM_ESTRUTURA

REGRA_NEGOCIO GestaoEstoque
    OPERACAO SalvarProduto(p: Produto): RESULTADO[TEXTO,TEXTO]
    INICIO EXIGE: p.preco >= 0.00 RETORNAR RESULTADO("Salvo: " + p.sku) FIM
FIM_REGRA_NEGOCIO

PROCEDIMENTO Principal()
INICIO
    VARIAVEL form <- CRIAR Produto(sku: "SKU-001", nome: "Notebook", preco: 1500.00)
    TELA.renderizarFormulario(form, "GestaoEstoque.SalvarProduto")
FIM
FIM_PROGRAMA
```

```bash
./gradlew :thz-cli-jvm:run --args="run exemplos/cadastro_produto_gui.thz" # WebView
```

Outros GUIs: `hello_world_gui.thz`, `pedido_vendas_gui.thz` (vendas), `showcase_widgets_gui.thz` (todos widgets), `simulador_credito_gui.thz` (juros), `cadastro_cliente_gui.thz`.

---

## 6. PIPELINE_DADOS — ETL Streaming

`exemplos/pipeline_etl_telemetria.thz:1` (ver `PIPELINE_DADOS.md:3`):

```thz
PIPELINE_DADOS TelemetriaTransacional
METADADOS_ARQUITETURA DOMINIO: "EngenhariaDeDados" SLO_LATENCIA_MAXIMA: "5ms" FIM_METADADOS
ESTRUTURA EventoTelemetria LAYOUT_COLUNAR
    latencia_ms: INTEIRO32
    taxa_erros_pct: DECIMAL(5, 2)
FIM_ESTRUTURA
FONTE_ENTRADA Origem TIPO: "STREAMING" CONECTOR: "POSTGRESQL" FORMATO: "JSONB" FIM_FONTE
DESTINO_SAIDA Destino CONECTOR: "MONGODB" COLECAO: "eventos" FIM_DESTINO
TRANSFORMACAO Agregar RASTREIO_REQUISITO: "REQ-STREAM-4412" EXIGE: tamanho(lote)>0
    VETORIZAR_PARA ev EM eventos PASSO_SIMD 8
        SE ev.taxa_erros_pct > 5.00 ENTAO ev.severidade <- CRITICO FIM_SE
    FIM_PARA
FIM_TRANSFORMACAO
FIM_PIPELINE
```

---

## 7. BIBLIOTECA — Código Reutilizável

`exemplos/biblioteca_utilitarios_fintech.thz` — `BIBLIOTECA` + `IMPORTAR`:

```thz
BIBLIOTECA UtilitariosFintech
METADADOS_ARQUITETURA DOMINIO: "Financeiro" CAMADA: "Dominio" FIM_METADADOS

REGRA_NEGOCIO JurosCompostos
    OPERACAO Calcular(principal: DECIMAL(12,2), taxa: DECIMAL(5,4), periodos: INTEIRO): DECIMAL(12,2)
    INICIO
        VARIAVEL montante <- principal
        PARA i DE 1 ATE periodos PASSO 1 FACA
            montante <- montante * (1.0000 + taxa)
        FIM_PARA
        RETORNE montante
    FIM
FIM_REGRA_NEGOCIO
FIM_BIBLIOTECA

# Uso:
# PROGRAMA NEGOCIO App
# IMPORTAR Calcular DE "biblioteca_utilitarios_fintech.thz"
# VARIAVEL m <- JurosCompostos.Calcular(1000.00, 0.05, 12)
```

---

## 8. Arquivos & Relatórios (PDF/XLSX/DOCX)

`exemplos/automacao_arquivos_relatorios.thz` — `ARQUIVO`, `DIRETORIO`, `DOCUMENTO`:

```thz
PROGRAMA NEGOCIO AutomacaoRelatorios
PROCEDIMENTO Gerar()
INICIO
    VARIAVEL dados <- ARQUIVO.lerTexto("./data/faturamento.csv")
    VARIAVEL linhas <- TEXTO.dividir(dados, "\n")
    DOCUMENTO.exportarXlsx("./dist/relatorio.xlsx", "Faturamento", linhas)
    DOCUMENTO.exportarPdf("./dist/relatorio.pdf", "Faturamento", linhas)
    EXIBA("Relatórios gerados em ./dist/")
FIM
FIM_PROGRAMA
```

Stdlib: `ARQUIVO.lerTexto/escreverTexto`, `DIRETORIO.listar/criar`, `DOCUMENTO.exportar*` (POI/OpenPDF, `thz-core-jvm/build.gradle.kts:30`).

---

## 9. Segurança — Criptografia e Auditoria

`exemplos/seguranca_criptografia_auditoria.thz` — `SEGURANCA`, `LOG`:

```thz
PROGRAMA NEGOCIO SegurancaAuditoria
PROCEDIMENTO Proteger()
INICIO
    VARIAVEL hash <- SEGURANCA.sha256("dado sensivel")
    VARIAVEL token <- SEGURANCA.gerarToken(32)
    VARIAVEL aes <- SEGURANCA.criptografarAes("segredo", "chave-32-bytes-aqui-123456")
    VARIAVEL ok <- SEGURANCA.verificarSenha("senha", SEGURANCA.hashSenha("senha"))
    LOG.auditoria("usuario-123", "ACESSO_DADO", "recurso-456")
    EXIBA("Hash: " + hash + " Token: " + token)
FIM
FIM_PROGRAMA
```

Validar: `thz audit seguranca_criptografia_auditoria.thz` (LGPD, SOX).

---

## 10. Temporal — Folha de Pagamento

`exemplos/calculo_temporal_folha.thz` — `DATA`, `VERSAO`:

```thz
PROGRAMA NEGOCIO FolhaPagamento
PROCEDIMENTO CalcularFolha()
INICIO
    VARIAVEL hoje <- DATA.hoje()
    VARIAVEL admissao <- DATA.criar(2020, 3, 15)
    VARIAVEL dias <- DATA.diferencaDias(admissao, hoje)
    VARIAVEL aniversario <- DATA.adicionarDias(hoje, 30)
    EXIBA("Dias na empresa: " + dias + " Hoje: " + DATA.texto(hoje))
    SE VERSAO.satisfaz(VERSAO.obter(), ">=2.4.0") ENTAO EXIBA("THZ 2.4+") FIM_SE
FIM
FIM_PROGRAMA
```

---

## 11. Arena O(1) — `USAR_BLOCO_MEMORIA`

`exemplos/processamento_simd_arena.thz` — `USAR_BLOCO_MEMORIA` + `VETORIZAR_PARA`:

```thz
PROGRAMA NEGOCIO ProcessamentoArena
PROCEDIMENTO Lote()
INICIO
    USAR_BLOCO_MEMORIA BlocoTemp FACA
        VARIAVEL itens <- CarregarLote()  # FATIA[Item] LAYOUT_COLUNAR
        VETORIZAR_PARA it EM itens PASSO_SIMD 8
            it.total <- it.qtd * it.preco
        FIM_PARA
    FIM_BLOCO_MEMORIA  # libera O(1)
FIM
FIM_PROGRAMA
```

Ver `GUIA_PERFORMANCE.md:4`, `RUNTIME_NATIVO.md:4`, `BlocoMemoria.java:50`.

---

## 12. Teste Integrado — `TESTE`

`exemplos/suite_testes_integrados.thz` — arquétipo `TESTE`:

```thz
TESTE SuiteIntegrada
METADADOS_ARQUITETURA DOMINIO: "Qualidade" FIM_METADADOS

REGRA_NEGOCIO TestesCalculo
    OPERACAO TestarSoma(): LOGICO
    INICIO
        VARIAVEL a <- 2
        VARIAVEL b <- 3
        RETORNE (a + b) == 5
    FIM
FIM_REGRA_NEGOCIO
FIM_TESTE
```

```bash
./gradlew :thz-cli-jvm:run --args="check exemplos/suite_testes_integrados.thz"
./gradlew test  # JUnit 5 também valida TESTE
```

---

## Tabela — Todos os `exemplos/` (18 arquivos)

| Arquivo | Arquétipo | Conceitos |
| :--- | :--- | :--- |
| `faturamento.thz` | `PROGRAMA` | `LAYOUT_COLUNAR`, `VETORIZAR_PARA 8`, `DECIMAL`, contratos |
| `pedidos.thz` | `PROGRAMA` | DDD, `RESULTADO` |
| `processamento_simd_arena.thz` | `PROGRAMA` | SoA+SIMD+Arena |
| `pipeline_etl_telemetria.thz` | `PIPELINE_DADOS` | Streaming, SoA, `FONTE/TRANSFORMACAO/DESTINO` |
| `faturamento_dashboard.thzui` | `TELA` | `criarCard`, `adicionarMetrica` |
| `thz_studio_ide.thzui` | `TELA` | IDE declarativa |
| `cadastro_produto_gui.thz` | `PROGRAMA` + `TELA.renderizarFormulario` | Formulário reativo |
| `cadastro_cliente_gui.thz` | `PROGRAMA` | GUI cliente |
| `hello_world_gui.thz` | `PROGRAMA VISUAL` | Mínimo GUI |
| `pedido_vendas_gui.thz` | `PROGRAMA` | Vendas GUI |
| `showcase_widgets_gui.thz` | `PROGRAMA` | Todos widgets |
| `simulador_credito_gui.thz` | `PROGRAMA` | Juros compostos GUI |
| `biblioteca_utilitarios_fintech.thz` | `BIBLIOTECA` | `IMPORTAR`, juros |
| `automacao_arquivos_relatorios.thz` | `PROGRAMA` | `ARQUIVO`, `DOCUMENTO`, `DIRETORIO` |
| `seguranca_criptografia_auditoria.thz` | `PROGRAMA` | `SEGURANCA`, `LOG` |
| `calculo_temporal_folha.thz` | `PROGRAMA` | `DATA`, `VERSAO` |
| `gestao_contratos_resultado.thz` | `PROGRAMA` | `RESULTADO`, `FALHAR_COM` |
| `suite_testes_integrados.thz` | `TESTE` | `TESTE` arquétipo |

```bash
# Validar todos de uma vez
./scripts/health-check.ps1
./gradlew :thz-cli-jvm:run --args="check exemplos/faturamento.thz && check exemplos/pipeline_etl_telemetria.thz"
```

