# Formal Grammar Specification — THZ-LANG (EBNF)

This specification defines the formal Extended Backus-Naur Form (EBNF) grammar for the **THZ-LANG** programming language for both `pt-BR` and `en-US` dialects.

---

## 1. Dialect Header & Program Unit

```ebnf
CompilationUnit ::= [ DialectDirective ] ModuleDeclaration ( ImportDeclaration | MetadataDeclaration | StructureDeclaration | EnumDeclaration | BusinessRuleDeclaration | ProcedureDeclaration )* ModuleTerminator ;

DialectDirective ::= ( "LANGUAGE" ":" "en-US" ) | ( "LINGUAGEM" ":" "pt-BR" ) ;

ModuleDeclaration ::= ( "PROGRAM" | "PROGRAMA" ) [ "VISUAL" | "BUSINESS" | "NEGOCIO" | "ARCHITECTURE" | "ARQUITETURA" ] Identifier
                    | ( "LIBRARY" | "BIBLIOTECA" ) Identifier
                    | ( "EXTENSION" | "EXTENSAO" ) Identifier
                    | ( "TOOL" | "FERRAMENTA" ) Identifier
                    | ( "TEST" | "TESTE" ) Identifier
                    | ( "SCREEN" | "TELA" ) Identifier
                    | ( "DATA_PIPELINE" | "PIPELINE_DADOS" ) Identifier ;

ModuleTerminator ::= "END_PROGRAM" | "FIM_PROGRAMA"
                   | "END_LIBRARY" | "FIM_BIBLIOTECA"
                   | "END_EXTENSION" | "FIM_EXTENSAO"
                   | "END_TOOL" | "FIM_FERRAMENTA"
                   | "END_TEST" | "FIM_TESTE"
                   | "END_SCREEN" | "FIM_TELA"
                   | "END_PIPELINE" | "FIM_PIPELINE" ;
```

---

## 2. Living Architecture Metadata

```ebnf
MetadataDeclaration ::= ( "ARCHITECTURE_METADATA" | "METADADOS_ARQUITETURA" ) MetadataField* ( "END_METADATA" | "FIM_METADADOS" ) ;

MetadataField ::= ( "DOMAIN" | "DOMINIO" ) ":" StringLiteral
                | ( "SUBDOMAIN" | "SUBDOMINIO" ) ":" StringLiteral
                | ( "LAYER" | "CAMADA" ) ":" StringLiteral
                | ( "VERSION" | "VERSAO" ) ":" StringLiteral
                | ( "AUTHOR" | "AUTOR" ) ":" StringLiteral
                | ( "MAX_LATENCY_SLO" | "SLO_LATENCIA_MAXIMA" ) ":" StringLiteral
                | ( "COMPLIANCE" | "CONFORMIDADE" ) ":" StringLiteral ( "," StringLiteral )* ;
```

---

## 3. Business Rules & Design by Contract

```ebnf
BusinessRuleDeclaration ::= ( "BUSINESS_RULE" | "REGRA_NEGOCIO" ) Identifier RuleProperty* [ InputContract ] [ OutputContract ] OperationDeclaration* ( "END_BUSINESS_RULE" | "FIM_REGRA_NEGOCIO" ) ;

InputContract  ::= ( "INPUT_CONTRACT" | "CONTRATO_ENTRADA" ) ( ( "REQUIRES" | "EXIGE" ) Expression )* ( "END_INPUT_CONTRACT" | "FIM_CONTRATO_ENTRADA" ) ;
OutputContract ::= ( "OUTPUT_CONTRACT" | "CONTRATO_SAIDA" ) ( ( "ENSURES" | "GARANTE" ) Expression )* ( "END_OUTPUT_CONTRACT" | "FIM_CONTRATO_SAIDA" ) ;

OperationDeclaration ::= ( "OPERATION" | "OPERACAO" ) [ "IDEMPOTENT" | "IDEMPOTENTE" ] Identifier "(" [ ParameterList ] ")" ":" DataType ( "BEGIN" | "INICIO" ) Statement* ( "END" | "FIM" ) ;
```

---

## 4. Control Flow & Memory Engineering

```ebnf
Statement ::= VariableDeclaration
            | Assignment
            | IfStatement
            | WhileStatement
            | ForStatement
            | VectorizeForStatement
            | MemoryBlockStatement
            | ReturnStatement
            | PrintStatement
            | FailStatement ;

VectorizeForStatement ::= ( "VECTORIZE_FOR" | "VETORIZAR_PARA" ) Identifier ( "IN" | "EM" ) QualifiedPath [ ( "SIMD_STEP" | "PASSO_SIMD" ) IntegerLiteral ] Statement* ( "END_FOR" | "FIM_PARA" ) ;

MemoryBlockStatement  ::= ( "USE_MEMORY_BLOCK" | "USAR_BLOCO_MEMORIA" ) Identifier Statement* ( "END_MEMORY_BLOCK" | "FIM_BLOCO_MEMORIA" ) ;
```
