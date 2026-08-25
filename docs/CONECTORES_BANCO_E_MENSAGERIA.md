# Conectores Universais de Banco de Dados e Mensageria — THZ-LANG v3.0.0

Este documento apresenta a arquitetura, configuração e uso dos **Conectores Universais de Banco de Dados** (Raw SQL + JPA/ORM + Busca Vetorial) e **Mensageria Distribuída** (RabbitMQ, Kafka, AWS SQS/SNS, Embutido) no **THZ-LANG**.

---

## 1. Manifesto de Configuração Centralizada (`thz.config.json`)

Seguindo os padrões consagrados da indústria (`tsconfig.json`, `package.json`, `Cargo.toml`), o THZ-LANG busca automaticamente na raiz do projeto o arquivo `thz.config.json` (ou `thz.json`).

### Criando o manifesto via CLI:
```bash
thz init
```

### Exemplo de `thz.config.json`:
```json
{
  "projeto": {
    "nome": "SistemaCorporativo",
    "versao": "3.0.0",
    "autor": "Lucas Thomaz",
    "dialeto": "pt-BR",
    "descricao": "Sistema de Liquidação Financeira e IA On-Device"
  },
  "banco": {
    "driver": "auto",
    "url": "jdbc:sqlite:dados/app.db",
    "usuario": "",
    "senha": "",
    "poolMin": 2,
    "poolMax": 10,
    "autoMigracao": true,
    "vetorial": "embutido"
  },
  "mensageria": {
    "driver": "auto",
    "url": "auto",
    "host": "localhost",
    "porta": 5672,
    "topicoPadrao": "eventos.sistema",
    "autoCriarFilas": true
  },
  "ia": {
    "motorEmbeddings": "local-fnv1a",
    "dimensaoVetor": 128,
    "armazenamentoVetorial": "sqlite-vec"
  },
  "governanca": {
    "modoEstrito": false,
    "sloLatencia": "15ms",
    "conformidade": [
      "ISO-IEC-10967",
      "LGPD-Art7"
    ]
  }
}
```

---

## 2. Conectores de Banco de Dados (`BANCO.*`)

O THZ-LANG oferece suporte completo para dois paradigmas integrados: **Persistência JPA/ORM** e **Raw SQL Puro de Alta Performance**.

### 2.1 Persistência Estilo JPA/ORM:

* `BANCO.criarTabela(nomeTabela, definicaoCampos)` — DDL Dinâmica com auto-migração.
* `BANCO.salvar(nomeTabela, entidade)` — Executa `INSERT` ou `UPDATE` de forma automática inspecionando a chave primária.
* `BANCO.buscarPorId(nomeTabela, id)` — Localiza e retorna a estrutura/registro pelo ID.
* `BANCO.removerPorId(nomeTabela, id)` — Deleta o registro pelo ID.
* `BANCO.driverAtivo()` — Retorna o driver em execução (`"SQLITE"`, `"POSTGRES"`, `"MYSQL"`, `"GENERIC_JDBC"`).

#### Exemplo:
```thz
ESTRUTURA Cliente
    id: TEXTO
    nome: TEXTO
    renda: DECIMAL
FIM_ESTRUTURA

# Criação dinâmica e salvamento
BANCO.criarTabela("clientes", "id:TEXT PRIMARY KEY, nome:TEXT, renda:DECIMAL")
VARIAVEL c : Cliente <- CRIAR Cliente(id: "CLI-01", nome: "Empresa Alpha", renda: 45000.00)
BANCO.salvar("clientes", c)

# Busca por ID
VARIAVEL buscado : Cliente <- BANCO.buscarPorId("clientes", "CLI-01")
EXIBA "Cliente: " + buscado.nome
```

---

### 2.2 Raw SQL Puro e Transações:

* `BANCO.consultar(sql, parametros)` — Executa queries `SELECT` com parâmetros posicionais seguros contra SQL Injection. Retorna `FATIA[REGISTRO]`.
* `BANCO.consultarEm(conexaoNome, sql, parametros)` — Consulta em uma conexão específica (multi-banco).
* `BANCO.consultarValor(sql, parametros)` — Retorna diretamente um valor escalar único (`COUNT`, `SUM`, `MAX`).
* `BANCO.executar(sql, parametros)` — Executa `INSERT`, `UPDATE`, `DELETE` ou `ALTER TABLE`. Retorna linhas afetadas (`INTEIRO`).
* `BANCO.executarScript(sqlMultiplo)` — Executa lote de instruções DDL/DML separadas por ponto e vírgula.
* `BANCO.iniciarTransacao()`, `BANCO.confirmarTransacao()`, `BANCO.cancelarTransacao()` — Controle transacional explícito (Commit/Rollback).

#### Exemplo:
```thz
BANCO.iniciarTransacao()

BANCO.executar("UPDATE contas SET saldo = saldo - ? WHERE id = ?", [500.00, "C-01"])
BANCO.executar("UPDATE contas SET saldo = saldo + ? WHERE id = ?", [500.00, "C-02"])

BANCO.confirmarTransacao()

VARIAVEL total : INTEIRO <- BANCO.consultarValor("SELECT COUNT(*) FROM contas WHERE saldo > ?", [0.00])
EXIBA "Total de contas positivas: " + TEXTO.deInteiro(total)
```

---

### 2.3 Busca Semântica Vetorial KNN (`BANCO.consultarVetorial`):

Realiza busca por similaridade de cosseno diretamente nas linhas do banco utilizando os embeddings gerados pelo motor `IA.embedding`:

