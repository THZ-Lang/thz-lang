# THZ-LANG — Gramática Canônica (v2.2)

> **Fonte da verdade da sintaxe.** Qualquer divergência entre este documento,
> o `lexer.ts`/`parser.ts` e a suíte de testes é um defeito. Alterações aqui
> exigem bump de versão conforme a política de compatibilidade.

---

## 1. Política de Compatibilidade de Palavras Reservadas

1. **Imutabilidade retroativa:** palavras reservadas publicadas nunca são removidas, renomeadas ou rebaixadas a identificadores.
2. **Adição controlada:** novas palavras reservadas só entram em versões *minor* ou *major* — nunca em *patch*.
3. **Política estrita:** palavras reservadas não podem ser usadas como identificadores; violação gera `[Erro Sintático][Linha L:C]` com mensagem explícita.
4. **Declaração de alvo:** todo programa corporativo deve declarar `VERSAO_LINGUAGEM "x.y"` antes de `PROGRAMA`; a ausência do pragma é aceita, porém sinalizada pelo toolchain (e reprovada no lint estrito).
5. **Tabela canônica:** a lista oficial vive exclusivamente em `src/keywords.ts`, organizada por categoria (`DECLARACAO`, `FIM_BLOCO`, `CONTRATO`, `CONTROLE`, `MEMORIA`, `MODIFICADOR`, `LITERAL`, `CONECTIVO_LOGICO`).

### Convenções operador/literal

| Domínio | Forma canônica | Notas |
|---|---|---|
| Comparação | símbolos `=  <>  <  <=  >  >=` | `=` é comparação; atribuição usa `<-` |
| Atribuição | `<-` | token atômico `SETA_ATRIBUICAO` |
| Aritmética | símbolos `+  -  *  /  %` | |
| Conectivos lógicos | verbais `E  OU  NAO` | coerência com sintaxe PT-BR |
| Literais lógicos/nulo | verbais `VERDADEIRO  FALSO  NULO` | |

---

## 2. Gramática EBNF (alvo v2.2)

Notação: `{ }` = repetição zero ou mais; `[ ]` = opcional; `|` = alternativa.
Marcados com **[2.2]** os constructos novos nesta versão.

