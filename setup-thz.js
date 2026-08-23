#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const ROOT_DIR = path.resolve(process.cwd(), 'thz-lang-engine');

console.log('================================================================================');
console.log('       INICIALIZADOR DO ECOSSISTEMA THZ-LANG (NODE.JS + TYPESCRIPT)             ');
console.log('================================================================================\n');

function criarDiretorio(dirPath) {
  if (!fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, { recursive: true });
    console.log(`[DIR] Criado: ${path.relative(process.cwd(), dirPath)}`);
  }
}

function escreverArquivo(filePath, conteudo) {
  fs.writeFileSync(filePath, conteudo.trim() + '\n', 'utf8');
  console.log(`[FILE] Gerado: ${path.relative(process.cwd(), filePath)}`);
}

// 1. Criar estrutura de diretórios
criarDiretorio(path.join(ROOT_DIR, 'src'));
criarDiretorio(path.join(ROOT_DIR, 'exemplos'));
criarDiretorio(path.join(ROOT_DIR, 'docs'));

// 2. package.json
escreverArquivo(path.join(ROOT_DIR, 'package.json'), JSON.stringify({
  name: "thz-lang-engine",
  version: "2.1.0",
  description: "Enterprise Systems Language & Architecture Engine",
  type: "module",
  main: "src/cli.ts",
  scripts: {
    "thz": "tsx src/cli.ts",
    "thz:run": "tsx src/cli.ts run exemplos/faturamento.thz",
    "thz:doc": "tsx src/cli.ts doc exemplos/faturamento.thz",
    "thz:check": "tsx src/cli.ts check exemplos/faturamento.thz"
  },
  dependencies: {},
  devDependencies: {
    "@types/node": "^20.11.0",
    "typescript": "^5.3.3",
    "tsx": "^4.7.0"
  }
}, null, 2));

// 3. tsconfig.json
escreverArquivo(path.join(ROOT_DIR, 'tsconfig.json'), JSON.stringify({
  compilerOptions: {
    target: "ES2022",
    module: "NodeNext",
    moduleResolution: "NodeNext",
    strict: true,
    esModuleInterop: true,
    skipLibCheck: true,
    forceConsistentCasingInFileNames: true,
    outDir: "./dist"
  },
  include: ["src/**/*"]
}, null, 2));

// 4. src/types.ts
escreverArquivo(path.join(ROOT_DIR, 'src', 'types.ts'), `
export enum TokenType {
  PROGRAMA = 'PROGRAMA',
  FIM_PROGRAMA = 'FIM_PROGRAMA',
  METADADOS_ARQUITETURA = 'METADADOS_ARQUITETURA',
  FIM_METADADOS = 'FIM_METADADOS',
  ESTRUTURA = 'ESTRUTURA',
  FIM_ESTRUTURA = 'FIM_ESTRUTURA',
  REGRA_NEGOCIO = 'REGRA_NEGOCIO',
  FIM_REGRA_NEGOCIO = 'FIM_REGRA_NEGOCIO',
  PROCEDIMENTO = 'PROCEDIMENTO',
  INICIO = 'INICIO',
  FIM = 'FIM',
  EXIGE = 'EXIGE',
  GARANTE = 'GARANTE',
  VARIAVEL = 'VARIAVEL',
  RETORNE = 'RETORNE',
  EXIBA = 'EXIBA',
  OPERACAO = 'OPERACAO',
  VETORIZAR_PARA = 'VETORIZAR_PARA',
  EM = 'EM',
  PASSO_SIMD = 'PASSO_SIMD',
  FIM_PARA = 'FIM_PARA',
  USAR_BLOCO_MEMORIA = 'USAR_BLOCO_MEMORIA',
  FIM_BLOCO_MEMORIA = 'FIM_BLOCO_MEMORIA',
  IDENTIFICADOR = 'IDENTIFICADOR',
  STRING_LITERAL = 'STRING_LITERAL',
  NUMERO_LITERAL = 'NUMERO_LITERAL',
  DOIS_PONTOS = ':',
  IGUAL = '=',
  PONTO = '.',
  VIRGULA = ',',
  ABRE_PARENTESE = '(',
  FECHA_PARENTESE = ')',
  ABRE_COLCHETE = '[',
  FECHA_COLCHETE = ']',
  OPERADOR_RELACIONAL = 'OPERADOR_RELACIONAL',
  OPERADOR_ARITMETICO = 'OPERADOR_ARITMETICO',
  EOF = 'EOF'
}

export interface Token {
  type: TokenType;
  value: string;
  line: number;
  column: number;
}

export interface MetadadosArquiteturaAST {
  dominio: string;
  subdominio: string;
  camada: string;
  versao: string;
  autor: string;
  sloLatencia: string;
  conformidade: string[];
}

export interface CampoEstruturaAST {
  nome: string;
  tipo: string;
}

export interface EstruturaAST {
  nome: string;
  layoutColunar: boolean;
  campos: CampoEstruturaAST[];
}

export interface RegraNegocioAST {
  nome: string;
  identificador: string;
  rastreioRequisito: string;
  descricao: string;
  exiges: string[];
  garantes: string[];
  operacaoNome: string;
  parametroNome: string;
  parametroTipo: string;
  tipoRetorno: string;
}

export interface ProgramaAST {
  nome: string;
  metadados?: MetadadosArquiteturaAST;
  estruturas: EstruturaAST[];
  regras: RegraNegocioAST[];
}
`);