```thz
VARIAVEL vetorBusca : TEXTO <- IA.embedding("Consultoria em planejamento tributário")
VARIAVEL maisAderentes : FATIA[REGISTRO] <- BANCO.consultarVetorial("produtos", "vetorInteresse", vetorBusca, 3)

PARA item EM maisAderentes
    EXIBA "Item: " + item.nome + " | Similaridade: " + TEXTO.deDecimal(item._similaridade)
FIM_PARA
```

---

## 3. Conectores de Mensageria Distribuída (`MENSAGERIA.*`)

A bridge `ThzMessagingBridge` conecta seu código THZ a brokers corporativos sem alterar a sintaxe do programa.

### Drivers Suportados:
1. **`EMBUTIDO`**: Barramento em memória de alta performance baseado em *RingBuffer* lock-free e Virtual Threads do Java 25.
2. **`RABBITMQ`**: AMQP / REST Bridge com detecção automática na porta `5672` ou URL customizada `amqp://...`.
3. **`KAFKA`**: Apache Kafka com suporte a brokers `localhost:9092` ou Kafka REST Proxy.
4. **`AWS_SQS`**: Filas gerenciadas AWS SQS e compatibilidade com LocalStack na porta `4566`.
5. **`AWS_SNS`**: Tópicos Pub/Sub AWS SNS.
6. **`AUTO`**: Sondagem automática de serviços locais. Se nenhum broker externo for detectado, ativa o motor embutido com zero latência.

### Operações:
* `MENSAGERIA.publicar(topico, mensagem)` — Publica evento no broker ativo.
* `MENSAGERIA.consumir(topico, timeoutMs)` — Consome a próxima mensagem da fila.
* `MENSAGERIA.tamanhoFila(topico)` — Retorna a contagem de mensagens pendentes.
* `MENSAGERIA.driverAtivo()` — Retorna o nome do driver conectado (`"EMBUTIDO"`, `"RABBITMQ"`, `"KAFKA"`, etc.).
* `MENSAGERIA.statusConexao()` — Retorna `VERDADEIRO` se o broker estiver operante.
* `MENSAGERIA.conectar(driver, url)` — Troca dinamicamente o broker de mensageria em tempo de execução.

#### Exemplo:
```thz
MENSAGERIA.conectar("rabbitmq", "amqp://localhost:5672")
EXIBA "Broker ativo: " + MENSAGERIA.driverAtivo()

VARIAVEL pedido : PedidoVenda <- CRIAR PedidoVenda(id: "PED-10", valor: 350.00)
MENSAGERIA.publicar("pedidos.aprovados", pedido)

VARIAVEL recebido : PedidoVenda <- MENSAGERIA.consumir("pedidos.aprovados", 100)
EXIBA "Pedido recebido: " + recebido.id
```

---

## 6. Histórico de Commits Atômicos & Rastreabilidade

A implementação desta suíte seguiu o padrão de **Commits Atômicos e Determinísticos**:

1. **`feat(exemplos): adicionar suite de 25 exemplos canonicos para versoes v2.6 a v3.0 e scripts de compilacao em lote`**
   - Criação de 25 programas demonstrativos canônicos cobrindo IA/ML, LINQ, EDA, Contratos/DAP e WASM/Rust (`exemplos/novos_recursos/`).
   - Scripts de automação `scripts/compile-all-examples.ps1` e `scripts/compile-all-examples.sh`.

2. **`feat(config): implementar manifesto de configuracao thz.config.json e comando thz init`**
   - Parser determinístico `ThzProjectConfig.java` para `thz.config.json`.
   - Comando `thz init` e auto-detecção em tempo de execução no `ThzCli.java`.
   - Testes unitários `ThzProjectConfigTest.java`.

3. **`feat(mensageria): implementar conectores universais para RabbitMQ, Kafka, AWS SQS/SNS e barramento reativo embutido`**
   - Intermediador universal `ThzMessagingBridge.java` com detecção inteligente de portas e contingência instantânea.
   - Funções expostas `MENSAGERIA.*` na `BibliotecaPadrao.java`.
   - Testes unitários `ThzMessagingBridgeTest.java` e exemplo canônico `exemplos/mensageria_conectores_hibridos.thz`.

4. **`feat(db): implementar bridge de banco universal com camada JPA/ORM, Raw SQL, transacoes e busca vetorial KNN`**
   - Abstração ORM/JPA (`salvar`, `buscarPorId`, `removerPorId`, `criarTabela`) em `ThzDatabaseBridge.java`.
   - Raw SQL com parametrização anti-injection, transações explícitas (Commit/Rollback) e consultas escalares em `ThzDb.java`.
   - Busca Semântica Vetorial KNN com similaridade de cosseno SIMD em `BANCO.consultarVetorial`.
   - Testes com arquivo físico real `.db` em disco `ThzSqliteFisicoVetorialTest.java` e `ThzDatabaseBridgeTest.java`.
   - Exemplos canônicos `exemplos/banco_jpa_orm_vetorial.thz`, `exemplos/banco_rawsql_avancado.thz` e `exemplos/sqlite_fisico_e_vetorial_real.thz`.

5. **`docs(db,mensageria): registrar documentacao de conectores universais, historico de commits atomicos e changelog v3.0.0`**
   - Criação do tratado oficial `docs/CONECTORES_BANCO_E_MENSAGERIA.md`.
   - Atualização do `README.md`, `CHANGELOG.md` e `.gitignore`.

