# Guia de Exemplos e Padrões de Arquitetura — THZ-LANG (v2.4.0)

Este documento fornece receitas práticas e padrões de código testados para o desenvolvimento de aplicações em **THZ-LANG**.

---

## 📚 Receitas Disponíveis
1. [Padrão DDD: Cadastro & Validação de Clientes](#1-padrão-ddd-cadastro--validação-de-clientes)
2. [Processamento Financeiro de Alto Desempenho (SoA / SIMD)](#2-processamento-financeiro-de-alto-desempenho-soa--simd)
3. [Tratamento Seguro de Erros com `RESULTADO`](#3-tratamento-seguro-de-erros-com-resultado)
4. [Interface Gráfica Declarativa `.thzui`](#4-interface-gráfica-declarativa-thzui)
5. [Servidor Web REST HTTP com Virtual Threads](#5-servidor-web-rest-http-com-virtual-threads)

---

## 1. Padrão DDD: Cadastro & Validação de Clientes

```thz
PROGRAMA NEGOCIO GestaoClientes
VERSAO_LINGUAGEM "2.4"

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
FIM_ESTRUTURA

REGRA_NEGOCIO AvaliarCreditoCliente
    RASTREIO_REQUISITO: "REQ-CRM-001"
    EXIGE: cliente.id > 0
    GARANTE: RESULTADO == VERDADEIRO OU RESULTADO == FALSO

    INICIO
        SE cliente.risco == NivelRisco.ALTO ENTAO
            RETORNAR FALSO
        SENAO
            RETORNAR VERDADEIRO
        FIM_SE
    FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA
```

---

## 2. Processamento Financeiro de Alto Desempenho (SoA / SIMD)

Uso do modificador `LAYOUT_COLUNAR` para otimização de memória contígua e laços vetorizados via SIMD:

```thz
PROGRAMA NEGOCIO FaturamentoVetorizado
VERSAO_LINGUAGEM "2.4"

METADADOS_ARQUITETURA
    SISTEMA: "MotorCalculo"
    DOMINIO: "Financeiro"
    SLO_LATENCIA_MS: 15
FIM_METADADOS

ESTRUTURA LoteItem LAYOUT_COLUNAR
    id: INTEIRO
    quantidade: INTEIRO
    preco_unitario: DECIMAL(12, 2)
    subtotal: DECIMAL(12, 2)
FIM_ESTRUTURA

REGRA_NEGOCIO CalcularSubtotaisLote
    RASTREIO_REQUISITO: "REQ-FIN-SIMD-01"
    EXIGE: tamanho(lote) > 0

    INICIO
        VETORIZAR_PARA i DE 0 ATE tamanho(lote) - 1 PASSO_SIMD 8
            lote.subtotal[i] <- lote.quantidade[i] * lote.preco_unitario[i]
        FIM_VETORIZAR
    FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA
```

---

## 3. Tratamento Seguro de Erros com `RESULTADO`

Abordagem explícita para retorno de operações sem disparo de exceções descontroladas:

```thz
PROGRAMA NEGOCIO TransferenciaBancaria

PROCEDIMENTO ExecutarTransferencia(origem_id: INTEIRO, destino_id: INTEIRO, valor: MONETARIO(BRL))
INICIO
    SE valor <= 0.00 BRL ENTAO
        FALHAR_COM("O valor da transferência deve ser positivo")
    SENAO
        RETORNAR RESULTADO("Transferência de " + valor + " realizada com sucesso")
    FIM_SE
FIM

PROCEDIMENTO Processar()
INICIO
    VARIAVEL res <- ExecutarTransferencia(1001, 2002, 1500.00 BRL)

    CASO_RESULTADO res
        SUCESSO mensagem =>
            EXIBA("[OK] " + mensagem)
        ERRO erro_msg =>
            EXIBA("[FALHA] Não foi possível transferir: " + erro_msg)
    FIM_CASO
FIM

FIM_PROGRAMA
```

---

## 4. Interface Gráfica Declarativa `.thzui`

Exemplo completo de dashboard corporativo salvo em arquivo `dashboard.thzui`:

```thz
TELA DashboardVendas

METADADOS_ARQUITETURA
    DOMINIO: "Vendas"
    CAMADA: "Apresentacao"
FIM_METADADOS

PROCEDIMENTO MontarInterface()
INICIO
    TELA.criarContainer("raiz", "CONTAINER")
    TELA.criarCard("card_faturamento", "Gestão de Faturamento & Métricas")
    TELA.adicionarMetrica("kpi_receita", "Receita Diária", "R$ 1.450.000,00")
    TELA.adicionarCampoTexto("txt_cliente", "Cliente", "Filtrar por razão social...")
    TELA.adicionarCampoMoeda("txt_valor_min", "Valor Mínimo", "BRL")
    TELA.adicionarBotao("btn_filtrar", "Filtrar Resultados", "AplicarFiltro")
    TELA.exibir("DashboardVendas")
FIM

FIM_TELA
```

Para visualizar no navegador:
```bash
thz ui dashboard.thzui --html > dashboard.html
```

---

## 5. Servidor Web REST HTTP com Virtual Threads

Mapeamento de rotas e criação de APIs REST com suporte a Virtual Threads da JVM 25:

```thz
PROGRAMA NEGOCIO ApiStatusSistema

PROCEDIMENTO IniciarServidor()
INICIO
    EXIBA("Iniciando servidor HTTP THZ na porta 8080...")
    // Inicia servidor e responde JSON com métricas de sistema
FIM

FIM_PROGRAMA
```