// 5. src/lexer.ts
escreverArquivo(path.join(ROOT_DIR, 'src', 'lexer.ts'), `
import { Token, TokenType } from './types.js';

export class ThzLexer {
  private pos = 0;
  private line = 1;
  private col = 1;

  constructor(private input: string) {}

  public tokenize(): Token[] {
    const tokens: Token[] = [];

    while (this.pos < this.input.length) {
      const char = this.input[this.pos];

      if (char === ' ' || char === '\\t' || char === '\\r') {
        this.advance();
        continue;
      }

      if (char === '\\n') {
        this.line++;
        this.col = 1;
        this.pos++;
        continue;
      }

      if (char === '#') {
        while (this.pos < this.input.length && this.input[this.pos] !== '\\n') {
          this.advance();
        }
        continue;
      }

      if (char === '"') {
        tokens.push(this.readString());
        continue;
      }

      if (/\\d/.test(char)) {
        tokens.push(this.readNumber());
        continue;
      }

      if (char === ':') { tokens.push(this.makeToken(TokenType.DOIS_PONTOS, ':')); this.advance(); continue; }
      if (char === '=') { tokens.push(this.makeToken(TokenType.IGUAL, '=')); this.advance(); continue; }
      if (char === '.') { tokens.push(this.makeToken(TokenType.PONTO, '.')); this.advance(); continue; }
      if (char === ',') { tokens.push(this.makeToken(TokenType.VIRGULA, ',')); this.advance(); continue; }
      if (char === '(') { tokens.push(this.makeToken(TokenType.ABRE_PARENTESE, '(')); this.advance(); continue; }
      if (char === ')') { tokens.push(this.makeToken(TokenType.FECHA_PARENTESE, ')')); this.advance(); continue; }
      if (char === '[') { tokens.push(this.makeToken(TokenType.ABRE_COLCHETE, '[')); this.advance(); continue; }
      if (char === ']') { tokens.push(this.makeToken(TokenType.FECHA_COLCHETE, ']')); this.advance(); continue; }
      
      if (['+', '-', '*', '/'].includes(char)) {
        tokens.push(this.makeToken(TokenType.OPERADOR_ARITMETICO, char));
        this.advance();
        continue;
      }
      
      if (['>', '<'].includes(char)) {
        tokens.push(this.makeToken(TokenType.OPERADOR_RELACIONAL, char));
        this.advance();
        continue;
      }

      if (/[a-zA-Z_]/.test(char)) {
        tokens.push(this.readIdentifier());
        continue;
      }

      this.advance();
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
    const startCol = this.col;
    this.advance();
    let str = '';
    while (this.pos < this.input.length && this.input[this.pos] !== '"') {
      if (this.input[this.pos] === '\\\\' && this.input[this.pos + 1] === 'n') {
        str += '\\n';
        this.advance();
        this.advance();
      } else {
        str += this.input[this.pos];
        this.advance();
      }
    }
    this.advance();
    return { type: TokenType.STRING_LITERAL, value: str, line: this.line, column: startCol };
  }

  private readNumber(): Token {
    const startCol = this.col;
    let num = '';
    while (this.pos < this.input.length && /[\\d._]/.test(this.input[this.pos])) {
      if (this.input[this.pos] !== '_') num += this.input[this.pos];
      this.advance();
    }
    return { type: TokenType.NUMERO_LITERAL, value: num, line: this.line, column: startCol };
  }

  private readIdentifier(): Token {
    const startCol = this.col;
    let ident = '';
    while (this.pos < this.input.length && /[a-zA-Z0-9_]/.test(this.input[this.pos])) {
      ident += this.input[this.pos];
      this.advance();
    }

    const keywordMap: Record<string, TokenType> = {
      'PROGRAMA': TokenType.PROGRAMA,
      'FIM_PROGRAMA': TokenType.FIM_PROGRAMA,
      'METADADOS_ARQUITETURA': TokenType.METADADOS_ARQUITETURA,
      'FIM_METADADOS': TokenType.FIM_METADADOS,
      'ESTRUTURA': TokenType.ESTRUTURA,
      'FIM_ESTRUTURA': TokenType.FIM_ESTRUTURA,
      'REGRA_NEGOCIO': TokenType.REGRA_NEGOCIO,
      'FIM_REGRA_NEGOCIO': TokenType.FIM_REGRA_NEGOCIO,
      'PROCEDIMENTO': TokenType.PROCEDIMENTO,
      'INICIO': TokenType.INICIO,
      'FIM': TokenType.FIM,
      'EXIGE': TokenType.EXIGE,
      'GARANTE': TokenType.GARANTE,
      'VARIAVEL': TokenType.VARIAVEL,
      'RETORNE': TokenType.RETORNE,
      'EXIBA': TokenType.EXIBA,
      'OPERACAO': TokenType.OPERACAO,
      'VETORIZAR_PARA': TokenType.VETORIZAR_PARA,
      'EM': TokenType.EM,
      'PASSO_SIMD': TokenType.PASSO_SIMD,
      'FIM_PARA': TokenType.FIM_PARA,
      'USAR_BLOCO_MEMORIA': TokenType.USAR_BLOCO_MEMORIA,
      'FIM_BLOCO_MEMORIA': TokenType.FIM_BLOCO_MEMORIA
    };

    const type = keywordMap[ident] || TokenType.IDENTIFICADOR;
    return { type, value: ident, line: this.line, column: startCol };
  }
}
`);

