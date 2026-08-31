# Especificação Formal EBNF — THZ-LANG (v2.4.0)

Este documento define a gramática formal em sintaxe EBNF (*Extended Backus-Naur Form*) do **THZ-LANG**.

---

## 1. Estrutura Global do Programa e Módulos

```ebnf
Programa          ::= ModuloHeader MetadadosHeader? Importacao* Declaracao* TerminadorModulo ;
ModuloHeader      ::= ArquetipoModulo IDENTIFICADOR ;

MetadadosHeader   ::= "METADADOS_ARQUITETURA" MetadadoItem* "FIM_METADADOS" ;
MetadadoItem      ::= IDENTIFICADOR ":" (STRING_LITERAL | NUMERO | IDENTIFICADOR) ;

ArquetipoModulo   ::= "PROGRAMA" ("NEGOCIO" | "VISUAL" | "ARQUITETURA")?
                    | "PIPELINE_DADOS"
                    | "BIBLIOTECA"
                    | "EXTENSAO"
                    | "FERRAMENTA"
                    | "TESTE"
                    | "TELA" ;

TerminadorModulo  ::= "FIM_PROGRAMA"
                    | "FIM_PIPELINE"
                    | "FIM_BIBLIOTECA"
                    | "FIM_EXTENSAO"
                    | "FIM_FERRAMENTA"
                    | "FIM_TESTE"
                    | "FIM_TELA" ;
```

---

## 2. Elementos de Módulo

```ebnf
Declaracao        ::= Importacao
                    | DeclaracaoEstrutura
                    | DeclaracaoEnum
                    | RegraNegocio
                    | DeclaracaoPipelineBloco
                    | Procedimento
                    | Funcao ;

Importacao        ::= "IMPORTAR" IdentificadorLista "DE" STRING_LITERAL ;
IdentificadorLista::= IDENTIFICADOR ("," IDENTIFICADOR)* ;
```

---

## 3. Estruturas e Enumerações

```ebnf
DeclaracaoEstrutura ::= "ESTRUTURA" IDENTIFICADOR LayoutModificador? (CampoEstrutura | InvarianteEstrutura)* "FIM_ESTRUTURA" ;
LayoutModificador   ::= "LAYOUT_COLUNAR" ;
CampoEstrutura      ::= IDENTIFICADOR ":" TipoDado ;
InvarianteEstrutura ::= "INVARIANTE" Expressao ;

DeclaracaoEnum      ::= "ENUMERACAO" IDENTIFICADOR ItemEnum ("," ItemEnum)* "FIM_ENUMERACAO" ;
ItemEnum            ::= IDENTIFICADOR ;
```

---

## 4. Governança, Regras de Negócio e Big Data Pipelines

```ebnf
RegraNegocio       ::= "REGRA_NEGOCIO" IDENTIFICADOR ClausulaGovernanca* BlocoCodigo? "FIM_REGRA_NEGOCIO" ;

ClausulaGovernanca ::= RastreioRequisito | ClausulaExige | ClausulaGarante | ClausulaInvariante ;
RastreioRequisito  ::= "RASTREIO_REQUISITO" ":" STRING_LITERAL ;
ClausulaExige      ::= "EXIGE" ":" Expressao ;
ClausulaGarante    ::= "GARANTE" ":" Expressao ;
ClausulaInvariante ::= "INVARIANTE" ":" Expressao ;

DeclaracaoPipelineBloco ::= FonteEntradaBloco | DestinoSaidaBloco | TransformacaoBloco ;

FonteEntradaBloco   ::= "FONTE_ENTRADA" IDENTIFICADOR PropriedadeItem* "FIM_FONTE" ;
DestinoSaidaBloco   ::= "DESTINO_SAIDA" IDENTIFICADOR PropriedadeItem* "FIM_DESTINO" ;
TransformacaoBloco  ::= "TRANSFORMACAO" IDENTIFICADOR ClausulaGovernanca* BlocoCodigo "FIM_TRANSFORMACAO" ;
PropriedadeItem     ::= IDENTIFICADOR ":" (STRING_LITERAL | NUMERO | IDENTIFICADOR) ;
```

---

## 5. Procedimentos e Comandos

