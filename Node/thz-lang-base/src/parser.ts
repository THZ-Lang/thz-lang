import {
  Token,
  TokenType,
  ProgramaAST,
  MetadadosArquiteturaAST,
  EstruturaAST,
  RegraNegocioAST,
  ClausulaContratoAST,
  OperacaoAST,
  ParametroOperacaoAST,
  ComandoAST,
  ExprAST,
  OperadorBinario,
  EnumeracaoAST,
  InvarianteAST
} from './types.js';
import { ehPalavraReservada } from './keywords.js';

export class ThzParser {
  private current = 0;

  constructor(private tokens: Token[]) {}

  public parse(): ProgramaAST {
    const ast: Partial<ProgramaAST> = {
      estruturas: [],
      enumeracoes: [],
      regras: [],
      procedimentos: []
    };

    // Pragma opcional de compatibilidade: VERSAO_LINGUAGEM "2.2"
    if (this.match(TokenType.VERSAO_LINGUAGEM)) {
      ast.versaoLinguagem = this.consume(TokenType.STRING_LITERAL, "Esperada a versão da linguagem entre aspas após 'VERSAO_LINGUAGEM'.").value;
    }

    this.consume(TokenType.PROGRAMA, "Esperado 'PROGRAMA' no início do arquivo.");
    ast.nome = this.consumeIdentificador('Esperado o nome do programa.').value;

    while (!this.check(TokenType.FIM_PROGRAMA) && !this.isAtEnd()) {
      if (this.match(TokenType.METADADOS_ARQUITETURA)) {
        ast.metadados = this.parseMetadados();
      } else if (this.match(TokenType.ESTRUTURA)) {
        ast.estruturas!.push(this.parseEstrutura());
      } else if (this.match(TokenType.ENUMERACAO)) {
        ast.enumeracoes!.push(this.parseEnumeracao());
      } else if (this.match(TokenType.REGRA_NEGOCIO)) {
        ast.regras!.push(this.parseRegraNegocio());
      } else if (this.match(TokenType.PROCEDIMENTO)) {
        ast.procedimentos!.push(this.parseProcedimento());
      } else {
        this.erroDeclaracaoInvalida();
      }
    }

    this.consume(TokenType.FIM_PROGRAMA, "Esperado 'FIM_PROGRAMA' encerrando o programa.");

    return ast as ProgramaAST;
  }

  private erroDeclaracaoInvalida(): never {
    const token = this.peek();
    throw new Error('[Erro Sintático][Linha ' + token.line + ':' + token.column + "] Declaração não reconhecida no nível do programa. Esperados 'METADADOS_ARQUITETURA', 'ESTRUTURA', 'ENUMERACAO', 'REGRA_NEGOCIO', 'PROCEDIMENTO' ou 'FIM_PROGRAMA'. (Encontrado: '" + token.value + "')");
  }

  /* ============================================================
   * METADADOS
   * ============================================================ */

  private parseMetadados(): MetadadosArquiteturaAST {
    const meta: Partial<MetadadosArquiteturaAST> = { conformidade: [] };
    while (!this.match(TokenType.FIM_METADADOS) && !this.isAtEnd()) {
      const chave = this.consumeIdentificador('Esperada chave de metadado.').value;
      this.consume(TokenType.DOIS_PONTOS, "Esperado ':' após identificador de metadado.");

      if (chave === 'CONFORMIDADE') {
        meta.conformidade!.push(this.consume(TokenType.STRING_LITERAL, 'Esperada regra de conformidade.').value);
        while (this.match(TokenType.VIRGULA)) {
          meta.conformidade!.push(this.consume(TokenType.STRING_LITERAL, 'Esperado próximo valor.').value);
        }
      } else {
        const valor = this.consume(TokenType.STRING_LITERAL, 'Esperado valor textual do metadado.').value;
        if (chave === 'DOMINIO') meta.dominio = valor;
        if (chave === 'SUBDOMINIO') meta.subdominio = valor;
        if (chave === 'CAMADA') meta.camada = valor;
        if (chave === 'VERSAO') meta.versao = valor;
        if (chave === 'AUTOR') meta.autor = valor;
        if (chave === 'SLO_LATENCIA_MAXIMA') meta.sloLatencia = valor;
      }
    }
    return meta as MetadadosArquiteturaAST;
  }

  /* ============================================================
   * ESTRUTURA
   * ============================================================ */

