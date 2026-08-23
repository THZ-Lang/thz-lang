/* ============================================================
 * THZ-LANG Runtime — Aritmética Exata de Domínio (ISO/IEC 10967)
 * Inteiros escalados BigInt; proibido IEEE 754 binário.
 * ============================================================ */

/** Modos de arredondamento explícitos. Padrão da linguagem: bancário (half-even). */
export enum ModoArredondamento {
  /** Half-even: padrão contábil/financeiro, minimiza viés acumulado. */
  BANCARIO = 'BANCARIO',
  /** Half-up: arredondamento escolar/comercial. */
  MEIA_CIMA = 'MEIA_CIMA',
  /** Truncamento em direção ao zero. */
  TRUNCAR = 'TRUNCAR'
}

export class ErroDecimal extends Error {}

function rescalonar(escalado: bigint, deEscala: number, paraEscala: number, modo: ModoArredondamento): bigint {
  if (paraEscala === deEscala) return escalado;
  if (paraEscala > deEscala) return escalado * 10n ** BigInt(paraEscala - deEscala);

  const fator = 10n ** BigInt(deEscala - paraEscala);
  const negativo = escalado < 0n;
  const absoluto = negativo ? -escalado : escalado;
  const quociente = absoluto / fator;
  const resto = absoluto % fator;

  let arredondado = quociente;
  if (modo !== ModoArredondamento.TRUNCAR && resto !== 0n) {
    const metade = fator / 2n;
    if (resto > metade) {
      arredondado = quociente + 1n;
    } else if (resto === metade && modo === ModoArredondamento.BANCARIO) {
      // Empate: escolhe o vizinho de menor magnitude par (half-even).
      arredondado = quociente % 2n === 0n ? quociente : quociente + 1n;
    } else if (resto === metade && modo === ModoArredondamento.MEIA_CIMA) {
      arredondado = quociente + 1n;
    }
  }
  return negativo ? -arredondado : arredondado;
}

/**
 * Decimal de ponto fixo escalado com precisão arbitrária (BigInt) e
 * escala paramétrica (P,S). Operações normalizam os operandos à maior
 * escala e preservam essa escala no resultado; produtos/divisões são
 * computados exatos e arredondados uma única vez conforme o modo.
 */
export class DecimalFixo {
  public readonly valorEscalado: bigint;
  public readonly escala: number;

  public static readonly MODO_PADRAO = ModoArredondamento.BANCARIO;

  /** Escala canônica do motor quando nenhuma é declarada. */
  public static readonly ESCALA_PADRAO = 4;

  /** Construtor interno por escalado; prefira as fábricas deTexto/deInteiro. */
  constructor(valorEscalado: bigint, escala: number = DecimalFixo.ESCALA_PADRAO) {
    if (escala < 0 || !Number.isInteger(escala)) {
      throw new ErroDecimal('[Erro Decimal] Escala deve ser inteiro não negativo.');
    }
    this.valorEscalado = valorEscalado;
    this.escala = escala;
  }

  public static deTexto(texto: string, escala: number = DecimalFixo.ESCALA_PADRAO): DecimalFixo {
    const limpo = texto.trim();
    if (!/^-?\d*(\.\d*)?$/.test(limpo) || limpo === '' || limpo === '-' || limpo === '.') {
      throw new ErroDecimal("[Erro Decimal] Literal decimal inválido: '" + texto + "'.");
    }
    const negativo = limpo.startsWith('-');
    const semSinal = negativo ? limpo.slice(1) : limpo;
    const [parteInteira = '0', parteFracionaria = ''] = semSinal.split('.');
    if (parteFracionaria.length > escala) {
      throw new ErroDecimal('[Erro Decimal] Literal com mais casas decimais (' + parteFracionaria.length + ') que a escala declarada (' + escala + '): ' + texto);
    }
    const fracao = parteFracionaria.padEnd(escala, '0');
    const magnitude = BigInt(parteInteira || '0') * 10n ** BigInt(escala) + BigInt(fracao === '' ? '0' : fracao);
    return new DecimalFixo(negativo ? -magnitude : magnitude, escala);
  }

  public static deInteiro(valor: bigint, escala: number = DecimalFixo.ESCALA_PADRAO): DecimalFixo {
    return new DecimalFixo(valor * 10n ** BigInt(escala), escala);
  }