```ebnf
(* ===================== PROGRAMA ===================== *)

programa          = [ versao_linguagem ] , "PROGRAMA" , identificador ,
                    { decl_topo } , "FIM_PROGRAMA" ;

versao_linguagem  = "VERSAO_LINGUAGEM" , texto ;                     (* [2.2] *)

decl_topo         = metadados | estrutura | enumeracao | regra_negocio ;

(* ===================== METADADOS ==================== *)

metadados         = "METADADOS_ARQUITETURA" ,
                    { chave_metadado , ":" , valor_metadado } ,
                    "FIM_METADADOS" ;

chave_metadado    = identificador ;   (* DOMINIO, SUBDOMINIO, CAMADA,
                                         VERSAO, AUTOR, SLO_LATENCIA_MAXIMA,
                                         CONFORMIDADE *)
valor_metadado    = texto { "," , texto } ;

(* ===================== ESTRUTURA ==================== *)

estrutura         = "ESTRUTURA" , identificador , [ "LAYOUT_COLUNAR" ] ,
                    { campo | invariante } , "FIM_ESTRUTURA" ;

campo             = identificador , ":" , tipo_dado ;

(* [2.2] Arquitetura Viva: cláusula avaliada em toda construção/mutação. *)
invariante        = "INVARIANTE" , expressao ;

(* [2.2] Enumerações de domínio; membros são identificadores globais. *)
enumeracao        = "ENUMERACAO" , identificador ,
                    { identificador } , "FIM_ENUMERACAO" ;

tipo_dado         = identificador , [ "(" , args_tipo , ")" ]
                  | tipo_fatiado ;
args_tipo         = numero { ("," | número) } ;   (* ex.: DECIMAL(12,4) *)
tipo_fatiado      = ( "FATIA" , "[" , identificador , "]"
                    | "RESULTADO" , "[" , tipo_dado , "," , tipo_dado , "]" );  (* [2.2] *)

(* ===================== REGRA DE NEGÓCIO ============= *)

regra_negocio     = "REGRA_NEGOCIO" , identificador ,
                    { elemento_regra } , "FIM_REGRA_NEGOCIO" ;

elemento_regra    = chave_governanca | contrato | operacao ;

chave_governanca  = ( "IDENTIFICADOR_REGRA" | "RASTREIO_REQUISITO" | "DESCRICAO" ) ,
                    ":" , texto ;

contrato          = bloco_contrato_entrada | bloco_contrato_saida ;
bloco_contrato_entrada = "CONTRATO_ENTRADA" , { clausula_exige } , "FIM_CONTRATO_ENTRADA" ;
bloco_contrato_saida   = "CONTRATO_SAIDA"   , { clausula_garante } , "FIM_CONTRATO_SAIDA" ;
clausula_exige    = "EXIGE"   , expressao ;                          (* [2.2] árvore avaliável *)
clausula_garante  = "GARANTE" , expressao ;                          (* [2.2] árvore avaliável *)

operacao          = "OPERACAO" , identificador ,
                    "(" , [ parametros ] , ")" , ":" , tipo_retorno ,
                    [ corpo_operacao ] ;

parametros        = parametro { "," , parametro } ;
parametro         = identificador , ":" , tipo_parametro ;
tipo_parametro    = identificador , [ "[" , identificador , "]" ] ;  (* FATIA[T] *)
tipo_retorno      = tipo_dado ;

corpo_operacao    = "INICIO" , { comando } , "FIM" ;                 (* [2.2] *)

(* ===================== COMANDOS [2.2] =============== *)

comando           = decl_variavel
                  | atribuicao
                  | comando_se
                  | comando_enquanto
                  | comando_vetorizado
                  | bloco_memoria
                  | comando_exiba
                  | comando_retorne
                  | comando_falhar_com                          (* [2.2] *)
                  | chamada ;

decl_variavel     = "VARIAVEL" , identificador , ":" , tipo_dado ,
                    "<-" , expressao ;

atribuicao        = acesso_campo , "<-" , expressao ;

comando_se        = "SE" , expressao , { comando } ,
                    [ "SENAO" , { comando } ] , "FIM_SE" ;

comando_enquanto  = "ENQUANTO" , expressao , { comando } , "FIM_ENQUANTO" ;

comando_vetorizado= "VETORIZAR_PARA" , identificador , "EM" , acesso_campo ,
                    [ "PASSO_SIMD" , numero ] , { comando } , "FIM_PARA" ;

bloco_memoria     = "USAR_BLOCO_MEMORIA" , identificador ,
                    { comando } , "FIM_BLOCO_MEMORIA" ;

comando_exiba     = "EXIBA" , expressao ;
comando_retorne   = "RETORNE" , [ expressao ] ;

(* [2.2] Canal de erro de RESULTADO[T,E]: interrompe a operação e devolve
   RESULTADO(falha, valor); GARANTE avalia apenas o caminho de sucesso. *)
comando_falhar_com= "FALHAR_COM" , expressao ;
chamada           = identificador , "(" , [ argumentos ] , ")" ;
argumentos        = expressao { "," , expressao } ;

(* ===================== EXPRESSÕES [2.2] ============= *)

(* Precedência crescente: OU < E < relacional < aditivo < multiplicativo < unário *)

expressao         = expr_ou ;
expr_ou           = expr_e { "OU" , expr_e } ;
expr_e            = expr_relacional { "E" , expr_relacional } ;
expr_relacional   = expr_aditiva [ ( "=" | "<>" | "<" | "<=" | ">" | ">=" ) , expr_aditiva ] ;
expr_aditiva      = expr_multiplicativa { ( "+" | "-" ) , expr_multiplicativa } ;
expr_mult         = expr_unaria { ( "*" | "/" | "%" ) , expr_unaria } ;
expr_unaria       = [ "-" | "NAO" ] , expr_primaria ;
expr_primaria     = numero
                  | texto
                  | "VERDADEIRO" | "FALSO" | "NULO"
                  | acesso_campo
                  | chamada
                  | "(" , expressao , ")" ;

acesso_campo      = identificador { "." , identificador } ;

(* ===================== LÉXICO ======================= *)

identificador     = letra , { letra | dígito | "_" } ;
numero            = dígito , { dígito | "_" } , [ "." , dígito , { dígito } ] ;
texto             = '"' , { caractere - '"' | "\\n" } , '"' ;
comentario        = "#" , { caractere - fim_de_linha } ;
```