  private parseEstrutura(): EstruturaAST {
    const nome = this.consumeIdentificador('Esperado nome da estrutura.').value;
    let layoutColunar = false;
    if (this.match(TokenType.LAYOUT_COLUNAR)) {
      layoutColunar = true;
    }

    const campos: { nome: string; tipo: string }[] = [];
    const invariantes: InvarianteAST[] = [];
    while (!this.match(TokenType.FIM_ESTRUTURA) && !this.isAtEnd()) {
      if (this.check(TokenType.INVARIANTE)) {
        invariantes.push(this.parseInvariante());
        continue;
      }
      const campoNome = this.consumeIdentificador('Esperado nome do campo.').value;
      this.consume(TokenType.DOIS_PONTOS, "Esperado ':' após o nome do campo.");
      campos.push({ nome: campoNome, tipo: this.parseTipoDado() });
    }

    return { nome, layoutColunar, campos, invariantes };
  }

  /**
   * ENUMERACAO Nome
   *     MEMBRO_A
   * FIM_ENUMERACAO
   */
  private parseEnumeracao(): EnumeracaoAST {
    const nome = this.consumeIdentificador('Esperado nome da enumeração.').value;
    const membros: string[] = [];
    while (!this.match(TokenType.FIM_ENUMERACAO) && !this.isAtEnd()) {
      membros.push(this.consumeIdentificador('Esperado membro da enumeração.').value);
    }
    return { nome, membros };
  }

  /** INVARIANTE <expressão> — cláusula viva dentro de ESTRUTURA. */
  private parseInvariante(): InvarianteAST {
    const inicio = this.consume(TokenType.INVARIANTE, "Esperado 'INVARIANTE'.");
    const expressao = this.parseExpressao();
    return {
      expressao,
      textoCanonico: textoCanonicoDe(expressao),
      linha: inicio.line,
      coluna: inicio.column
    };
  }

  /**
   * Tipo de dado canônico: IDENTIFICADOR [(args)] [[T]]
   * Preserva verbatim (ex.: 'DECIMAL(12,4)', 'FATIA[ItemFatura]').
   */
  private parseTipoDado(): string {
    let tipo = this.consumeIdentificador('Esperado tipo do dado.').value;
    if (this.match(TokenType.ABRE_PARENTESE)) {
      let args = '';
      while (!this.check(TokenType.FECHA_PARENTESE) && !this.isAtEnd()) {
        args += this.peek().value;
        this.advance();
      }
      this.consume(TokenType.FECHA_PARENTESE, "Esperado ')' após parâmetros do tipo.");
      tipo += '(' + args + ')';
    }
    if (this.match(TokenType.ABRE_COLCHETE)) {
      if (tipo === 'RESULTADO') {
        // RESULTADO[T, E]: canal de sucesso e canal de erro.
        const sucesso = this.parseTipoDado();
        this.consume(TokenType.VIRGULA, "Esperado ',' separando os canais de RESULTADO[T, E].");
        const erro = this.parseTipoDado();
        this.consume(TokenType.FECHA_COLCHETE, "Esperado ']' após canais de RESULTADO.");
        return tipo + '[' + sucesso + ', ' + erro + ']';
      }
      tipo += '[' + this.consumeIdentificador('Esperado tipo interno do fatiamento.').value + ']';
      this.consume(TokenType.FECHA_COLCHETE, "Esperado ']' após tipo interno.");
    }
    return tipo;
  }

  /* ============================================================
   * REGRA DE NEGÓCIO
   * ============================================================ */