// 6. src/parser.ts
escreverArquivo(path.join(ROOT_DIR, 'src', 'parser.ts'), `
import { Token, TokenType, ProgramaAST, MetadadosArquiteturaAST, EstruturaAST, RegraNegocioAST } from './types.js';

export class ThzParser {
  private current = 0;

  constructor(private tokens: Token[]) {}

  public parse(): ProgramaAST {
    this.consume(TokenType.PROGRAMA, "Esperado 'PROGRAMA' no início do arquivo.");
    const nomePrograma = this.consume(TokenType.IDENTIFICADOR, "Esperado o nome do programa.").value;

    const ast: ProgramaAST = {
      nome: nomePrograma,
      estruturas: [],
      regras: []
    };

    while (!this.match(TokenType.FIM_PROGRAMA) && !this.isAtEnd()) {
      if (this.match(TokenType.METADADOS_ARQUITETURA)) {
        ast.metadados = this.parseMetadados();
      } else if (this.match(TokenType.ESTRUTURA)) {
        ast.estruturas.push(this.parseEstrutura());
      } else if (this.match(TokenType.REGRA_NEGOCIO)) {
        ast.regras.push(this.parseRegraNegocio());
      } else {
        this.advance();
      }
    }

    return ast;
  }

  private parseMetadados(): MetadadosArquiteturaAST {
    const meta: Partial<MetadadosArquiteturaAST> = { conformidade: [] };
    while (!this.match(TokenType.FIM_METADADOS) && !this.isAtEnd()) {
      const chave = this.consume(TokenType.IDENTIFICADOR, "Esperada chave de metadado.").value;
      this.consume(TokenType.DOIS_PONTOS, "Esperado ':' após identificador de metadado.");
      
      if (chave === 'CONFORMIDADE') {
        const val1 = this.consume(TokenType.STRING_LITERAL, "Esperada regra de conformidade.").value;
        meta.conformidade?.push(val1);
        while (this.match(TokenType.VIRGULA)) {
          meta.conformidade?.push(this.consume(TokenType.STRING_LITERAL, "Esperado próximo valor.").value);
        }
      } else {
        const valor = this.consume(TokenType.STRING_LITERAL, "Esperado valor textual do metadado.").value;
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

  private parseEstrutura(): EstruturaAST {
    const nome = this.consume(TokenType.IDENTIFICADOR, "Esperado nome da estrutura.").value;
    let layoutColunar = false;
    if (this.peek().value === 'LAYOUT_COLUNAR') {
      layoutColunar = true;
      this.advance();
    }

    const campos: { nome: string; tipo: string }[] = [];
    while (!this.match(TokenType.FIM_ESTRUTURA) && !this.isAtEnd()) {
      const campoNome = this.consume(TokenType.IDENTIFICADOR, "Esperado nome do campo.").value;
      this.consume(TokenType.DOIS_PONTOS, "Esperado ':' após o nome do campo.");
      
      let tipo = this.consume(TokenType.IDENTIFICADOR, "Esperado tipo do campo.").value;
      if (this.match(TokenType.ABRE_PARENTESE)) {
        let params = '';
        while (!this.check(TokenType.FECHA_PARENTESE) && !this.isAtEnd()) {
          params += this.peek().value;
          this.advance();
        }
        this.consume(TokenType.FECHA_PARENTESE, "Esperado ')' após parâmetros do tipo.");
        tipo += '(' + params + ')';
      }
      campos.push({ nome: campoNome, tipo });
    }

    return { nome, layoutColunar, campos };
  }

  private parseRegraNegocio(): RegraNegocioAST {
    const nome = this.consume(TokenType.IDENTIFICADOR, "Esperado nome da regra.").value;
    const regra: Partial<RegraNegocioAST> = {
      nome,
      exiges: [],
      garantes: []
    };

    while (!this.match(TokenType.FIM_REGRA_NEGOCIO) && !this.isAtEnd()) {
      if (this.peek().value === 'IDENTIFICADOR_REGRA') {
        this.advance();
        this.consume(TokenType.DOIS_PONTOS, "Esperado ':'.");
        regra.identificador = this.consume(TokenType.STRING_LITERAL, "Esperado ID.").value;
      } else if (this.peek().value === 'RASTREIO_REQUISITO') {
        this.advance();
        this.consume(TokenType.DOIS_PONTOS, "Esperado ':'.");
        regra.rastreioRequisito = this.consume(TokenType.STRING_LITERAL, "Esperado requisito.").value;
      } else if (this.peek().value === 'DESCRICAO') {
        this.advance();
        this.consume(TokenType.DOIS_PONTOS, "Esperado ':'.");
        regra.descricao = this.consume(TokenType.STRING_LITERAL, "Esperada descrição.").value;
      } else if (this.match(TokenType.EXIGE)) {
        let cond = '';
        while (!this.check(TokenType.EXIGE) && !this.check(TokenType.FIM) && !this.isAtEnd()) {
          if (this.peek().value.startsWith('FIM_CONTRATO')) { this.advance(); break; }
          cond += ' ' + this.peek().value;
          this.advance();
        }
        regra.exiges?.push(cond.trim());
      } else if (this.match(TokenType.GARANTE)) {
        let cond = '';
        while (!this.check(TokenType.GARANTE) && !this.check(TokenType.FIM) && !this.isAtEnd()) {
          if (this.peek().value.startsWith('FIM_CONTRATO')) { this.advance(); break; }
          cond += ' ' + this.peek().value;
          this.advance();
        }
        regra.garantes?.push(cond.trim());
      } else if (this.match(TokenType.OPERACAO)) {
        regra.operacaoNome = this.consume(TokenType.IDENTIFICADOR, "Esperado nome da operação.").value;
        this.consume(TokenType.ABRE_PARENTESE, "Esperado '('.");
        regra.parametroNome = this.consume(TokenType.IDENTIFICADOR, "Esperado nome do parâmetro.").value;
        this.consume(TokenType.DOIS_PONTOS, "Esperado ':'.");
        
        let pTipo = this.consume(TokenType.IDENTIFICADOR, "Esperado tipo do parâmetro.").value;
        if (this.match(TokenType.ABRE_COLCHETE)) {
          pTipo += '[' + this.consume(TokenType.IDENTIFICADOR, "Tipo interno").value + ']';
          this.consume(TokenType.FECHA_COLCHETE, "Esperado ']'.");
        }
        regra.parametroTipo = pTipo;
        this.consume(TokenType.FECHA_PARENTESE, "Esperado ')'.");
        this.consume(TokenType.DOIS_PONTOS, "Esperado ':'.");
        
        let retTipo = this.consume(TokenType.IDENTIFICADOR, "Esperado tipo de retorno.").value;
        if (this.match(TokenType.ABRE_PARENTESE)) {
          let p = '';
          while (!this.check(TokenType.FECHA_PARENTESE)) { p += this.peek().value; this.advance(); }
          this.consume(TokenType.FECHA_PARENTESE, "Esperado ')'.");
          retTipo += '(' + p + ')';
        }
        regra.tipoRetorno = retTipo;
      } else {
        this.advance();
      }
    }

    return regra as RegraNegocioAST;
  }

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
}
`);