  /** Escala combinada de dois operandos: a maior das duas. */
  private static escalaComum(a: DecimalFixo, b: DecimalFixo): number {
    return Math.max(a.escala, b.escala);
  }

  private normalizar(escalaDestino: number, modo: ModoArredondamento = DecimalFixo.MODO_PADRAO): DecimalFixo {
    return new DecimalFixo(rescalonar(this.valorEscalado, this.escala, escalaDestino, modo), escalaDestino);
  }

  public somar(outro: DecimalFixo): DecimalFixo {
    const escala = DecimalFixo.escalaComum(this, outro);
    const a = this.normalizar(escala);
    const b = outro.normalizar(escala);
    return new DecimalFixo(a.valorEscalado + b.valorEscalado, escala);
  }

  public subtrair(outro: DecimalFixo): DecimalFixo {
    const escala = DecimalFixo.escalaComum(this, outro);
    const a = this.normalizar(escala);
    const b = outro.normalizar(escala);
    return new DecimalFixo(a.valorEscalado - b.valorEscalado, escala);
  }

  /**
   * Produto computado exato em BigInt (escala s1+s2) e reescalado uma única
   * vez para a escala comum dos operandos, com arredondamento explícito.
   */
  public multiplicar(outro: DecimalFixo, modo: ModoArredondamento = DecimalFixo.MODO_PADRAO): DecimalFixo {
    const produtoExato = this.valorEscalado * outro.valorEscalado;
    const escalaExata = this.escala + outro.escala;
    const escalaAlvo = DecimalFixo.escalaComum(this, outro);
    return new DecimalFixo(rescalonar(produtoExato, escalaExata, escalaAlvo, modo), escalaAlvo);
  }

  /**
   * Divisão com dígitos de guarda: quociente exato é obtido em precisão
   * ampliada e arredondado uma única vez para a escala comum.
   */
  public dividir(outro: DecimalFixo, modo: ModoArredondamento = DecimalFixo.MODO_PADRAO): DecimalFixo {
    if (outro.valorEscalado === 0n) {
      throw new ErroDecimal('[Erro Decimal] Divisão por zero.');
    }
    const escala = DecimalFixo.escalaComum(this, outro);
    const a = this.normalizar(escala).valorEscalado;
    const b = outro.normalizar(escala).valorEscalado;
    const numeradorAmpliado = a * 10n ** BigInt(escala);
    const negativoResultado = (numeradorAmpliado < 0n) !== (b < 0n);
    const num = negativoNumerico(numeradorAmpliado);
    const den = negativoNumerico(b);
    const quociente = num / den;
    const resto = num % den;

    let escaladoFinal = quociente;
    if (modo !== ModoArredondamento.TRUNCAR && resto !== 0n) {
      const metade = den / 2n;
      if (resto > metade) escaladoFinal += 1n;
      else if (resto === metade && modo === ModoArredondamento.BANCARIO) escaladoFinal = quociente % 2n === 0n ? quociente : quociente + 1n;
      else if (resto === metade && modo === ModoArredondamento.MEIA_CIMA) escaladoFinal += 1n;
    }
    return new DecimalFixo(negativoResultado ? -escaladoFinal : escaladoFinal, escala);
  }

  public negar(): DecimalFixo {
    return new DecimalFixo(-this.valorEscalado, this.escala);
  }

  public abs(): DecimalFixo {
    return new DecimalFixo(this.valorEscalado < 0n ? -this.valorEscalado : this.valorEscalado, this.escala);
  }

  /** Comparações normalizam à escala comum antes de comparar escalados. */
  public comparar(outro: DecimalFixo): number {
    const escala = DecimalFixo.escalaComum(this, outro);
    const a = this.normalizar(escala).valorEscalado;
    const b = outro.normalizar(escala).valorEscalado;
    return a === b ? 0 : a < b ? -1 : 1;
  }

  public formatar(): string {
    const divisor = 10n ** BigInt(this.escala);
    const negativo = this.valorEscalado < 0n;
    const absoluto = negativo ? -this.valorEscalado : this.valorEscalado;
    const inteiro = absoluto / divisor;
    if (this.escala === 0) return (negativo ? '-' : '') + inteiro.toString();
    const fracao = (absoluto % divisor).toString().padStart(this.escala, '0');
    return (negativo ? '-' : '') + inteiro.toString() + '.' + fracao;
  }