  private parseRegraNegocio(): RegraNegocioAST {
    const regra: RegraNegocioAST = {
      nome: this.consumeIdentificador('Esperado nome da regra.').value,
      clausulasEntrada: [],
      clausulasSaida: [],
      operacoes: []
    };

    while (!this.check(TokenType.FIM_REGRA_NEGOCIO) && !this.isAtEnd()) {
      if (this.peek().value === 'IDENTIFICADOR_REGRA' && this.peek().type === TokenType.IDENTIFICADOR) {
        this.advance();
        this.consume(TokenType.DOIS_PONTOS, "Esperado ':' após 'IDENTIFICADOR_REGRA'.");
        regra.identificador = this.consume(TokenType.STRING_LITERAL, 'Esperado ID da regra.').value;
      } else if (this.peek().value === 'RASTREIO_REQUISITO' && this.peek().type === TokenType.IDENTIFICADOR) {
        this.advance();
        this.consume(TokenType.DOIS_PONTOS, "Esperado ':' após 'RASTREIO_REQUISITO'.");
        regra.rastreioRequisito = this.consume(TokenType.STRING_LITERAL, 'Esperado requisito de rastreio.').value;
      } else if (this.peek().value === 'DESCRICAO' && this.peek().type === TokenType.IDENTIFICADOR) {
        this.advance();
        this.consume(TokenType.DOIS_PONTOS, "Esperado ':' após 'DESCRICAO'.");
        regra.descricao = this.consume(TokenType.STRING_LITERAL, 'Esperada descrição.').value;
      } else if (this.match(TokenType.CONTRATO_ENTRADA)) {
        while (!this.check(TokenType.FIM_CONTRATO_ENTRADA) && !this.isAtEnd()) {
          regra.clausulasEntrada.push(this.parseClausulaContrato(TokenType.EXIGE));
        }
        this.consume(TokenType.FIM_CONTRATO_ENTRADA, "Esperado 'FIM_CONTRATO_ENTRADA' encerrando o bloco de pré-condições.");
      } else if (this.match(TokenType.CONTRATO_SAIDA)) {
        while (!this.check(TokenType.FIM_CONTRATO_SAIDA) && !this.isAtEnd()) {
          regra.clausulasSaida.push(this.parseClausulaContrato(TokenType.GARANTE));
        }
        this.consume(TokenType.FIM_CONTRATO_SAIDA, "Esperado 'FIM_CONTRATO_SAIDA' encerrando o bloco de pós-condições.");
      } else if (this.match(TokenType.OPERACAO)) {
        regra.operacoes.push(this.parseOperacao());
      } else {
        const token = this.peek();
        throw new Error('[Erro Sintático][Linha ' + token.line + ':' + token.column + "] Elemento não reconhecido dentro de 'REGRA_NEGOCIO'. Esperados 'IDENTIFICADOR_REGRA', 'RASTREIO_REQUISITO', 'DESCRICAO', 'CONTRATO_ENTRADA', 'CONTRATO_SAIDA', 'OPERACAO' ou 'FIM_REGRA_NEGOCIO'. (Encontrado: '" + token.value + "')");
      }
    }

    this.consume(TokenType.FIM_REGRA_NEGOCIO, "Esperado 'FIM_REGRA_NEGOCIO'.");
    return regra;
  }

  private parseClausulaContrato(tipo: TokenType): ClausulaContratoAST {
    const inicio = this.consume(tipo, "Esperada cláusula '" + (tipo === TokenType.EXIGE ? 'EXIGE' : 'GARANTE') + "' dentro do bloco de contrato.");
    const expressao = this.parseExpressao();
    return {
      tipoClausula: inicio.value as 'EXIGE' | 'GARANTE',
      expressao,
      textoCanonico: textoCanonicoDe(expressao),
      linha: inicio.line,
      coluna: inicio.column
    };
  }

  private parseOperacao(): OperacaoAST {
    const operacao: OperacaoAST = {
      nome: this.consumeIdentificador('Esperado nome da operação.').value,
      parametros: [],
      tipoRetorno: '',
      corpo: []
    };

    this.consume(TokenType.ABRE_PARENTESE, "Esperado '(' na assinatura da operação.");
    if (!this.check(TokenType.FECHA_PARENTESE)) {
      do {
        const parametro: ParametroOperacaoAST = {
          nome: this.consumeIdentificador('Esperado nome do parâmetro.').value,
          tipo: ''
        };
        this.consume(TokenType.DOIS_PONTOS, "Esperado ':' após nome do parâmetro.");
        parametro.tipo = this.parseTipoDado();
        operacao.parametros.push(parametro);
      } while (this.match(TokenType.VIRGULA));
    }
    this.consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando os parâmetros.");
    this.consume(TokenType.DOIS_PONTOS, "Esperado ':' antes do tipo de retorno.");
    operacao.tipoRetorno = this.parseTipoDado();

    if (this.match(TokenType.INICIO)) {
      operacao.corpo = this.parseBlocoComandos(TokenType.FIM);
      this.consume(TokenType.FIM, "Esperado 'FIM' encerrando o corpo da operação.");
    }

    return operacao;
  }