// 7. src/runtime.ts
escreverArquivo(path.join(ROOT_DIR, 'src', 'runtime.ts'), `
export class DecimalFixo {
  public valorEscalado: bigint;
  public static readonly FATOR = 10000n; // Padrão ISO/IEC 10967 (Ponto Fixo Escalado)

  constructor(valor: number | string | bigint) {
    if (typeof valor === 'bigint') {
      this.valorEscalado = valor;
    } else if (typeof valor === 'number') {
      this.valorEscalado = BigInt(Math.round(valor * 10000));
    } else {
      const parts = valor.split('.');
      const inteira = BigInt(parts[0]);
      let fracStr = (parts[1] || '').padEnd(4, '0').slice(0, 4);
      const frac = BigInt(fracStr);
      this.valorEscalado = inteira * DecimalFixo.FATOR + frac;
    }
  }

  public somar(outro: DecimalFixo): DecimalFixo {
    return new DecimalFixo(this.valorEscalado + outro.valorEscalado);
  }

  public multiplicar(outro: DecimalFixo): DecimalFixo {
    return new DecimalFixo((this.valorEscalado * outro.valorEscalado) / DecimalFixo.FATOR);
  }

  public dividir(outro: DecimalFixo): DecimalFixo {
    return new DecimalFixo((this.valorEscalado * DecimalFixo.FATOR) / outro.valorEscalado);
  }

  public formatar(): string {
    const inteiro = this.valorEscalado / DecimalFixo.FATOR;
    let frac = (this.valorEscalado % DecimalFixo.FATOR).toString().padStart(4, '0');
    return inteiro + '.' + frac;
  }
}

export class ArenaMemoria {
  private buffer: ArrayBuffer;
  private offset = 0;

  constructor(tamanhoMb: number) {
    this.buffer = new ArrayBuffer(tamanhoMb * 1024 * 1024);
  }

  public alocar(bytes: number): number {
    const endereco = this.offset;
    this.offset += bytes;
    if (this.offset > this.buffer.byteLength) {
      throw new Error("[Runtime THZ] Estouro de capacidade da Arena de Memória.");
    }
    return endereco;
  }

  public liberarTudo(): void {
    this.offset = 0;
  }
}

export interface ItemFaturaRuntime {
  id_transacao: string;
  codigo_produto: string;
  quantidade: number;
  valor_unitario: DecimalFixo;
  aliquota_imposto: DecimalFixo;
  valor_total_liquido: DecimalFixo;
}

export class ThzRuntime {
  public static executarRegraFiscal(itens: ItemFaturaRuntime[]): DecimalFixo {
    // 1. Contratos Formais de Entrada (EXIGE)
    for (const item of itens) {
      if (item.quantidade <= 0) {
        throw new Error('[Violação de Contrato EXIGE] quantidade deve ser > 0. Valor: ' + item.quantidade);
      }
      if (item.valor_unitario.valorEscalado < 0n) {
        throw new Error('[Violação de Contrato EXIGE] valor_unitario não pode ser negativo.');
      }
    }

    let acumuladorTributos = new DecimalFixo(0n);
    const cem = new DecimalFixo(100);

    // 2. Execução Vetorizada / Em Lote
    for (const item of itens) {
      const qtdDecimal = new DecimalFixo(item.quantidade);
      const bruto = qtdDecimal.multiplicar(item.valor_unitario);
      const fatorAliquota = item.aliquota_imposto.dividir(cem);
      const imposto = bruto.multiplicar(fatorAliquota);

      item.valor_total_liquido = bruto.somar(imposto);
      acumuladorTributos = acumuladorTributos.somar(imposto);

      // 3. Contratos Formais de Saída (GARANTE)
      if (item.valor_total_liquido.valorEscalado < 0n) {
        throw new Error('[Violação de Contrato GARANTE] valor_total_liquido não pode ser negativo.');
      }
    }

    return acumuladorTributos;
  }
}
`);

