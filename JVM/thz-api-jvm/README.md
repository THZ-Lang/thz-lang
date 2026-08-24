# thz-api-jvm — REST API Java

API REST do THZ-LANG, escrita em Spring Boot 4.1.1. Expõe o engine `thz-core-jvm` via HTTP para consumo do Playground Web e de clientes externos.

## Funcionalidades

| Endpoint | Método | Descrição |
|----------|--------|-----------|
| `/api/health` | GET | Status do engine |
| `/api/analyze` | POST | Análise completa (léxico + sintático + semântico + governança) |
| `/api/hover` | POST | Hover info (tipo, linha, coluna) |
| `/api/ast` | POST | AST completa em JSON |
| `/api/format` | POST | Formatação canônica |
| `/api/doc` | POST | Geração de documentação |
| `/api/audit` | POST | Auditoria de governança |
| `/api/ir` | POST | Geração de código intermediário (IR) |
| `/api/simd` | POST | Validação SIMD |

## Compilação

```bash
cd JVM/thz-api-jvm
./gradlew bootJar
```

## Execução

```bash
./gradlew bootRun
# ou
java -jar build/libs/thz-api-jvm-2.3.3.jar
```

A API roda em `http://localhost:8080` por padrão.

## Exemplo de Uso

```bash
# Health check
curl http://localhost:8080/api/health

# Análise
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"fonte": "PROGRAMA teste FIM_PROGRAMA"}'
```

## Configuração

Edite `src/main/resources/application.yml` para:
- `server.port` — porta HTTP (padrão: 8080)
- `spring.profiles.active` — perfil de configuração
- CORS: configurado em `CorsConfig.java`