  private parseProcedimento(): import('./types.js').ProcedimentoAST {
    const nome = this.consumeIdentificador('Esperado nome do procedimento.').value;
    const parametros: ParametroOperacaoAST[] = [];
    this.consume(TokenType.ABRE_PARENTESE, "Esperado '(' na assinatura do procedimento.");
    if (!this.check(TokenType.FECHA_PARENTESE)) {
      do {
        const p: ParametroOperacaoAST = {
          nome: this.consumeIdentificador('Esperado nome do parâmetro.').value,
          tipo: ''
        };
        this.consume(TokenType.DOIS_PONTOS, "Esperado ':' após nome do parâmetro.");
        p.tipo = this.parseTipoDado();
        parametros.push(p);
      } while (this.match(TokenType.VIRGULA));
    }
    this.consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando os parâmetros.");
    // Corpo opcional INICIO ... FIM
    let corpo: ComandoAST[] = [];
    if (this.match(TokenType.INICIO)) {
      corpo = this.parseBlocoComandos(TokenType.FIM);
      this.consume(TokenType.FIM, "Esperado 'FIM' encerrando o corpo do procedimento.");
    }
    return { nome, parametros, corpo };
  }

  /* ============================================================
   * COMANDOS
   * ============================================================ */

  /** Consome comandos até qualquer um dos terminadores informados (sem consumi-los). */
  private parseBlocoComandos(...terminadores: TokenType[]): ComandoAST[] {
    const comandos: ComandoAST[] = [];
    while (!this.isAtEnd() && !terminadores.some((t) => this.check(t))) {
      comandos.push(this.parseComando());
    }
    return comandos;
  }