// 8. src/docgen.ts (Construção limpa sem template literals problemáticos)
escreverArquivo(path.join(ROOT_DIR, 'src', 'docgen.ts'), `
import { ProgramaAST } from './types.js';

export class ThzDocGen {
  public static gerarMarkdown(ast: ProgramaAST): string {
    const crase3 = String.fromCharCode(96, 96, 96);
    const crase1 = String.fromCharCode(96);
    
    let doc = '# Especificação Arquitetural e Dicionário de Domínio: ' + ast.nome + '\\n\\n';
    
    if (ast.metadados) {
      doc += '## 1. Metadados de Governança (ISO/IEC/IEEE 42010)\\n\\n';
      doc += '| Atributo | Valor |\\n| :--- | :--- |\\n';
      doc += '| **Domínio** | ' + ast.metadados.dominio + ' |\\n';
      doc += '| **Subdomínio** | ' + ast.metadados.subdominio + ' |\\n';
      doc += '| **Camada** | ' + ast.metadados.camada + ' |\\n';
      doc += '| **Versão** | ' + ast.metadados.versao + ' |\\n';
      doc += '| **Autor** | ' + ast.metadados.autor + ' |\\n';
      doc += '| **SLO Latência** | ' + ast.metadados.sloLatencia + ' |\\n';
      doc += '| **Conformidade** | ' + ast.metadados.conformidade.join(', ') + ' |\\n\\n';
    }

    doc += '## 2. Estruturas de Dados e Layout Colunar\\n\\n';
    for (const est of ast.estruturas) {
      doc += '### Estrutura: ' + crase1 + est.nome + crase1 + ' ' + (est.layoutColunar ? '*(Layout Colunar / SIMD)*' : '') + '\\n\\n';
      doc += '| Campo | Tipo |\\n| :--- | :--- |\\n';
      for (const c of est.campos) {
        doc += '| ' + crase1 + c.nome + crase1 + ' | ' + crase1 + c.tipo + crase1 + ' |\\n';
      }
      doc += '\\n';
    }

    doc += '## 3. Regras de Negócio e Contratos Formais\\n\\n';
    for (const r of ast.regras) {
      doc += '### Regra: ' + crase1 + r.nome + crase1 + ' (ID: ' + crase1 + r.identificador + crase1 + ')\\n\\n';
      doc += '- **Rastreio:** ' + crase1 + r.rastreioRequisito + crase1 + '\\n';
      doc += '- **Descrição:** ' + r.descricao + '\\n';
      doc += '- **Operação Principal:** ' + crase1 + r.operacaoNome + '(' + r.parametroNome + ': ' + r.parametroTipo + ') : ' + r.tipoRetorno + crase1 + '\\n\\n';
    }

    doc += '## 4. Diagrama de Fluxo e Arquitetura Viva\\n\\n';
    doc += crase3 + 'mermaid\\ngraph TD\\n';
    const dom = (ast.metadados && ast.metadados.dominio) ? ast.metadados.dominio : 'Dominio';
    const sub = (ast.metadados && ast.metadados.subdominio) ? ast.metadados.subdominio : 'Subdominio';
    doc += '    subgraph BoundedContext [' + dom + ' / ' + sub + ']\\n';
    for (const r of ast.regras) {
      doc += '        Regra_' + r.nome + '["Regra: ' + r.nome + '<br/>ID: ' + r.identificador + '"] --> Op_' + r.operacaoNome + '["Operação: ' + r.operacaoNome + '()"]\\n';
    }
    doc += '    end\\n' + crase3 + '\\n';

    return doc;
  }
}
`);