```ebnf
Procedimento       ::= "PROCEDIMENTO" IDENTIFICADOR "(" Parametros? ")" (":" TipoDado)? BlocoCodigo "FIM" ;
Funcao             ::= "FUNCAO" IDENTIFICADOR "(" Parametros? ")" ":" TipoDado ("=" Expressao | Comando* "FIM_FUNCAO") ;
Parametros         ::= Parametro ("," Parametro)* ;
Parametro          ::= IDENTIFICADOR ":" TipoDado ;

BlocoCodigo        ::= "INICIO" Comando* ;

Comando            ::= DeclaracaoVariavel
                     | Atribuicao
                     | Condicional
                     | LacoEnquanto
                     | LacoPara
                     | LacoVetorizado
                     | BlocoMemoria
                     | RetornoResultado
                     | FalhaResultado
                     | CasoResultadoComando
                     | ChamadaProcedimento ;

DeclaracaoVariavel ::= "VARIAVEL" IDENTIFICADOR (":" TipoDado)? "<-" Expressao ;
Atribuicao         ::= (IDENTIFICADOR | AcessoMembro) "<-" Expressao ;

Condicional        ::= "SE" Expressao "ENTAO" Comando* ("SENAO" Comando*)? "FIM_SE" ;
LacoEnquanto       ::= "ENQUANTO" Expressao "FACA" Comando* "FIM_ENQUANTO" ;
LacoPara           ::= "PARA" IDENTIFICADOR "DE" Expressao "ATE" Expressao ("PASSO" Expressao)? "FACA" Comando* "FIM_PARA" ;

LacoVetorizado     ::= "VETORIZAR_PARA" IDENTIFICADOR ("EM" Expressao | "DE" Expressao "ATE" Expressao) "PASSO_SIMD" NUMERO Comando* ("FIM_VETORIZAR" | "FIM_PARA") ;
BlocoMemoria       ::= "USAR_BLOCO_MEMORIA" STRING_LITERAL ("," Expressao)? "FACA" Comando* "FIM_BLOCO_MEMORIA" ;

RetornoResultado   ::= "RETORNAR" ("RESULTADO" "(" Expressao ")" | Expressao)? ;
FalhaResultado     ::= "FALHAR_COM" "(" Expressao ")" ;

CasoResultadoComando ::= ("ESCOLHA" | "CASO_RESULTADO") Expressao
                         ("CASO")? "SUCESSO" "(" IDENTIFICADOR ")" "->" Comando*
                         ("CASO")? ("FALHA" | "ERRO") "(" IDENTIFICADOR ")" "->" Comando*
                       ("FIM_ESCOLHA" | "FIM_CASO") ;
```

---

## 6. Tipos e Expressões

```ebnf
TipoDado          ::= "INTEIRO"
                    | "DECIMAL" "(" NUMERO "," NUMERO ")"
                    | "MONETARIO" "(" IDENTIFICADOR ")"
                    | "TEXTO"
                    | "LOGICO"
                    | "UUID"
                    | "DATA"
                    | "DATA_HORA"
                    | "FATIA" "[" TipoDado "]"
                    | "RESULTADO" "[" TipoDado "," TipoDado "]"
                    | IDENTIFICADOR ;

Expressao         ::= TermoOperador ;
TermoOperador     ::= TermoRelacional ( ("E" | "OU") TermoRelacional )* ;
TermoRelacional   ::= TermoAritmetico ( (">" | "<" | ">=" | "<=" | "==" | "!=") TermoAritmetico )? ;
TermoAritmetico   ::= Fator ( ("+" | "-") Fator )* ;
Fator             ::= Primario ( ("*" | "/") Primario )* ;

Primario          ::= LITERAL_NUMERICO
                    | LITERAL_MONETARIO
                    | STRING_LITERAL
                    | "VERDADEIRO" | "FALSO" | "NULO"
                    | IDENTIFICADOR
                    | AcessoMembro
                    | ChamadaFuncao
                    | "(" Expressao ")" ;

AcessoMembro      ::= IDENTIFICADOR ("." IDENTIFICADOR | "[" Expressao "]")+ ;
ChamadaFuncao     ::= IDENTIFICADOR "(" (Expressao ("," Expressao)*)? ")" ;
```