  private parseComando(): ComandoAST {
    const token = this.peek();

    switch (token.type) {
      case TokenType.VARIAVEL: {
        this.advance();
        const nome = this.consumeIdentificador('Esperado nome da variável.').value;
        this.consume(TokenType.DOIS_PONTOS, "Esperado ':' após o nome da variável.");
        const tipoDado = this.parseTipoDado();
        this.consume(TokenType.SETA_ATRIBUICAO, "Esperado '<-' na inicialização da variável '" + nome + "'.");
        const inicializacao = this.parseExpressao();
        return { tipoComando: 'DECL_VARIAVEL', nome, tipoDado, inicializacao, linha: token.line, coluna: token.column };
      }

      case TokenType.SE: {
        this.advance();
        const condicao = this.parseExpressao();
        const entao = this.parseBlocoComandos(TokenType.FIM_SE, TokenType.SENAO);
        let senao: ComandoAST[] = [];
        if (this.match(TokenType.SENAO)) {
          senao = this.parseBlocoComandos(TokenType.FIM_SE);
        }
        this.consume(TokenType.FIM_SE, "Esperado 'FIM_SE' encerrando o comando 'SE'.");
        return { tipoComando: 'SE', condicao, entao, senao, linha: token.line, coluna: token.column };
      }

      case TokenType.ENQUANTO: {
        this.advance();
        const condicao = this.parseExpressao();
        const corpo = this.parseBlocoComandos(TokenType.FIM_ENQUANTO);
        this.consume(TokenType.FIM_ENQUANTO, "Esperado 'FIM_ENQUANTO' encerrando o laço 'ENQUANTO'.");
        return { tipoComando: 'ENQUANTO', condicao, corpo, linha: token.line, coluna: token.column };
      }

      case TokenType.VETORIZAR_PARA: {
        this.advance();
        const variavel = this.consumeIdentificador("Esperado nome da variável do laço 'VETORIZAR_PARA'.").value;
        this.consume(TokenType.EM, "Esperado 'EM' introduzindo a fonte de dados vetorizada.");
        const fonte = this.parseAcessoCaminho();
        let passoSimd: number | undefined;
        if (this.match(TokenType.PASSO_SIMD)) {
          const valorPasso = this.consume(TokenType.NUMERO_LITERAL, 'Esperado tamanho do bloco SIMD após PASSO_SIMD.');
          passoSimd = Number.parseInt(valorPasso.value, 10);
          if (!Number.isInteger(passoSimd) || passoSimd <= 0) {
            throw new Error('[Erro Sintático][Linha ' + valorPasso.line + ':' + valorPasso.column + '] PASSO_SIMD exige inteiro positivo.');
          }
        }
        const corpo = this.parseBlocoComandos(TokenType.FIM_PARA);
        this.consume(TokenType.FIM_PARA, "Esperado 'FIM_PARA' encerrando o laço vetorizado.");
        return { tipoComando: 'VETORIZAR_PARA', variavel, fonte, passoSimd, corpo, linha: token.line, coluna: token.column };
      }

      case TokenType.USAR_BLOCO_MEMORIA: {
        this.advance();
        const nome = this.consumeIdentificador("Esperado nome do bloco de memória (arena).").value;
        const corpo = this.parseBlocoComandos(TokenType.FIM_BLOCO_MEMORIA);
        this.consume(TokenType.FIM_BLOCO_MEMORIA, "Esperado 'FIM_BLOCO_MEMORIA' encerrando o escopo de memória.");
        return { tipoComando: 'BLOCO_MEMORIA', nome, corpo, linha: token.line, coluna: token.column };
      }

      case TokenType.LER: {
        this.advance();
        const alvo = this.parseAcessoCaminho();
        return { tipoComando: 'LER', alvo, linha: token.line, coluna: token.column };
      }

      case TokenType.PARA: {
        this.advance();
        const variavel = this.consumeIdentificador("Esperado nome da variável do laço 'PARA'.").value;
        this.consume(TokenType.DE, "Esperado 'DE' após variável do laço 'PARA'.");
        const inicio = this.parseExpressao();
        this.consume(TokenType.ATE, "Esperado 'ATE' após início do intervalo do laço 'PARA'.");
        const fim = this.parseExpressao();
        let passo: ExprAST | undefined;
        if (this.match(TokenType.PASSO)) {
          passo = this.parseExpressao();
        }
        const corpo = this.parseBlocoComandos(TokenType.FIM_PARA);
        this.consume(TokenType.FIM_PARA, "Esperado 'FIM_PARA' encerrando o laço 'PARA'.");
        return { tipoComando: 'PARA', variavel, inicio, fim, passo, corpo, linha: token.line, coluna: token.column };
      }

      case TokenType.EXIBA: {
        this.advance();
        return { tipoComando: 'EXIBA', expressao: this.parseExpressao(), linha: token.line, coluna: token.column };
      }

      case TokenType.RETORNE: {
        this.advance();
        const podeTerValor = !this.check(TokenType.FIM) && !this.check(TokenType.FIM_SE)
          && !this.check(TokenType.FIM_ENQUANTO) && !this.check(TokenType.FIM_PARA)
          && !this.check(TokenType.FIM_BLOCO_MEMORIA);
        return {
          tipoComando: 'RETORNE',
          expressao: podeTerValor ? this.parseExpressao() : undefined,
          linha: token.line,
          coluna: token.column
        };
      }

      case TokenType.FALHAR_COM: {
        this.advance();
        return { tipoComando: 'FALHAR_COM', expressao: this.parseExpressao(), linha: token.line, coluna: token.column };
      }

      case TokenType.IDENTIFICADOR: {
        const alvo = this.parseAcessoCaminho();
        if (this.check(TokenType.ABRE_PARENTESE)) {
          // Chamada isolada como comando: TEXTO.comprimento(x) ou MATEMATICA.abs(-3)
          this.advance(); // '('
          const args: ExprAST[] = [];
          if (!this.check(TokenType.FECHA_PARENTESE)) {
            do {
              args.push(this.parseExpressao());
            } while (this.match(TokenType.VIRGULA));
          }
          this.consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando argumentos da chamada.");
          let expr: ExprAST = { tipo: 'CHAMADA', caminho: alvo, argumentos: args, linha: token.line, coluna: token.column };
          while (this.check(TokenType.ABRE_COLCHETE)) {
            this.advance();
            const indice = this.parseExpressao();
            this.consume(TokenType.FECHA_COLCHETE, "Esperado ']' após índice.");
            expr = { tipo: 'INDEXACAO', alvo: expr, indice, linha: token.line, coluna: token.column };
          }
          return { tipoComando: 'CHAMADA', expressao: expr, linha: token.line, coluna: token.column };
        }
        this.consume(TokenType.SETA_ATRIBUICAO, "Esperado '<-' na atribuição a '" + alvo.join('.') + "'.");
        const expressao = this.parseExpressao();
        return { tipoComando: 'ATRIBUICAO', alvo, expressao, linha: token.line, coluna: token.column };
      }

      default:
        throw new Error('[Erro Sintático][Linha ' + token.line + ':' + token.column + "] Comando não reconhecido. Esperados 'VARIAVEL', 'SE', 'ENQUANTO', 'VETORIZAR_PARA', 'PARA', 'USAR_BLOCO_MEMORIA', 'EXIBA', 'LER', 'RETORNE', 'FALHAR_COM' ou atribuição/chamada. (Encontrado: '" + token.value + "')");
    }
  }