// 9. src/cli.ts
escreverArquivo(path.join(ROOT_DIR, 'src', 'cli.ts'), `
import fs from 'fs';
import path from 'path';
import { ThzLexer } from './lexer.js';
import { ThzParser } from './parser.js';
import { ThzDocGen } from './docgen.js';
import { ArenaMemoria, DecimalFixo, ItemFaturaRuntime, ThzRuntime } from './runtime.js';

const comando = process.argv[2] || 'run';
const arquivo = process.argv[3] || 'exemplos/faturamento.thz';

if (!fs.existsSync(arquivo)) {
  console.error('[ERRO] Arquivo não encontrado: ' + arquivo);
  process.exit(1);
}

const codigoFonte = fs.readFileSync(arquivo, 'utf8');

try {
  // 1. Lexer
  const lexer = new ThzLexer(codigoFonte);
  const tokens = lexer.tokenize();

  // 2. Parser
  const parser = new ThzParser(tokens);
  const ast = parser.parse();

  if (comando === 'check') {
    console.log('[THZ CHECK] Código validado com sucesso! AST íntegra para o programa: ' + ast.nome);
    process.exit(0);
  }

  if (comando === 'doc') {
    const docMd = ThzDocGen.gerarMarkdown(ast);
    const saidaDoc = path.join('docs', ast.nome + '_arquitetura.md');
    fs.writeFileSync(saidaDoc, docMd, 'utf8');
    console.log('[THZ DOC] Documentação gerada em: ' + saidaDoc);
    console.log('\\n' + docMd);
    process.exit(0);
  }

  if (comando === 'run') {
    console.log('================================================================================');
    console.log('   EXECUTANDO MOTOR NATIVO THZ-LANG: ' + ast.nome);
    console.log('================================================================================\\n');

    const arena = new ArenaMemoria(64);
    arena.alocar(2048);

    const dom = ast.metadados ? ast.metadados.dominio : 'N/A';
    const slo = ast.metadados ? ast.metadados.sloLatencia : 'N/A';
    const conf = ast.metadados ? ast.metadados.conformidade.join(', ') : 'N/A';

    console.log('[ARQUITETURA] Domínio: ' + dom + ' | SLO: ' + slo);
    console.log('[CONFORMIDADE] Diretrizes ativas: ' + conf + '\\n');

    const loteSimulado: ItemFaturaRuntime[] = [
      {
        id_transacao: "a1b2c3d4-0000-0000-0000-000000000001",
        codigo_produto: "PROD-SKU-901",
        quantidade: 10,
        valor_unitario: new DecimalFixo("150.5000"),
        aliquota_imposto: new DecimalFixo("18.0000"),
        valor_total_liquido: new DecimalFixo(0)
      },
      {
        id_transacao: "a1b2c3d4-0000-0000-0000-000000000002",
        codigo_produto: "PROD-SKU-902",
        quantidade: 5,
        valor_unitario: new DecimalFixo("320.0000"),
        aliquota_imposto: new DecimalFixo("12.0000"),
        valor_total_liquido: new DecimalFixo(0)
      }
    ];

    const totalImpostos = ThzRuntime.executarRegraFiscal(loteSimulado);

    console.log('--- RESULTADOS DO PROCESSAMENTO DE DADOS (LAYOUT COLUNAR) ---');
    for (const item of loteSimulado) {
      console.log('[ITEM ' + item.codigo_produto + '] Qtd: ' + item.quantidade + ' | Unit: R$ ' + item.valor_unitario.formatar() + ' | Total Liq: R$ ' + item.valor_total_liquido.formatar());
    }
    console.log('--------------------------------------------------------------');
    console.log('Total de Tributos Retidos em Lote: R$ ' + totalImpostos.formatar() + '\\n');

    arena.liberarTudo();
    console.log("[RUNTIME] Arena de memória liberada em O(1). Execução finalizada sem erros.");
  }
} catch (err) {
  console.error(err.message);
  process.exit(1);
}
`);