  /** Reescala mantendo o valor matemático (com arredondamento se necessário). */
  public paraEscala(escala: number, modo: ModoArredondamento = DecimalFixo.MODO_PADRAO): DecimalFixo {
    return this.normalizar(escala, modo);
  }
}

function negativoNumerico(v: bigint): bigint {
  return v < 0n ? -v : v;
}

/* ============================================================
 * MONETÁRIO — quantias cientes de moeda (ISO 4217)
 * ============================================================ */

export interface DefinicaoMoeda {
  codigo: string;
  /** Casas decimais menores da unidade (ISO 4217 exponent). */
  casas: number;
}

/** Subconjunto essencial ISO 4217; extensível sob demanda. */
export const TABELA_MOEDAS_ISO4217: Readonly<Record<string, DefinicaoMoeda>> = Object.freeze({
  BRL: { codigo: 'BRL', casas: 2 },
  USD: { codigo: 'USD', casas: 2 },
  EUR: { codigo: 'EUR', casas: 2 },
  GBP: { codigo: 'GBP', casas: 2 },
  JPY: { codigo: 'JPY', casas: 0 },
  CHF: { codigo: 'CHF', casas: 2 },
  CNY: { codigo: 'CNY', casas: 2 }
});

export class ErroMonetario extends Error {}

/** Quantia monetária amarrada a uma moeda ISO 4217. Mistura de moedas é erro. */
export class Monetario {
  private constructor(public readonly quantia: DecimalFixo, public readonly moeda: DefinicaoMoeda) {}

  public static deTexto(texto: string, codigoMoeda: string): Monetario {
    const definicao = TABELA_MOEDAS_ISO4217[codigoMoeda];
    if (!definicao) {
      throw new ErroMonetario("[Erro Monetário] Código de moeda não reconhecido (ISO 4217): '" + codigoMoeda + "'.");
    }
    return new Monetario(DecimalFixo.deTexto(texto, definicao.casas), definicao);
  }

  public static deInteiro(valor: bigint, codigoMoeda: string): Monetario {
    const definicao = TABELA_MOEDAS_ISO4217[codigoMoeda];
    if (!definicao) {
      throw new ErroMonetario("[Erro Monetário] Código de moeda não reconhecido (ISO 4217): '" + codigoMoeda + "'.");
    }
    return new Monetario(DecimalFixo.deInteiro(valor, definicao.casas), definicao);
  }

  private exigirMesmaMoeda(outro: Monetario, operacao: string): void {
    if (this.moeda.codigo !== outro.moeda.codigo) {
      throw new ErroMonetario('[Erro Monetário] Impossível ' + operacao + ' ' + this.moeda.codigo + ' com ' + outro.moeda.codigo + '. Converta explicitamente antes da operação.');
    }
  }

  public somar(outro: Monetario): Monetario {
    this.exigirMesmaMoeda(outro, 'somar');
    return new Monetario(this.quantia.somar(outro.quantia), this.moeda);
  }

  public subtrair(outro: Monetario): Monetario {
    this.exigirMesmaMoeda(outro, 'subtrair');
    return new Monetario(this.quantia.subtrair(outro.quantia), this.moeda);
  }

  /** Multiplicação por fator escalar (ex.: quantidade ou taxa adimensional). */
  public multiplicar(fator: DecimalFixo, modo: ModoArredondamento = DecimalFixo.MODO_PADRAO): Monetario {
    return new Monetario(this.quantia.multiplicar(fator, modo), this.moeda);
  }

  /** Divisão por divisor escalar; razão entre monetários é escalar. */
  public dividir(divisor: DecimalFixo | Monetario, modo: ModoArredondamento = DecimalFixo.MODO_PADRAO): DecimalFixo | Monetario {
    if (divisor instanceof Monetario) {
      this.exigirMesmaMoeda(divisor, 'dividir');
      return this.quantia.dividir(divisor.quantia, modo);
    }
    return new Monetario(this.quantia.dividir(divisor, modo), this.moeda);
  }

  public comparar(outro: Monetario): number {
    this.exigirMesmaMoeda(outro, 'comparar');
    return this.quantia.comparar(outro.quantia);
  }

  public negar(): Monetario {
    return new Monetario(this.quantia.negar(), this.moeda);
  }