  private parseAcessoCaminho(): string[] {
    const caminho = [this.consumeIdentificador('Esperado identificador.').value];
    while (this.match(TokenType.PONTO)) {
      caminho.push(this.consumeIdentificador('Esperado campo após ponto de acesso.').value);
    }
    return caminho;
  }

  /* ============================================================
   * EXPRESSÕES — precedência OU < E < relacional < aditivo < mult < unário
   * ============================================================ */

  public parseExpressao(): ExprAST {
    return this.parseExprOu();
  }

  private parseExprOu(): ExprAST {
    let esquerda = this.parseExprE();
    while (this.ehLogico('OU')) {
      const op = this.advance();
      const direita = this.parseExprE();
      esquerda = { tipo: 'OP_BINARIA', operador: 'OU', esquerda, direita, linha: op.line, coluna: op.column };
    }
    return esquerda;
  }

  private parseExprE(): ExprAST {
    let esquerda = this.parseExprRelacional();
    while (this.ehLogico('E')) {
      const op = this.advance();
      const direita = this.parseExprRelacional();
      esquerda = { tipo: 'OP_BINARIA', operador: 'E', esquerda, direita, linha: op.line, coluna: op.column };
    }
    return esquerda;
  }

  private ehLogico(palavra: 'E' | 'OU'): boolean {
    const t = this.peek();
    return t.type === TokenType.OPERADOR_LOGICO && t.value === palavra;
  }

  private static RELACIONAIS: Record<string, OperadorBinario> = {
    '=': '=', '<>': '<>', '<': '<', '<=': '<=', '>': '>', '>=': '>='
  };

  private parseExprRelacional(): ExprAST {
    const esquerda = this.parseExprAditiva();
    const t = this.peek();
    if (t.type === TokenType.OPERADOR_RELACIONAL && ThzParser.RELACIONAIS[t.value]) {
      this.advance();
      const direita = this.parseExprAditiva();
      return { tipo: 'OP_BINARIA', operador: ThzParser.RELACIONAIS[t.value], esquerda, direita, linha: t.line, coluna: t.column };
    }
    return esquerda;
  }

  private parseExprAditiva(): ExprAST {
    let esquerda = this.parseExprMultiplicativa();
    while (this.check(TokenType.OPERADOR_ARITMETICO) && (this.peek().value === '+' || this.peek().value === '-')) {
      const op = this.advance();
      const direita = this.parseExprMultiplicativa();
      esquerda = { tipo: 'OP_BINARIA', operador: op.value as OperadorBinario, esquerda, direita, linha: op.line, coluna: op.column };
    }
    return esquerda;
  }

  private parseExprMultiplicativa(): ExprAST {
    let esquerda = this.parseExprUnaria();
    while (this.check(TokenType.OPERADOR_ARITMETICO) && ['*', '/', '%'].includes(this.peek().value)) {
      const op = this.advance();
      const direita = this.parseExprUnaria();
      esquerda = { tipo: 'OP_BINARIA', operador: op.value as OperadorBinario, esquerda, direita, linha: op.line, coluna: op.column };
    }
    return esquerda;
  }

  private parseExprUnaria(): ExprAST {
    const t = this.peek();
    if (t.type === TokenType.OPERADOR_ARITMETICO && t.value === '-') {
      this.advance();
      return { tipo: 'OP_UNARIA', operador: '-', operando: this.parseExprUnaria(), linha: t.line, coluna: t.column };
    }
    if (t.type === TokenType.OPERADOR_LOGICO && t.value === 'NAO') {
      this.advance();
      return { tipo: 'OP_UNARIA', operador: 'NAO', operando: this.parseExprUnaria(), linha: t.line, coluna: t.column };
    }
    return this.parseExprPrimaria();
  }