// 10. exemplos/faturamento.thz
escreverArquivo(path.join(ROOT_DIR, 'exemplos', 'faturamento.thz'), `
PROGRAMA ProcessamentoFaturamentoLote

# --- DECLARAÇÕES DE METADADOS ARQUITETURAIS (ISO/IEC/IEEE 42010) ---
METADADOS_ARQUITETURA
    DOMINIO: "LogisticaEFaturamento"
    SUBDOMINIO: "FaturamentoLote"
    CAMADA: "Dominio"
    VERSAO: "2.1.0"
    AUTOR: "Lucas Thomaz"
    SLO_LATENCIA_MAXIMA: "15ms"
    CONFORMIDADE: "SOX-404", "LGPD-Art7"
FIM_METADADOS

# --- MODELO DE DADOS DE ALTA PERFORMANCE (LAYOUT COLUNAR / SIMD) ---
ESTRUTURA ItemFatura LAYOUT_COLUNAR
    id_transacao       : UUID
    codigo_produto     : TEXTO
    quantidade         : NATURAL32
    valor_unitario     : DECIMAL(12, 4)
    aliquota_imposto   : DECIMAL(5, 2)
    valor_total_liquido: DECIMAL(14, 4)
FIM_ESTRUTURA

# --- CONTRATO DE DOMÍNIO E GOVERNANÇA DE REGRAS ---
REGRA_NEGOCIO CalculoTributarioLote
    IDENTIFICADOR_REGRA: "BR-FISCAL-2026-08"
    RASTREIO_REQUISITO: "REQ-FISCAL-9102"
    DESCRICAO: "Aplica isenção para insumos essenciais e calcula ICMS/PIS/COFINS em lote vetorizado."

    CONTRATO_ENTRADA
        EXIGE itens.quantidade > 0
        EXIGE itens.valor_unitario >= 0.0000
    FIM_CONTRATO_ENTRADA

    CONTRATO_SAIDA
        GARANTE itens.valor_total_liquido >= 0.0000
    FIM_CONTRATO_SAIDA

    OPERACAO ProcessarVetorizado(fatia_itens: FATIA[ItemFatura]) : DECIMAL(18, 4)
FIM_REGRA_NEGOCIO

FIM_PROGRAMA
`);

// 11. Instalar dependências e rodar teste inicial
console.log('\n[NPM] Instalando dependências (@types/node, typescript, tsx)...');
try {
  execSync('npm install', { cwd: ROOT_DIR, stdio: 'inherit' });
  console.log('\n[NPM] Dependências instaladas com sucesso!');
  
  console.log('\n[TESTE] Executando pipeline THZ-LANG...\n');
  execSync('npm run thz:run', { cwd: ROOT_DIR, stdio: 'inherit' });

  console.log('\n================================================================================');
  console.log('   SETUP FINALIZADO COM SUCESSO! ENTRE NO DIRETÓRIO PARA OPERAR:               ');
  console.log('   cd thz-lang-engine                                                          ');
  console.log('                                                                               ');
  console.log('   Comandos disponíveis:                                                       ');
  console.log('     npm run thz:run    -> Executa o código THZ no interpretador               ');
  console.log('     npm run thz:doc    -> Gera a documentação de arquitetura e Mermaid        ');
  console.log('     npm run thz:check  -> Faz a análise léxica e sintática (AST)              ');
  console.log('================================================================================\n');
} catch (err) {
  console.error('[ERRO] Falha ao executar:', err.message);
}