  public formatar(): string {
    return this.quantia.formatar() + ' ' + this.moeda.codigo;
  }
}

/* ============================================================
 * DATA / DATA_HORA — tipos temporais gregorianos (BigInt epoch)
 * ============================================================ */

export class ErroData extends Error {}

function ehBissexto(ano: number): boolean {
  return (ano % 4 === 0 && ano % 100 !== 0) || ano % 400 === 0;
}

function diasNoMes(ano: number, mes: number): number {
  const tabela = [31, ehBissexto(ano) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  return tabela[mes - 1];
}

/** Howard Hinnant: dias desde civil 1970-01-01 */
function diasDesdeCivil(ano: number, mes: number, dia: number): bigint {
  let y = ano;
  let m = mes;
  y -= m <= 2 ? 1 : 0;
  const era = Math.floor((y >= 0 ? y : y - 399) / 400);
  const yoe = y - era * 400;
  const mp = m > 2 ? m - 3 : m + 9;
  const doy = Math.floor((153 * mp + 2) / 5) + dia - 1;
  const doe = yoe * 365 + Math.floor(yoe / 4) - Math.floor(yoe / 100) + doy;
  return BigInt(era * 146097 + doe - 719468);
}

function civilDeDias(z: bigint): { ano: number; mes: number; dia: number } {
  let zz = Number(z) + 719468;
  const era = Math.floor((zz >= 0 ? zz : zz - 146096) / 146097);
  const doe = zz - era * 146097;
  const yoe = Math.floor((doe - Math.floor(doe / 1460) + Math.floor(doe / 36524) - Math.floor(doe / 146096)) / 365);
  let y = yoe + era * 400;
  const doy = doe - (365 * yoe + Math.floor(yoe / 4) - Math.floor(yoe / 100));
  const mp = Math.floor((5 * doy + 2) / 153);
  const d = doy - Math.floor((153 * mp + 2) / 5) + 1;
  const m = mp + (mp < 10 ? 3 : -9);
  y += m <= 2 ? 1 : 0;
  return { ano: y, mes: m, dia: d };
}

export class DataThz {
  constructor(public readonly epochDias: bigint) {}

  public static deComponentes(ano: number, mes: number, dia: number): DataThz {
    if (!Number.isInteger(ano) || !Number.isInteger(mes) || !Number.isInteger(dia)) {
      throw new ErroData('[Erro Data] Componentes de data devem ser inteiros.');
    }
    if (mes < 1 || mes > 12) throw new ErroData('[Erro Data] Mês inválido: ' + mes);
    const dim = diasNoMes(ano, mes);
    if (dia < 1 || dia > dim) throw new ErroData('[Erro Data] Dia inválido: ' + dia + ' para ' + ano + '-' + String(mes).padStart(2, '0'));
    return new DataThz(diasDesdeCivil(ano, mes, dia));
  }

  public static deTexto(texto: string): DataThz {
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(texto.trim());
    if (!m) throw new ErroData("[Erro Data] Formato de DATA inválido (esperado AAAA-MM-DD): '" + texto + "'");
    return DataThz.deComponentes(Number.parseInt(m[1], 10), Number.parseInt(m[2], 10), Number.parseInt(m[3], 10));
  }

  public get componentes(): { ano: number; mes: number; dia: number } {
    return civilDeDias(this.epochDias);
  }

  public get ano(): number { return this.componentes.ano; }
  public get mes(): number { return this.componentes.mes; }
  public get dia(): number { return this.componentes.dia; }

  public adicionarDias(dias: bigint | number): DataThz {
    const d = typeof dias === 'bigint' ? dias : BigInt(dias);
    return new DataThz(this.epochDias + d);
  }

  public diferencaDias(outro: DataThz): bigint {
    return this.epochDias - outro.epochDias;
  }

  public diaDaSemana(): number {
    // 1970-01-01 foi quinta-feira (4), domingo=0
    const v = Number(this.epochDias + 4n) % 7;
    return v < 0 ? v + 7 : v;
  }

  public comparar(outro: DataThz): number {
    return this.epochDias === outro.epochDias ? 0 : this.epochDias < outro.epochDias ? -1 : 1;
  }

  public formatar(): string {
    const { ano, mes, dia } = this.componentes;
    return String(ano).padStart(4, '0') + '-' + String(mes).padStart(2, '0') + '-' + String(dia).padStart(2, '0');
  }
}

export class DataHoraThz {
  constructor(public readonly epochSegundos: bigint) {}

  public static deComponentes(ano: number, mes: number, dia: number, hora: number, minuto: number, segundo: number = 0): DataHoraThz {
    if ([hora, minuto, segundo].some((v) => !Number.isInteger(v))) throw new ErroData('[Erro DataHora] Hora/minuto/segundo devem ser inteiros.');
    if (hora < 0 || hora > 23) throw new ErroData('[Erro DataHora] Hora inválida: ' + hora);
    if (minuto < 0 || minuto > 59) throw new ErroData('[Erro DataHora] Minuto inválido: ' + minuto);
    if (segundo < 0 || segundo > 59) throw new ErroData('[Erro DataHora] Segundo inválido: ' + segundo);
    const dias = diasDesdeCivil(ano, mes, dia);
    // valida mês/dia via DataThz
    DataThz.deComponentes(ano, mes, dia);
    const seg = dias * 86400n + BigInt(hora) * 3600n + BigInt(minuto) * 60n + BigInt(segundo);
    return new DataHoraThz(seg);
  }

  public static deTexto(texto: string): DataHoraThz {
    const m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(texto.trim());
    if (!m) throw new ErroData("[Erro DataHora] Formato de DATA_HORA inválido (esperado AAAA-MM-DDTHH:MM[:SS]): '" + texto + "'");
    return DataHoraThz.deComponentes(
      Number.parseInt(m[1], 10), Number.parseInt(m[2], 10), Number.parseInt(m[3], 10),
      Number.parseInt(m[4], 10), Number.parseInt(m[5], 10), m[6] ? Number.parseInt(m[6], 10) : 0
    );
  }

  public get data(): DataThz {
    const dias = this.epochSegundos >= 0n ? this.epochSegundos / 86400n : (this.epochSegundos - 86399n) / 86400n;
    return new DataThz(dias);
  }

  public get hora(): number { return Number((this.epochSegundos % 86400n + 86400n) % 86400n / 3600n); }
  public get minuto(): number { return Number(((this.epochSegundos % 86400n + 86400n) % 86400n % 3600n) / 60n); }
  public get segundo(): number { return Number((this.epochSegundos % 86400n + 86400n) % 86400n % 60n); }

  public adicionarSegundos(s: bigint | number): DataHoraThz {
    return new DataHoraThz(this.epochSegundos + (typeof s === 'bigint' ? s : BigInt(s)));
  }
  public adicionarMinutos(m: bigint | number): DataHoraThz { return this.adicionarSegundos((typeof m === 'bigint' ? m : BigInt(m)) * 60n); }
  public adicionarHoras(h: bigint | number): DataHoraThz { return this.adicionarSegundos((typeof h === 'bigint' ? h : BigInt(h)) * 3600n); }

  public comparar(outro: DataHoraThz): number {
    return this.epochSegundos === outro.epochSegundos ? 0 : this.epochSegundos < outro.epochSegundos ? -1 : 1;
  }

  public formatar(): string {
    const dias = this.epochSegundos >= 0n ? this.epochSegundos / 86400n : (this.epochSegundos - 86399n) / 86400n;
    const resto = (this.epochSegundos % 86400n + 86400n) % 86400n;
    const { ano, mes, dia } = civilDeDias(dias);
    const h = Number(resto / 3600n);
    const m = Number((resto % 3600n) / 60n);
    const s = Number(resto % 60n);
    const base = String(ano).padStart(4, '0') + '-' + String(mes).padStart(2, '0') + '-' + String(dia).padStart(2, '0') + 'T' + String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0');
    return s !== 0 ? base + ':' + String(s).padStart(2, '0') : base;
  }
}

/* ============================================================
 * ARENA DE MEMÓRIA — alocação contígua, descarte O(1)
 * ============================================================ */

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
      throw new Error('[Runtime THZ] Estouro de capacidade da Arena de Memória.');
    }
    return endereco;
  }

  public liberarTudo(): void {
    this.offset = 0;
  }

  public get capacidadeBytes(): number {
    return this.buffer.byteLength;
  }

  public get utilizacaoBytes(): number {
    return this.offset;
  }
}