  private parseExprPrimaria(): ExprAST {
    const t = this.peek();

    if (t.type === TokenType.NUMERO_LITERAL) {
      this.advance();
      const indicePonto = t.value.indexOf('.');
      let base: ExprAST;
      if (indicePonto >= 0) {
        const parteInteira = t.value.slice(0, indicePonto);
        const parteFracionaria = t.value.slice(indicePonto + 1);
        base = {
          tipo: 'LITERAL_DECIMAL',
          escalado: BigInt((parteInteira || '0') + parteFracionaria),
          escala: parteFracionaria.length,
          linha: t.line,
          coluna: t.column
        };
      } else {
        base = { tipo: 'LITERAL_INTEIRO', valor: BigInt(t.value), linha: t.line, coluna: t.column };
      }
      return this.parsePosfixo(base);
    }

    if (t.type === TokenType.STRING_LITERAL) {
      this.advance();
      const base: ExprAST = { tipo: 'LITERAL_TEXTO', valor: t.value, linha: t.line, coluna: t.column };
      return this.parsePosfixo(base);
    }

    if (t.type === TokenType.VERDADEIRO || t.type === TokenType.FALSO) {
      this.advance();
      const base: ExprAST = { tipo: 'LITERAL_LOGICO', valor: t.type === TokenType.VERDADEIRO, linha: t.line, coluna: t.column };
      return this.parsePosfixo(base);
    }

    if (t.type === TokenType.NULO) {
      this.advance();
      const base: ExprAST = { tipo: 'NULO', linha: t.line, coluna: t.column };
      return this.parsePosfixo(base);
    }

    if (t.type === TokenType.ABRE_PARENTESE) {
      this.advance();
      const interna = this.parseExpressao();
      this.consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando expressão parentetizada.");
      return this.parsePosfixo(interna);
    }

    if (t.type === TokenType.ABRE_COLCHETE) {
      this.advance();
      const elementos: ExprAST[] = [];
      if (!this.check(TokenType.FECHA_COLCHETE)) {
        do {
          elementos.push(this.parseExpressao());
        } while (this.match(TokenType.VIRGULA));
      }
      this.consume(TokenType.FECHA_COLCHETE, "Esperado ']' fechando literal de fatia.");
      const base: ExprAST = { tipo: 'FATIA_LITERAL', elementos, linha: t.line, coluna: t.column };
      return this.parsePosfixo(base);
    }

    if (t.type === TokenType.CRIAR) {
      this.advance();
      const nomeEstrutura = this.consumeIdentificador('Esperado nome da estrutura após CRIAR.').value;
      this.consume(TokenType.ABRE_PARENTESE, "Esperado '(' após nome da estrutura em 'CRIAR'.");
      const campos: { nome: string; valor: ExprAST }[] = [];
      if (!this.check(TokenType.FECHA_PARENTESE)) {
        do {
          const nomeCampo = this.consumeIdentificador('Esperado nome do campo em CRIAR.').value;
          this.consume(TokenType.DOIS_PONTOS, "Esperado ':' após nome do campo em CRIAR.");
          const valor = this.parseExpressao();
          campos.push({ nome: nomeCampo, valor });
        } while (this.match(TokenType.VIRGULA));
      }
      this.consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando 'CRIAR'.");
      const base: ExprAST = { tipo: 'CRIAR_REGISTRO', nomeEstrutura, campos, linha: t.line, coluna: t.column };
      return this.parsePosfixo(base);
    }

    if (t.type === TokenType.IDENTIFICADOR) {
      const caminho = this.parseAcessoCaminho();
      let base: ExprAST;
      if (this.check(TokenType.ABRE_PARENTESE)) {
        this.advance();
        const args: ExprAST[] = [];
        if (!this.check(TokenType.FECHA_PARENTESE)) {
          do {
            args.push(this.parseExpressao());
          } while (this.match(TokenType.VIRGULA));
        }
        this.consume(TokenType.FECHA_PARENTESE, "Esperado ')' fechando argumentos da chamada.");
        base = { tipo: 'CHAMADA', caminho, argumentos: args, linha: t.line, coluna: t.column };
      } else {
        base = { tipo: 'ACESSO', caminho, linha: t.line, coluna: t.column };
      }
      return this.parsePosfixo(base);
    }

    if (ehPalavraReservada(t.value)) {
      throw new Error('[Erro Sintático][Linha ' + t.line + ':' + t.column + "] '" + t.value + "' é palavra reservada e não pode iniciar uma expressão.");
    }

    throw new Error('[Erro Sintático][Linha ' + t.line + ':' + t.column + "] Expressão inválida. (Encontrado: '" + t.value + "')");
  }