**Chaves contextuais** (`IDENTIFICADOR_REGRA`, `RASTREIO_REQUISITO`, `DESCRICAO`) e nomes de tipos (`DECIMAL`, `MONETARIO`, `NATURAL32`, …) **não** são palavras reservadas: são identificadores reconhecidos em posição sintática específica e validados pela análise semântica.

---

## 3. Palavras Reservadas por Categoria (v2.2)

| Categoria | Palavras |
|---|---|
| DECLARACAO | `PROGRAMA` `METADADOS_ARQUITETURA` `ESTRUTURA` `ENUMERACAO` `REGRA_NEGOCIO` `PROCEDIMENTO`† `OPERACAO` `VARIAVEL` `VERSAO_LINGUAGEM` |
| FIM_BLOCO | `FIM_PROGRAMA` `FIM_METADADOS` `FIM_ESTRUTURA` `FIM_ENUMERACAO` `FIM_REGRA_NEGOCIO` `FIM_PARA` `FIM_BLOCO_MEMORIA` `FIM_SE` `FIM_ENQUANTO` `FIM` |
| CONTRATO | `EXIGE` `GARANTE` `INVARIANTE` |
| CONTROLE | `INICIO` `SE` `SENAO` `ENQUANTO` `RETORNE` `FALHAR_COM` `EXIBA` `VETORIZAR_PARA` |
| MEMORIA | `USAR_BLOCO_MEMORIA` |
| MODIFICADOR | `LAYOUT_COLUNAR` `EM` `PASSO_SIMD` |
| LITERAL | `VERDADEIRO` `FALSO` `NULO` |
| CONECTIVO_LOGICO | `E` `OU` `NAO` |

† Reservada desde v2.1; implementação sintática completa planejada — uso gera erro explícito citando a versão.

---

## 4. Tooling e correspondência com a gramática

Esta gramática descreve **apenas a sintaxe**. O tooling não altera a linguagem, mas a consome:

- **Formatador** `thz fmt` (`src/fmt.ts`): re-emite AST em forma canônica `formatar()` — idempotente, preserva semântica, descarta comentários `#` (trivia não armazenada na AST). Usado por `thz fmt --check/--escrever`, LSP `textDocument/formatting` e Playground `✨ Fmt`.
- **THZ-IR** `thz-ir/1` (`src/ir.ts`) e **SIMD** (`src/simd.ts` R1-R5): analisam `VETORIZAR_PARA ... PASSO_SIMD` sobreposto à gramática; não acrescentam produções.
- **Auditoria** (`src/governanca.ts`): valida `RASTREIO_REQUISITO` e contratos `EXIGE`/`GARANTE`/`INVARIANTE` definidos acima.
- Toda nova produção EBNF deve vir acompanhada de: keyword em `src/keywords.ts`, regra em `src/parser.ts`, validação em `src/analisador.ts`, caso em `src/runtime.ts`/`src/interpretador.ts`/`src/fmt.ts`/`src/docgen.ts` e golden test em `test/__snapshots__`.
