import { Token, TokenType } from './types.js';
import { tokenDe } from './keywords.js';

export class ErroLexico extends Error {
  constructor(linha: number, coluna: number, mensagem: string) {
    super('[Erro Léxico][Linha ' + linha + ':' + coluna + '] ' + mensagem);
    this.name = 'ErroLexico';
  }
}

export class ThzLexer {
  private pos = 0;
  private line = 1;
  private col = 1;

  constructor(private input: string) {}

  public tokenize(): Token[] {
    const tokens: Token[] = [];

    while (this.pos < this.input.length) {
      const char = this.input[this.pos];

      if (char === ' ' || char === '\t' || char === '\r') {
        this.advance();
        continue;
      }

      if (char === '\n') {
        this.line++;
        this.col = 1;
        this.pos++;
        continue;
      }

      if (char === '#') {
        while (this.pos < this.input.length && this.input[this.pos] !== '\n') {
          this.advance();
        }
        continue;
      }

      if (char === '"') {
        tokens.push(this.readString());
        continue;
      }

      if (/\d/.test(char)) {
        tokens.push(this.readNumber());
        continue;
      }

      if (char === ':') { tokens.push(this.makeToken(TokenType.DOIS_PONTOS, ':')); this.advance(); continue; }
      if (char === '=') { tokens.push(this.makeToken(TokenType.OPERADOR_RELACIONAL, '=')); this.advance(); continue; }
      if (char === '.') { tokens.push(this.makeToken(TokenType.PONTO, '.')); this.advance(); continue; }
      if (char === ',') { tokens.push(this.makeToken(TokenType.VIRGULA, ',')); this.advance(); continue; }
      if (char === '(') { tokens.push(this.makeToken(TokenType.ABRE_PARENTESE, '(')); this.advance(); continue; }
      if (char === ')') { tokens.push(this.makeToken(TokenType.FECHA_PARENTESE, ')')); this.advance(); continue; }
      if (char === '[') { tokens.push(this.makeToken(TokenType.ABRE_COLCHETE, '[')); this.advance(); continue; }
      if (char === ']') { tokens.push(this.makeToken(TokenType.FECHA_COLCHETE, ']')); this.advance(); continue; }
      if (char === '%') { tokens.push(this.makeToken(TokenType.OPERADOR_ARITMETICO, '%')); this.advance(); continue; }

      if (char === '+') { tokens.push(this.makeToken(TokenType.OPERADOR_ARITMETICO, '+')); this.advance(); continue; }
      if (char === '-') { tokens.push(this.makeToken(TokenType.OPERADOR_ARITMETICO, '-')); this.advance(); continue; }
      if (char === '*') { tokens.push(this.makeToken(TokenType.OPERADOR_ARITMETICO, '*')); this.advance(); continue; }
      if (char === '/') { tokens.push(this.makeToken(TokenType.OPERADOR_ARITMETICO, '/')); this.advance(); continue; }

      // Operadores multi-caractere atômicos: <-, <=, <>, >=
      if (char === '<') {
        const proximo = this.input[this.pos + 1];
        if (proximo === '-') { tokens.push(this.makeToken(TokenType.SETA_ATRIBUICAO, '<-')); this.advance(); this.advance(); continue; }
        if (proximo === '=') { tokens.push(this.makeToken(TokenType.OPERADOR_RELACIONAL, '<=')); this.advance(); this.advance(); continue; }
        if (proximo === '>') { tokens.push(this.makeToken(TokenType.OPERADOR_RELACIONAL, '<>')); this.advance(); this.advance(); continue; }
        tokens.push(this.makeToken(TokenType.OPERADOR_RELACIONAL, '<'));
        this.advance();
        continue;
      }

      if (char === '>') {
        const proximo = this.input[this.pos + 1];
        if (proximo === '=') { tokens.push(this.makeToken(TokenType.OPERADOR_RELACIONAL, '>=')); this.advance(); this.advance(); continue; }
        tokens.push(this.makeToken(TokenType.OPERADOR_RELACIONAL, '>'));
        this.advance();
        continue;
      }

      if (/[a-zA-Z_]/.test(char)) {
        tokens.push(this.readIdentifier());
        continue;
      }

      throw new ErroLexico(this.line, this.col, "Caractere não reconhecido pela gramática THZ: '" + char + "'.");
    }

    tokens.push({ type: TokenType.EOF, value: '', line: this.line, column: this.col });
    return tokens;
  }

  private advance(): void {
    this.pos++;
    this.col++;
  }

  private makeToken(type: TokenType, value: string): Token {
    return { type, value, line: this.line, column: this.col };
  }

  private readString(): Token {
    const startLine = this.line;
    const startCol = this.col;
    this.advance();
    let str = '';
    while (this.pos < this.input.length && this.input[this.pos] !== '"' && this.input[this.pos] !== '\n') {
      if (this.input[this.pos] === '\\' && this.input[this.pos + 1] === 'n') {
        str += '\n';
        this.advance();
        this.advance();
      } else {
        str += this.input[this.pos];
        this.advance();
      }
    }
    if (this.pos >= this.input.length || this.input[this.pos] !== '"') {
      throw new ErroLexico(startLine, startCol, 'Literal de texto não terminado (esperada aspa dupla de fechamento).');
    }
    this.advance();
    return { type: TokenType.STRING_LITERAL, value: str, line: startLine, column: startCol };
  }

  private readNumber(): Token {
    const startCol = this.col;
    let num = '';
    while (this.pos < this.input.length && /[\d._]/.test(this.input[this.pos])) {
      // O ponto só integra o literal se seguido de dígito (evita engolir acesso a campo).
      if (this.input[this.pos] === '.' && !/\d/.test(this.input[this.pos + 1] ?? '')) break;
      if (this.input[this.pos] !== '_') num += this.input[this.pos];
      this.advance();
    }
    if (num.endsWith('.')) num = num.slice(0, -1);
    return { type: TokenType.NUMERO_LITERAL, value: num, line: this.line, column: startCol };
  }

  private readIdentifier(): Token {
    const startCol = this.col;
    let ident = '';
    while (this.pos < this.input.length && /[a-zA-Z0-9_]/.test(this.input[this.pos])) {
      ident += this.input[this.pos];
      this.advance();
    }

    const type = tokenDe(ident) ?? TokenType.IDENTIFICADOR;
    return { type, value: ident, line: this.line, column: startCol };
  }
}