  private parsePosfixo(base: ExprAST): ExprAST {
    let cur = base;
    while (this.check(TokenType.ABRE_COLCHETE)) {
      const inicio = this.peek();
      this.advance();
      const indice = this.parseExpressao();
      this.consume(TokenType.FECHA_COLCHETE, "Esperado ']' após índice.");
      cur = { tipo: 'INDEXACAO', alvo: cur, indice, linha: inicio.line, coluna: inicio.column };
    }
    return cur;
  }

  /* ============================================================
   * INFRAESTRUTURA DO CURSOR
   * ============================================================ */

  private match(...types: TokenType[]): boolean {
    for (const t of types) {
      if (this.check(t)) {
        this.advance();
        return true;
      }
    }
    return false;
  }

  private check(type: TokenType): boolean {
    if (this.isAtEnd()) return false;
    return this.peek().type === type;
  }

  private advance(): Token {
    if (!this.isAtEnd()) this.current++;
    return this.previous();
  }

  private isAtEnd(): boolean {
    return this.peek().type === TokenType.EOF;
  }

  private peek(): Token {
    return this.tokens[this.current];
  }

  private previous(): Token {
    return this.tokens[this.current - 1];
  }

  private consume(type: TokenType, message: string): Token {
    if (this.check(type)) return this.advance();
    const token = this.peek();
    throw new Error('[Erro Sintático][Linha ' + token.line + ':' + token.column + '] ' + message + " (Encontrado: '" + token.value + "')");
  }

  /**
   * Política estrita de palavras reservadas: um nome declarado (programa,
   * estrutura, campo, regra, operação, parâmetro, variável) nunca pode
   * colidir com palavra reservada da linguagem.
   */
  private consumeIdentificador(message: string): Token {
    const token = this.peek();
    if (!this.check(TokenType.IDENTIFICADOR)) {
      if (ehPalavraReservada(token.value)) {
        throw new Error('[Erro Sintático][Linha ' + token.line + ':' + token.column + '] ' + message + " '" + token.value + "' é palavra reservada e não pode ser usada como identificador.");
      }
      throw new Error('[Erro Sintático][Linha ' + token.line + ':' + token.column + '] ' + message + " (Encontrado: '" + token.value + "')");
    }
    return this.advance();
  }
}

/** Renderização textual canônica de uma expressão (para docgen e auditoria). */
export function textoCanonicoDe(expr: ExprAST): string {
  switch (expr.tipo) {
    case 'LITERAL_INTEIRO':
      return expr.valor.toString();
    case 'LITERAL_DECIMAL':
      return formatarEscalado(expr.escalado, expr.escala);
    case 'LITERAL_TEXTO':
      return '"' + expr.valor.replace(/\n/g, '\\n') + '"';
    case 'LITERAL_LOGICO':
      return expr.valor ? 'VERDADEIRO' : 'FALSO';
    case 'NULO':
      return 'NULO';
    case 'ACESSO':
      return expr.caminho.join('.');
    case 'CHAMADA':
      return expr.caminho.join('.') + '(' + expr.argumentos.map(textoCanonicoDe).join(', ') + ')';
    case 'INDEXACAO':
      return textoCanonicoDe(expr.alvo) + '[' + textoCanonicoDe(expr.indice) + ']';
    case 'FATIA_LITERAL':
      return '[' + expr.elementos.map(textoCanonicoDe).join(', ') + ']';
    case 'CRIAR_REGISTRO':
      return 'CRIAR ' + expr.nomeEstrutura + '(' + expr.campos.map((c) => c.nome + ': ' + textoCanonicoDe(c.valor)).join(', ') + ')';
    case 'OP_UNARIA':
      return (expr.operador === '-' ? '-' : 'NAO ') + textoCanonicoDe(expr.operando);
    case 'OP_BINARIA':
      return textoCanonicoDe(expr.esquerda) + ' ' + expr.operador + ' ' + textoCanonicoDe(expr.direita);
  }
}

function formatarEscalado(escalado: bigint, escala: number): string {
  const negativo = escalado < 0n;
  const absoluto = negativo ? -escalado : escalado;
  const divisor = 10n ** BigInt(escala);
  const inteiro = absoluto / divisor;
  const fracao = (absoluto % divisor).toString().padStart(escala, '0');
  return (negativo ? '-' : '') + inteiro.toString() + '.' + fracao;
}
