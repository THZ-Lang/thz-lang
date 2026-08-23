# Exemplos THZ-LANG — coleção de partida

Programas autossuficientes que exercitam a gramática canônica (v2.3) no motor JVM.
Todos passam em `thz check` **e** executam com `thz run` sem argumentos extras
(exceto o nº 10, que lê da entrada padrão).

## Como executar

```bat
:: Via distribuição autônoma (Windows)
dist\thz\thz.exe run   exemplos\colecao\01-ola-mundo.thz
dist\thz\thz.exe check exemplos\colecao\05-decimal-financeiro.thz
dist\thz\thz.exe audit exemplos\faturamento.thz
dist\thz\thz.exe doc   exemplos\faturamento.thz
dist\thz\thz.exe gui

:: Ou via JAR com Java 25
java -jar target\thz-jvm-2.3.0.jar run   exemplos\colecao\01-ola-mundo.thz
java -jar target\thz-jvm-2.3.0.jar check exemplos\colecao\05-decimal-financeiro.thz
java -jar target\thz-jvm-2.3.0.jar gui
```

Na IDE Swing (`thz gui`), os arquivos desta pasta aparecem no menu **Exemplos** — clique para
carregar no editor (realce, verificar ▸ executar ▸ formatar ▸ auditoria ▸ doc ▸ IR).

## Índice

| # | Arquivo | Tema | Construtos demonstrados |
|---|---------|------|--------------------------|
| 01 | `01-ola-mundo.thz` | Programa mínimo | `VERSAO_LINGUAGEM`, `PROGRAMA`, `METADADOS_ARQUITETURA`, `PROCEDIMENTO Principal`, `EXIBA` |
| 02 | `02-tipos-estruturas.thz` | Tipos e estruturas | primitivos (`TEXTO/NATURAL32/DECIMAL/LOGICO/DATA`), `CRIAR`, acesso `campo`, mutação, `INVARIANTE` |
| 03 | `03-enumeracoes.thz` | Enumerações | `ENUMERACAO`, membros globais tipados, comparação `= <>`, `E` |
| 04 | `04-controle-fluxo.thz` | Fluxo de controle | `SE/SENAO/FIM_SE`, `ENQUANTO`, `PARA..DE..ATE [PASSO]`, conectivo `NAO` |
| 05 | `05-decimal-financeiro.thz` | Decimais exatos | `DECIMAL(p,s)` BigInt escalado, half-even via `MATEMATICA.arredondar`, `abs/min/max/raiz/potencia` |
| 06 | `06-texto-datas.thz` | Stdlib TEXTO/DATA | `comprimento/aparar/contem/subtexto/substituir/dividir/juntar`, `DATA.hoje/agora/adicionarDias/diferencaDias/diaDaSemana`, indexação `dividir(...)[i]` |
| 07 | `07-resultado-ddd.thz` | DDD + contratos | `RESULTADO[T,E]`, `FALHAR_COM`, `EXIGE/GARANTE` quantificados sobre FATIA (∀) |
| 08 | `08-vetorizado-simd.thz` | Lote vetorizado | `LAYOUT_COLUNAR` (SoA), `VETORIZAR_PARA..PASSO_SIMD`, UUID, acumulador DECIMAL |
| 09 | `09-bloco-memoria.thz` | Memória Efêmera (Arena O(1)) | `USAR_BLOCO_MEMORIA..FIM_BLOCO_MEMORIA`, alocação linear contígua, descarte instantâneo O(1) |
| 10 | `10-entrada-interativa.thz` | Entrada de dados | `LER destino`, fallback com `SE`, conversão para `NATURAL32` |
| 11 | `11-idempotencia-larga-escala.thz` | Idempotência Larga Escala | `IDEMPOTENTE`, `CHAVE_IDEMPOTENCIA`, memoização transacional O(1), supressão de re-execução redundante |



## Canônicos (paridade TS ⇄ JVM)

Fora desta coleção, na raiz `exemplos/`:

* `faturamento.thz` — processamento tributário em lote (motor injeta LOTE demo).
* `pedidos.thz` — DDD com `ENUMERACAO`/`RESULTADO` (requer `--principal` com args).
* `agenda.thz` — procedimentos, `FATIA[ESTRUTURA]` literal e stdlib DATA.

## Convenções usadas nos exemplos

1. Todo programa declara `VERSAO_LINGUAGEM "2.3"` antes de `PROGRAMA`.
2. `METADADOS_ARQUITETURA` completo (domínio/SLO/conformidade) — exigido pelo lint `--estrito`.
3. Atribuição é `<-`; comparação é `=`; conectivos verbais `E OU NAO`.
4. Comentários com `#` até o fim da linha (descartados pelo formatador canônico).
5. Acesso canônico: indexe primeiro (`lote[i]`), depois campos (`.valor`) — via variável temporária.

> Nota: cláusulas `EXIGE/GARANTE` sobre parâmetros `FATIA[T]` são **universais**
> (valem para todo elemento). Veja a nota dentro do exemplo 07.
