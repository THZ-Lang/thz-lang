# Exemplos da sintaxe enxuta

Esta pasta apresenta a forma canônica enxuta da THZ-LANG em ordem crescente
de complexidade. A indentação delimita os blocos, `:=` declara e infere uma
variável, `=` atribui um novo valor e `->` informa o retorno de uma função ou
operação.

1. `01_ola_mundo.thz`: programa e procedimento principal.
2. `02_funcoes_e_inferencia.thz`: funções tipadas e inferência local.
3. `03_controle_de_fluxo.thz`: condição, repetição e atribuição.
4. `04_estruturas.thz`: definição de tipos de domínio.
5. `05_regra_de_negocio.thz`: contratos diretos e operação corporativa.
6. `06_pipeline_dados.thz`: arquétipo enxuto para processamento de dados.

Para reescrever um arquivo na forma estrutural antiga, use:

```powershell
thz fmt --legado exemplos/sintaxe_enxuta/01_ola_mundo.thz
```
