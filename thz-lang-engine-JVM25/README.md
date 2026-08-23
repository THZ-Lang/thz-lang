# THZ-LANG Engine — JVM25

Porto **Java 25 + Maven** do motor THZ-LANG v2.3 (interpretador tree-walking), com paridade comportamental com o motor original em Node/TypeScript.

## Visão Geral
Este é o motor de execução para THZ-LANG, focado em alta performance na JVM.

### Diretrizes Técnicas
- **Aritmética Monetária/Fiscal**: Uso obrigatório de `DecimalFixo` (BigInteger escalado); proibido IEEE 754.
- **Gerenciamento de Memória**: Estruturado para uso de `ArenaMemoria` (O(1)).
- **Contratos**: Validação de `EXIGE`/`GARANTE`/`INVARIANTE` via `AnalisadorSemantico` e `InterpretadorThz`.

## Requisitos
- JDK 25 (OpenJDK 25)
- Maven 3.9+

## Build e Execução
O projeto utiliza **Maven** como sistema de build canônico. O suporte ao Gradle é previsto como alternativa futura (`build.gradle.kts` placeholder disponível).

### Comandos de Build
```bash
mvn clean compile      # Compila o projeto
mvn test               # Executa testes (JUnit 5)
mvn package            # Gera o jar em target\thz-jvm25-2.3.0.jar
mvn verify             # Build completo + testes
```

### Comandos de Execução
```bash
# Sem flags especiais (nenhum recurso preview é usado)
java -jar target\thz-jvm25-2.3.0.jar check exemplos\agenda.thz
java -jar target\thz-jvm25-2.3.0.jar run   exemplos\agenda.thz
java -jar target\thz-jvm25-2.3.0.jar fmt   exemplos\agenda.thz
java -jar target\thz-jvm25-2.3.0.jar repl
java -jar target\thz-jvm25-2.3.0.jar gui    # IDE Swing (galeria de exemplos no menu)
```

### Importar na IDE (IntelliJ)
1. **File → Open** e selecione apenas o `pom.xml` desta pasta (Open as Project).
2. Garanta **Project SDK = 25** (Settings → Project) — sem flags extras.
3. Não mantenha `build.gradle.kts` ativo junto com o Maven: dois sistemas sobre a
   mesma raiz fazem o compilador da IDE enxergar módulos parciais
   (`package thz.lang.ast does not exist`). O Gradle fica desativado em
   `build.gradle.kts.desativado` até a trilha futura.

## Variáveis de Ambiente
> TODO: Documentar variáveis de ambiente necessárias, se houver.

## Testes
O projeto garante paridade com o motor TypeScript original.
- Testes unitários e de integração: `mvn test`
- Suíte atual: `src/test/java/thz/lang/ParidadeTest.java` (8 casos, inclui galeria
  de exemplos e BOM) + `GuiPaletaTest.java` (3 casos de paleta).

## Estrutura do Projeto
```
src/main/java/thz/lang/
├── ast           # Árvore de Sintaxe Abstrata
├── cli           # Ponto de entrada (ThzCli.java)
├── interpretador # Interpretador tree-walking
├── lexico        # Analisador léxico
├── sintatico     # Analisador sintático
├── semantico     # Analisador semântico
├── runtime       # Ambiente de execução
├── formato       # Utilitários de formatação (ex: JsonEscritor)
├── repl          # REPL
└── diagnosticos  # Sistema de diagnósticos
```

## Licença
> TODO: Especificar a licença do projeto.

## RoadMap
Núcleo v2.3: check/run/fmt/repl + stdlib — IR/SIMD/docgen/audit deixados como trilha futura.
