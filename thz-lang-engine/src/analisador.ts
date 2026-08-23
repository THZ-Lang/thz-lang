import {
  ProgramaAST,
  EstruturaAST,
  RegraNegocioAST,
  OperacaoAST,
  ProcedimentoAST,
  ExprAST,
  ComandoAST,
  ClausulaContratoAST
} from './types.js';
import {
  TipoThz,
  CategoriaTipo,
  TIPOS_PRIMITIVOS,
  TIPO_LITERAL_INTEIRO,
  analisarNomeTipo,
  saoCompativeis,
  ehInteiro,
  ehNumerico,
  descrever
} from './tipos.js';

export interface ErroSemantico {
  linha: number;
  coluna: number;
  mensagem: string;
}

export interface OpcoesAnalise {
  /** Lint de governança: reprova programas sem pragma, rastreabilidade ou contratos. */
  estrito?: boolean;
}

const TIPO_LOGICO = TIPOS_PRIMITIVOS.LOGICO;
const TIPO_TEXTO = TIPOS_PRIMITIVOS.TEXTO;
const TIPO_NULO: TipoThz = { nome: '<nulo>', categoria: CategoriaTipo.PRIMITIVO };
const TIPO_INTEIRO_GENERICO: TipoThz = { nome: 'INTEIRO64', categoria: CategoriaTipo.INTEIRO };
const TIPO_DATA: TipoThz = TIPOS_PRIMITIVOS.DATA;
const TIPO_DATA_HORA: TipoThz = TIPOS_PRIMITIVOS.DATA_HORA;

class EscopoTipos {
  private simbolos = new Map<string, TipoThz>();

  constructor(public pai?: EscopoTipos) {}

  public definir(nome: string, tipo: TipoThz, linha: number, coluna: number, erros: ErroSemantico[]): void {
    if (this.simbolos.has(nome)) {
      erros.push({ linha, coluna, mensagem: "Redeclaração de '" + nome + "' no mesmo escopo." });
      return;
    }
    this.simbolos.set(nome, tipo);
  }

  public resolver(nome: string): TipoThz | undefined {
    let atual: EscopoTipos | undefined = this;
    while (atual) {
      const t = atual.simbolos.get(nome);
      if (t) return t;
      atual = atual.pai;
    }
    return undefined;
  }
}

interface ContextoOperacao {
  regra: RegraNegocioAST;
  operacao: OperacaoAST;
}
interface ContextoProcedimento {
  procedimento: ProcedimentoAST;
}
type ContextoExec = ContextoOperacao | ContextoProcedimento;

function ehContextoOperacao(c: ContextoExec): c is ContextoOperacao {
  return (c as ContextoOperacao).operacao !== undefined;
}

/* Signaturas stdlib para análise */
interface SigStdlib { paramMin: number; paramMax: number; retorno: TipoThz | ((args: TipoThz[]) => TipoThz); }
const SIG_STDLIB: Record<string, SigStdlib> = {
  'TEXTO.comprimento': { paramMin: 1, paramMax: 1, retorno: TIPO_INTEIRO_GENERICO },
  'TEXTO.maiusculas': { paramMin: 1, paramMax: 1, retorno: TIPO_TEXTO },
  'TEXTO.minusculas': { paramMin: 1, paramMax: 1, retorno: TIPO_TEXTO },
  'TEXTO.aparar': { paramMin: 1, paramMax: 1, retorno: TIPO_TEXTO },
  'TEXTO.contem': { paramMin: 2, paramMax: 2, retorno: TIPO_LOGICO },
  'TEXTO.subtexto': { paramMin: 2, paramMax: 3, retorno: TIPO_TEXTO },
  'TEXTO.substituir': { paramMin: 3, paramMax: 3, retorno: TIPO_TEXTO },
  'TEXTO.dividir': { paramMin: 2, paramMax: 2, retorno: { nome: 'FATIA[TEXTO]', categoria: CategoriaTipo.FATIA, interno: 'TEXTO' } },
  'TEXTO.juntar': { paramMin: 2, paramMax: 2, retorno: TIPO_TEXTO },
  'MATEMATICA.abs': { paramMin: 1, paramMax: 1, retorno: (a) => ehInteiro(a[0]) ? TIPO_INTEIRO_GENERICO : a[0] },
  'MATEMATICA.min': { paramMin: 2, paramMax: 2, retorno: TIPO_INTEIRO_GENERICO },
  'MATEMATICA.max': { paramMin: 2, paramMax: 2, retorno: TIPO_INTEIRO_GENERICO },
  'MATEMATICA.potencia': { paramMin: 2, paramMax: 2, retorno: TIPO_INTEIRO_GENERICO },
  'MATEMATICA.raiz': { paramMin: 1, paramMax: 1, retorno: TIPO_INTEIRO_GENERICO },
  'MATEMATICA.arredondar': { paramMin: 2, paramMax: 2, retorno: (a) => a[0] },
  'MATEMATICA.aleatorio': { paramMin: 1, paramMax: 1, retorno: TIPO_INTEIRO_GENERICO },
  'DATA.hoje': { paramMin: 0, paramMax: 0, retorno: TIPO_DATA },
  'DATA.agora': { paramMin: 0, paramMax: 0, retorno: TIPO_DATA_HORA },
  'DATA.criar': { paramMin: 3, paramMax: 3, retorno: TIPO_DATA },
  'DATA.criarDataHora': { paramMin: 5, paramMax: 6, retorno: TIPO_DATA_HORA },
  'DATA.adicionarDias': { paramMin: 2, paramMax: 2, retorno: TIPO_DATA },
  'DATA.adicionarHoras': { paramMin: 2, paramMax: 2, retorno: TIPO_DATA_HORA },
  'DATA.diferencaDias': { paramMin: 2, paramMax: 2, retorno: TIPO_INTEIRO_GENERICO },
  'DATA.ano': { paramMin: 1, paramMax: 1, retorno: TIPO_INTEIRO_GENERICO },
  'DATA.mes': { paramMin: 1, paramMax: 1, retorno: TIPO_INTEIRO_GENERICO },
  'DATA.dia': { paramMin: 1, paramMax: 1, retorno: TIPO_INTEIRO_GENERICO },
  'DATA.diaDaSemana': { paramMin: 1, paramMax: 1, retorno: TIPO_INTEIRO_GENERICO },
  'DATA.texto': { paramMin: 1, paramMax: 1, retorno: TIPO_TEXTO },
  'TELA.renderizarFormulario': { paramMin: 2, paramMax: 2, retorno: TIPO_TEXTO },
  'TELA.alerta': { paramMin: 2, paramMax: 2, retorno: TIPO_TEXTO },
  'TELA.confirmar': { paramMin: 2, paramMax: 2, retorno: TIPO_LOGICO },
  'TELA.pedirTexto': { paramMin: 2, paramMax: 2, retorno: TIPO_TEXTO },
};

export class AnalisadorSemantico {
  private erros: ErroSemantico[] = [];
  private estruturas = new Map<string, EstruturaAST>();
  private enumeracoes = new Set<string>();

  constructor(private ast: ProgramaAST) {}

  public analisar(opcoes: OpcoesAnalise = {}): ErroSemantico[] {
    this.erros = [];
    this.validarPragma();
    this.coletarEnumeracoes();
    this.coletarEstruturas();
    this.validarEstruturas();
    for (const regra of this.ast.regras) {
      this.validarRegra(regra);
      if (opcoes.estrito && regra.clausulasEntrada.length === 0 && regra.clausulasSaida.length === 0) {
        this.erros.push({ linha: 1, coluna: 1, mensagem: "[Governança] Regra '" + regra.nome + "' sem contratos formais (EXIGE/GARANTE)." });
      }
      const vistas = new Set<string>();
      for (const operacao of regra.operacoes) {
        if (vistas.has(operacao.nome)) {
          this.erros.push({ linha: 1, coluna: 1, mensagem: "Operação duplicada '" + operacao.nome + "' na regra '" + regra.nome + "'." });
        }
        vistas.add(operacao.nome);
      }
    }
    this.validarProcedimentos();
    if (opcoes.estrito) this.aplicarLintEstrito();
    return this.deduplicar(this.erros);
  }

  private deduplicar(erros: ErroSemantico[]): ErroSemantico[] {
    const unicos = new Map<string, ErroSemantico>();
    for (const erro of erros) {
      unicos.set(erro.linha + ':' + erro.coluna + ':' + erro.mensagem, erro);
    }
    return [...unicos.values()];
  }

  /* ---------------- Programa e estruturas ---------------- */

  private validarPragma(): void {
    const v = this.ast.versaoLinguagem;
    if (!v) return;
    if (v !== '2.2' && v !== '2.2.0' && v !== '2.3' && v !== '2.3.0') {
      if (!/^\d+(\.\d+){1,2}$/.test(v)) {
        this.erros.push({ linha: 1, coluna: 1, mensagem: "VERSAO_LINGUAGEM inválida: '" + v + "'. Use semver major.minor.patch." });
      }
    }
  }

  private coletarEnumeracoes(): void {
    for (const enumeracao of this.ast.enumeracoes) {
      if (this.enumeracoes.has(enumeracao.nome)) {
        this.erros.push({ linha: 1, coluna: 1, mensagem: "Enumeração duplicada: '" + enumeracao.nome + "'." });
        continue;
      }
      if (this.ast.estruturas.some((e) => e.nome === enumeracao.nome)) {
        this.erros.push({ linha: 1, coluna: 1, mensagem: "Nome '" + enumeracao.nome + "' conflita com estrutura declarada." });
        continue;
      }
      if (enumeracao.membros.length === 0) {
        this.erros.push({ linha: 1, coluna: 1, mensagem: "Enumeração '" + enumeracao.nome + "' sem membros." });
      }
      this.enumeracoes.add(enumeracao.nome);
    }
  }

  private coletarEstruturas(): void {
    for (const estrutura of this.ast.estruturas) {
      if (this.estruturas.has(estrutura.nome)) {
        this.erros.push({ linha: 1, coluna: 1, mensagem: "Estrutura duplicada: '" + estrutura.nome + "'." });
        continue;
      }
      this.estruturas.set(estrutura.nome, estrutura);
    }
  }

  private tipoValido(nomeVerbatim: string, linha: number, coluna: number): TipoThz | undefined {
    const resolvido = analisarNomeTipo(nomeVerbatim);
    if (resolvido) return resolvido;

    if (this.enumeracoes.has(nomeVerbatim)) {
      return { nome: nomeVerbatim, categoria: CategoriaTipo.ENUMERACAO };
    }
    if (this.estruturas.has(nomeVerbatim)) {
      return { nome: nomeVerbatim, categoria: CategoriaTipo.REGISTRO };
    }
    const fatia = /^FATIA\s*\[\s*(\w+)\s*\]$/.exec(nomeVerbatim);
    if (fatia && (this.estruturas.has(fatia[1]) || TIPOS_PRIMITIVOS[fatia[1]] || fatia[1] === 'DATA' || fatia[1] === 'DATA_HORA' || fatia[1] === 'TEXTO')) {
      return { nome: nomeVerbatim.replace(/\s+/g, ''), categoria: CategoriaTipo.FATIA, interno: fatia[1] };
    }

    this.erros.push({ linha, coluna, mensagem: "Tipo desconhecido: '" + nomeVerbatim + "'." });
    return undefined;
  }

  private validarEstruturas(): void {
    for (const estrutura of this.ast.estruturas) {
      const vistas = new Set<string>();
      for (const campo of estrutura.campos) {
        if (vistas.has(campo.nome)) {
          this.erros.push({ linha: 1, coluna: 1, mensagem: "Campo duplicado '" + campo.nome + "' na estrutura '" + estrutura.nome + "'." });
        }
        vistas.add(campo.nome);
        this.tipoValido(campo.tipo, 1, 1);
      }
      this.validarInvariantes(estrutura);
    }
  }

  /** INVARIANTE: validadas contra o ambiente dos campos da própria estrutura. */
  private validarInvariantes(estrutura: EstruturaAST): void {
    if (estrutura.invariantes.length === 0) return;
    const escopo = new EscopoTipos();
    for (const campo of estrutura.campos) {
      escopo.definir(campo.nome, this.tipoValido(campo.tipo, 1, 1) ?? TIPO_NULO, 1, 1, []);
    }
    for (const invariante of estrutura.invariantes) {
      const tipo = this.inferir(invariante.expressao, escopo);
      this.exigirLogico(tipo, "invariante de '" + estrutura.nome + "'", invariante.linha, invariante.coluna);
    }
  }

  /* ---------------- Regras ---------------- */

  private validarRegra(regra: RegraNegocioAST): void {
    for (const operacao of regra.operacoes) {
      const contexto: ContextoOperacao = { regra, operacao };
      const escopoRaiz = new EscopoTipos();
      for (const parametro of operacao.parametros) {
        escopoRaiz.definir(parametro.nome, this.tipoValido(parametro.tipo, 1, 1) ?? TIPO_NULO, 1, 1, this.erros);
      }
      for (const clausula of regra.clausulasEntrada) this.validarClausula(clausula, escopoRaiz);

      const escopoSaida = new EscopoTipos(escopoRaiz);
      const retornoTipo = this.tipoValido(operacao.tipoRetorno, 1, 1);
      if (retornoTipo) {
        escopoSaida.definir('RESULTADO', retornoTipo, 1, 1, this.erros);
      }
      for (const clausula of regra.clausulasSaida) this.validarClausula(clausula, escopoSaida);

      this.validarBlocoFilho(operacao.corpo, escopoRaiz, contexto);
    }
  }

  private validarClausula(clausula: ClausulaContratoAST, escopo: EscopoTipos): void {
    const tipo = this.inferir(clausula.expressao, escopo);
    if (tipo && tipo.nome !== TIPO_LOGICO.nome && tipo.nome !== TIPO_NULO.nome) {
      this.erros.push({
        linha: clausula.linha,
        coluna: clausula.coluna,
        mensagem: "Cláusula '" + clausula.tipoClausula + "' deve ser lógica; obtido " + descrever(tipo) + '.'
      });
    }
  }

  private validarProcedimentos(): void {
    const procedimentos = this.ast.procedimentos ?? [];
    const vistas = new Set<string>();
    for (const proc of procedimentos) {
      if (vistas.has(proc.nome)) this.erros.push({ linha: 1, coluna: 1, mensagem: "Procedimento duplicado: '" + proc.nome + "'." });
      vistas.add(proc.nome);
      // conflito com regras/enumerações?
      if (this.ast.regras.some((r) => r.nome === proc.nome)) this.erros.push({ linha: 1, coluna: 1, mensagem: "Nome '" + proc.nome + "' conflita com regra declarada." });
      const escopo = new EscopoTipos();
      for (const p of proc.parametros) escopo.definir(p.nome, this.tipoValido(p.tipo, 1, 1) ?? TIPO_NULO, 1, 1, this.erros);
      const ctx: ContextoProcedimento = { procedimento: proc };
      this.validarBlocoFilho(proc.corpo, escopo, ctx);
    }
  }

  /* ---------------- Comandos ---------------- */

  private validarBlocoFilho(comandos: ComandoAST[], escopoPai: EscopoTipos, contexto: ContextoExec): void {
    const escopo = new EscopoTipos(escopoPai);
    for (const comando of comandos) {
      this.validarComando(comando, escopo, contexto);
    }
  }

  private validarComando(cmd: ComandoAST, escopo: EscopoTipos, contexto: ContextoExec): void {
    switch (cmd.tipoComando) {
      case 'DECL_VARIAVEL': {
        const declarado = this.tipoValido(cmd.tipoDado, cmd.linha, cmd.coluna);
        const init = this.inferir(cmd.inicializacao, escopo);
        if (declarado && init && !saoCompativeis(init, declarado)) {
          this.erros.push({
            linha: cmd.linha,
            coluna: cmd.coluna,
            mensagem: "Inicialização de '" + cmd.nome + "' incompatível: " + descrever(init) + ' → ' + descrever(declarado) + '.'
          });
        }
        escopo.definir(cmd.nome, declarado ?? TIPO_NULO, cmd.linha, cmd.coluna, this.erros);
        return;
      }

      case 'ATRIBUICAO': {
        const alvo = this.resolverCaminho(cmd.alvo, escopo, cmd.linha, cmd.coluna);
        const valor = this.inferir(cmd.expressao, escopo);
        if (alvo && valor && !saoCompativeis(valor, alvo)) {
          this.erros.push({
            linha: cmd.linha,
            coluna: cmd.coluna,
            mensagem: 'Atribuição incompatível: ' + descrever(valor) + ' → ' + descrever(alvo) + " em '" + cmd.alvo.join('.') + "'."
          });
        }
        return;
      }

      case 'SE': {
        this.exigirLogico(this.inferir(cmd.condicao, escopo), "condição do 'SE'", cmd.linha, cmd.coluna);
        this.validarBlocoFilho(cmd.entao, escopo, contexto);
        this.validarBlocoFilho(cmd.senao, escopo, contexto);
        return;
      }

      case 'ENQUANTO': {
        this.exigirLogico(this.inferir(cmd.condicao, escopo), "condição do 'ENQUANTO'", cmd.linha, cmd.coluna);
        this.validarBlocoFilho(cmd.corpo, escopo, contexto);
        return;
      }

      case 'PARA': {
        const iniTipo = this.inferir(cmd.inicio, escopo);
        const fimTipo = this.inferir(cmd.fim, escopo);
        if (iniTipo && !ehInteiro(iniTipo)) this.erros.push({ linha: cmd.linha, coluna: cmd.coluna, mensagem: "'PARA' exige início inteiro; obtido " + descrever(iniTipo) + '.' });
        if (fimTipo && !ehInteiro(fimTipo)) this.erros.push({ linha: cmd.linha, coluna: cmd.coluna, mensagem: "'PARA' exige fim inteiro; obtido " + descrever(fimTipo) + '.' });
        if (cmd.passo) {
          const p = this.inferir(cmd.passo, escopo);
          if (p && !ehInteiro(p)) this.erros.push({ linha: cmd.linha, coluna: cmd.coluna, mensagem: "'PASSO' exige inteiro; obtido " + descrever(p) + '.' });
        }
        const escopoIter = new EscopoTipos(escopo);
        escopoIter.definir(cmd.variavel, TIPO_INTEIRO_GENERICO, cmd.linha, cmd.coluna, this.erros);
        this.validarBlocoFilho(cmd.corpo, escopoIter, contexto);
        return;
      }

      case 'VETORIZAR_PARA': {
        const fonte = escopo.resolver(cmd.fonte[0]);
        if (!fonte) {
          this.erros.push({ linha: cmd.linha, coluna: cmd.coluna, mensagem: "Fonte não declarada: '" + cmd.fonte[0] + "'." });
          return;
        }
        if (fonte.categoria !== CategoriaTipo.FATIA || !fonte.interno) {
          this.erros.push({ linha: cmd.linha, coluna: cmd.coluna, mensagem: "Fonte do 'VETORIZAR_PARA' deve ser FATIA[T]; obtido " + descrever(fonte) + '.' });
          return;
        }
        const escopoIteracao = new EscopoTipos(escopo);
        const interno = TIPOS_PRIMITIVOS[fonte.interno] ? { nome: fonte.interno, categoria: CategoriaTipo.PRIMITIVO } as TipoThz : { nome: fonte.interno, categoria: CategoriaTipo.REGISTRO, interno: fonte.interno } as TipoThz;
        escopoIteracao.definir(cmd.variavel, interno, cmd.linha, cmd.coluna, this.erros);
        this.validarBlocoFilho(cmd.corpo, escopoIteracao, contexto);
        return;
      }

      case 'BLOCO_MEMORIA':
        this.validarBlocoFilho(cmd.corpo, escopo, contexto);
        return;

      case 'EXIBA':
        this.inferir(cmd.expressao, escopo);
        return;

      case 'LER': {
        const alvo = this.resolverCaminho(cmd.alvo, escopo, cmd.linha, cmd.coluna);
        if (alvo) {
          const legivel = [CategoriaTipo.PRIMITIVO, CategoriaTipo.INTEIRO, CategoriaTipo.DECIMAL].includes(alvo.categoria) || alvo.nome === 'DATA' || alvo.nome === 'DATA_HORA';
          if (!legivel && alvo.categoria !== CategoriaTipo.PRIMITIVO) {
            // check nome DATA/DATA_HORA
            if (alvo.nome !== 'DATA' && alvo.nome !== 'DATA_HORA' && alvo.nome !== 'TEXTO' && alvo.nome !== 'LOGICO') {
              this.erros.push({ linha: cmd.linha, coluna: cmd.coluna, mensagem: "LER exige alvo TEXTO/numérico/DATA; obtido " + descrever(alvo) + '.' });
            }
          }
        }
        return;
      }

      case 'CHAMADA':
        this.inferir(cmd.expressao, escopo);
        return;

      case 'RETORNE': {
        if (!ehContextoOperacao(contexto)) {
          if (cmd.expressao) {
            this.erros.push({ linha: cmd.linha, coluna: cmd.coluna, mensagem: 'RETORNE com valor não permitido dentro de PROCEDIMENTO.' });
          }
          return;
        }
        if (!cmd.expressao) return;
        const retornoDeclarado = this.tipoValido(contexto.operacao.tipoRetorno, 1, 1);
        const retornado = this.inferir(cmd.expressao, escopo);

        // RESULTADO[T,E]: o canal RETORNE alimenta T (sucesso).
        const canalSucesso =
          retornoDeclarado && retornoDeclarado.categoria === CategoriaTipo.RESULTADO
            ? this.tipoValido(retornoDeclarado.interno ?? 'NULO', 1, 1)
            : retornoDeclarado;

        if (canalSucesso && retornado && !saoCompativeis(retornado, canalSucesso)) {
          this.erros.push({
            linha: cmd.linha,
            coluna: cmd.coluna,
            mensagem: 'RETORNE incompatível: ' + descrever(retornado) + ' → ' + descrever(canalSucesso) + '.'
          });
        }
        return;
      }

      case 'FALHAR_COM': {
        if (!ehContextoOperacao(contexto)) {
          this.erros.push({ linha: cmd.linha, coluna: cmd.coluna, mensagem: 'FALHAR_COM não permitido dentro de PROCEDIMENTO (exige RESULTADO).' });
          return;
        }
        const retornoDeclarado = this.tipoValido(contexto.operacao.tipoRetorno, cmd.linha, cmd.coluna);
        if (!retornoDeclarado || retornoDeclarado.categoria !== CategoriaTipo.RESULTADO) {
          this.erros.push({
            linha: cmd.linha,
            coluna: cmd.coluna,
            mensagem: "FALHAR_COM exige operação com retorno 'RESULTADO[T,E]'; declarado " + descrever(retornoDeclarado) + '.'
          });
          return;
        }
        const canalErro = this.tipoValido(retornoDeclarado.internoErro ?? 'TEXTO', 1, 1);
        const valorErro = this.inferir(cmd.expressao, escopo);
        if (canalErro && valorErro && !saoCompativeis(valorErro, canalErro)) {
          this.erros.push({
            linha: cmd.linha,
            coluna: cmd.coluna,
            mensagem: 'FALHAR_COM incompatível com o canal de erro: ' + descrever(valorErro) + ' → ' + descrever(canalErro) + '.'
          });
        }
        return;
      }
    }
  }

  /* ---------------- Expressões ---------------- */

  private exigirLogico(tipo: TipoThz | undefined, contexto: string, linha: number, coluna: number): void {
    if (tipo && tipo.nome !== TIPO_LOGICO.nome && tipo.nome !== TIPO_NULO.nome) {
      this.erros.push({ linha, coluna, mensagem: 'Esperado valor lógico em ' + contexto + '; obtido ' + descrever(tipo) + '.' });
    }
  }

  private resolverCaminho(caminho: string[], escopo: EscopoTipos, linha: number, coluna: number): TipoThz | undefined {
    let atual = escopo.resolver(caminho[0]);
    if (!atual) {
      this.erros.push({ linha, coluna, mensagem: "Identificador não declarado: '" + caminho[0] + "'." });
      return undefined;
    }
    for (let i = 1; i < caminho.length; i++) {
      const proximo = this.campoDe(atual, caminho[i], linha, coluna);
      if (!proximo) return undefined;
      atual = proximo;
    }
    return atual;
  }

  private donoDoMembro(membro: string): string | undefined {
    return this.ast.enumeracoes.find((e) => e.membros.includes(membro))?.nome;
  }

  private campoDe(tipo: TipoThz | undefined, campo: string, linha: number, coluna: number): TipoThz | undefined {
    if (!tipo) return undefined;

    let registro: string | undefined;
    if (tipo.categoria === CategoriaTipo.REGISTRO) registro = tipo.interno ?? tipo.nome;
    else if (tipo.categoria === CategoriaTipo.FATIA) registro = tipo.interno;

    if (!registro) {
      this.erros.push({ linha, coluna, mensagem: "Acesso a campo '" + campo + "' em não-registro (" + descrever(tipo) + ')."' });
      return undefined;
    }

    const estrutura = this.estruturas.get(registro);
    if (!estrutura) return undefined;

    const campoAst = estrutura.campos.find((c) => c.nome === campo);
    if (!campoAst) {
      this.erros.push({ linha, coluna, mensagem: "Campo '" + campo + "' inexistente na estrutura '" + registro + "'." });
      return undefined;
    }
    return this.tipoValido(campoAst.tipo, linha, coluna);
  }

  private inferir(expr: ExprAST, escopo: EscopoTipos): TipoThz | undefined {
    switch (expr.tipo) {
      case 'LITERAL_INTEIRO':
        return TIPO_LITERAL_INTEIRO;
      case 'LITERAL_DECIMAL':
        return { nome: 'DECIMAL(*,' + expr.escala + ')', categoria: CategoriaTipo.DECIMAL, escala: expr.escala };
      case 'LITERAL_TEXTO':
        return TIPO_TEXTO;
      case 'LITERAL_LOGICO':
        return TIPO_LOGICO;
      case 'NULO':
        return TIPO_NULO;
      case 'ACESSO': {
        // Membros de ENUMERACAO são identificadores globais (ex.: CANCELADA).
        if (expr.caminho.length === 1) {
          const membro = expr.caminho[0];
          if (!escopo.resolver(membro)) {
            const dono = this.donoDoMembro(membro);
            if (dono) return { nome: dono, categoria: CategoriaTipo.ENUMERACAO };
          }
        }
        return this.resolverCaminho(expr.caminho, escopo, expr.linha, expr.coluna);
      }
      case 'CHAMADA':
        return this.inferirChamada(expr, escopo);
      case 'INDEXACAO': {
        const alvo = this.inferir(expr.alvo, escopo);
        const idx = this.inferir(expr.indice, escopo);
        if (idx && !ehInteiro(idx)) this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: 'Índice deve ser inteiro; obtido ' + descrever(idx) + '.' });
        if (!alvo) return undefined;
        if (alvo.categoria === CategoriaTipo.FATIA) {
          const interno = alvo.interno!;
          if (TIPOS_PRIMITIVOS[interno]) return TIPOS_PRIMITIVOS[interno];
          if (interno === 'DATA') return TIPO_DATA;
          if (interno === 'DATA_HORA') return TIPO_DATA_HORA;
          if (this.estruturas.has(interno)) return { nome: interno, categoria: CategoriaTipo.REGISTRO, interno };
          return { nome: interno, categoria: CategoriaTipo.PRIMITIVO };
        }
        if (alvo.nome === 'TEXTO') return TIPO_TEXTO;
        this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: 'Indexação exige FATIA ou TEXTO; obtido ' + descrever(alvo) + '.' });
        return undefined;
      }
      case 'FATIA_LITERAL': {
        if (expr.elementos.length === 0) return { nome: 'FATIA[TEXTO]', categoria: CategoriaTipo.FATIA, interno: 'TEXTO' };
        const tipos = expr.elementos.map((e) => this.inferir(e, escopo));
        const primeiro = tipos[0];
        if (!primeiro) return { nome: 'FATIA[TEXTO]', categoria: CategoriaTipo.FATIA, interno: 'TEXTO' };
        for (let i = 1; i < tipos.length; i++) {
          if (tipos[i] && !saoCompativeis(tipos[i], primeiro) && !saoCompativeis(primeiro, tipos[i])) {
            this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: 'Elementos de fatia heterogêneos: ' + descrever(primeiro) + ' vs ' + descrever(tipos[i]) + '.' });
          }
        }
        const internoNome = primeiro.categoria === CategoriaTipo.REGISTRO ? (primeiro.interno ?? primeiro.nome) : primeiro.nome === '<literal-inteiro>' ? 'INTEIRO64' : primeiro.nome;
        // Se for DECIMAL(*,n) usar nome genérico FATIA[DECIMAL]
        const internoCan = internoNome.startsWith('DECIMAL') ? 'DECIMAL' : internoNome;
        return { nome: 'FATIA[' + internoCan + ']', categoria: CategoriaTipo.FATIA, interno: internoCan };
      }
      case 'CRIAR_REGISTRO': {
        const estrutura = this.estruturas.get(expr.nomeEstrutura);
        if (!estrutura) { this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "Estrutura '" + expr.nomeEstrutura + "' não declarada." }); return undefined; }
        if (expr.campos.length !== estrutura.campos.length) {
          this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "CRIAR '" + expr.nomeEstrutura + "' exige " + estrutura.campos.length + ' campos, recebidos ' + expr.campos.length + '.' });
        }
        for (const campo of estrutura.campos) {
          const fornecido = expr.campos.find((c) => c.nome === campo.nome);
          if (!fornecido) {
            this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "Campo '" + campo.nome + "' não fornecido em CRIAR '" + expr.nomeEstrutura + "'." });
            continue;
          }
          const esperado = this.tipoValido(campo.tipo, expr.linha, expr.coluna);
          const obtido = this.inferir(fornecido.valor, escopo);
          if (esperado && obtido && !saoCompativeis(obtido, esperado)) {
            this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "Campo '" + campo.nome + "' incompatível: " + descrever(obtido) + ' → ' + descrever(esperado) + '.' });
          }
        }
        return { nome: expr.nomeEstrutura, categoria: CategoriaTipo.REGISTRO, interno: expr.nomeEstrutura };
      }
      case 'OP_UNARIA': {
        const operando = this.inferir(expr.operando, escopo);
        if (expr.operador === 'NAO') {
          this.exigirLogico(operando, "operando do 'NAO'", expr.linha, expr.coluna);
          return TIPO_LOGICO;
        }
        if (!ehNumerico(operando)) {
          this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: 'Negação aritmética exige numérico.' });
        }
        return ehInteiro(operando) ? TIPO_INTEIRO_GENERICO : operando;
      }
      case 'OP_BINARIA':
        return this.inferirBinaria(expr, escopo);
    }
  }

  private inferirChamada(expr: Extract<ExprAST, { tipo: 'CHAMADA' }>, escopo: EscopoTipos): TipoThz | undefined {
    const nomeQ = expr.caminho.join('.');
    const sig = SIG_STDLIB[nomeQ];
    if (sig) {
      if (expr.argumentos.length < sig.paramMin || expr.argumentos.length > sig.paramMax) {
        this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "Função '" + nomeQ + "' exige " + sig.paramMin + (sig.paramMax !== sig.paramMin ? ' a ' + sig.paramMax : '') + ' arg(s), recebidos ' + expr.argumentos.length + '.' });
      }
      const tiposArgs = expr.argumentos.map((a) => this.inferir(a, escopo) ?? TIPO_NULO);
      const ret = typeof sig.retorno === 'function' ? (sig.retorno as (a: TipoThz[]) => TipoThz)(tiposArgs) : sig.retorno as TipoThz;
      return ret;
    }
    // procedimento?
    if (expr.caminho.length === 1) {
      const proc = (this.ast.procedimentos ?? []).find((p) => p.nome === expr.caminho[0]);
      if (proc) {
        if (expr.argumentos.length !== proc.parametros.length) {
          this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "Procedimento '" + proc.nome + "' exige " + proc.parametros.length + ' arg(s), recebidos ' + expr.argumentos.length + '.' });
        } else {
          proc.parametros.forEach((p, i) => {
            const esperado = this.tipoValido(p.tipo, expr.linha, expr.coluna);
            const obtido = this.inferir(expr.argumentos[i], escopo);
            if (esperado && obtido && !saoCompativeis(obtido, esperado)) this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "Arg " + (i + 1) + " de '" + proc.nome + "' incompatível: " + descrever(obtido) + ' → ' + descrever(esperado) + '.' });
          });
        }
        return TIPO_NULO;
      }
      // operação?
      for (const regra of this.ast.regras) {
        const op = regra.operacoes.find((o) => o.nome === expr.caminho[0]);
        if (op) {
          if (expr.argumentos.length !== op.parametros.length) {
            this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "Operação '" + op.nome + "' exige " + op.parametros.length + ' arg(s), recebidos ' + expr.argumentos.length + '.' });
          } else {
            op.parametros.forEach((p, i) => {
              const esperado = this.tipoValido(p.tipo, expr.linha, expr.coluna);
              const obtido = this.inferir(expr.argumentos[i], escopo);
              if (esperado && obtido && !saoCompativeis(obtido, esperado)) this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "Arg " + (i + 1) + " de '" + op.nome + "' incompatível: " + descrever(obtido) + ' → ' + descrever(esperado) + '.' });
            });
          }
          const ret = this.tipoValido(op.tipoRetorno, expr.linha, expr.coluna);
          return ret ?? TIPO_NULO;
        }
      }
    }
    this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "Chamada desconhecida: '" + nomeQ + "'." });
    expr.argumentos.forEach((a) => this.inferir(a, escopo));
    return undefined;
  }

  private inferirBinaria(expr: Extract<ExprAST, { tipo: 'OP_BINARIA' }>, escopo: EscopoTipos): TipoThz | undefined {
    const esq = this.inferir(expr.esquerda, escopo);
    const dir = this.inferir(expr.direita, escopo);

    if (expr.operador === 'E' || expr.operador === 'OU') {
      this.exigirLogico(esq, "conectivo '" + expr.operador + "'", expr.linha, expr.coluna);
      this.exigirLogico(dir, "conectivo '" + expr.operador + "'", expr.linha, expr.coluna);
      return TIPO_LOGICO;
    }

    if (['=', '<>', '<', '<=', '>', '>='].includes(expr.operador)) {
      this.validarComparacao(esq, dir, expr.linha, expr.coluna);
      return TIPO_LOGICO;
    }

    return this.validarAritmetica(esq, dir, expr);
  }

  private validarComparacao(esq: TipoThz | undefined, dir: TipoThz | undefined, linha: number, coluna: number): void {
    if (!esq || !dir) return;
    if (esq.nome === TIPO_NULO.nome || dir.nome === TIPO_NULO.nome) return;
    if (saoCompativeis(esq, dir) || saoCompativeis(dir, esq)) return;
    // DATA vs DATA_HORA são primitivos distintos — comparação já falha por saoCompativeis
    this.erros.push({ linha, coluna, mensagem: 'Comparação entre tipos incompatíveis: ' + descrever(esq) + ' e ' + descrever(dir) + '.' });
  }

  private validarAritmetica(esq: TipoThz | undefined, dir: TipoThz | undefined, expr: Extract<ExprAST, { tipo: 'OP_BINARIA' }>): TipoThz | undefined {
    const op = expr.operador;
    if (!esq || !dir) return undefined;

    // Concatenação textual: qualquer lado TEXTO torna a expressão TEXTO
    // (coerção universal, espelha o interpretador).
    if (op === '+') {
      const textoEsq = esq.categoria === CategoriaTipo.PRIMITIVO && esq.nome === TIPO_TEXTO.nome;
      const textoDir = dir.categoria === CategoriaTipo.PRIMITIVO && dir.nome === TIPO_TEXTO.nome;
      if (textoEsq || textoDir) return TIPO_TEXTO;
    }

    const monetarioEsq = esq.categoria === CategoriaTipo.MONETARIO;
    const monetarioDir = dir.categoria === CategoriaTipo.MONETARIO;
    if (monetarioEsq || monetarioDir) {
      if (op === '+' || op === '-') {
        if (!monetarioEsq || !monetarioDir) {
          this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "'" + op + "' exige MONETARIO nos dois lados." });
        } else if (esq.moeda !== dir.moeda) {
          this.erros.push({
            linha: expr.linha,
            coluna: expr.coluna,
            mensagem: "Impossível '" + op + "' MONETARIO('" + esq.moeda + "') com MONETARIO('" + dir.moeda + "') — moedas distintas."
          });
        }
      }
      if (monetarioEsq && monetarioDir && op === '*') {
        this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: 'Multiplicação de MONETARIO por MONETARIO é dimensionalmente inválida.' });
      }
      if ((op === '*' || op === '%') && monetarioEsq !== monetarioDir) {
        if (op === '%' || !ehNumerico(monetarioEsq ? dir : esq)) {
          this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "'" + op + "' exige fator/divisor numérico junto a MONETARIO." });
        }
      }
      return monetarioEsq ? esq : dir;
    }

    if (!ehNumerico(esq) || !ehNumerico(dir)) {
      this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "Operador '" + op + "' exige operandos numéricos; obtido " + descrever(esq) + ' e ' + descrever(dir) + '.' });
      return undefined;
    }

    if (op === '%') {
      if (!ehInteiro(esq) || !ehInteiro(dir)) {
        this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: "Resto '%' exige operandos inteiros." });
      }
      return TIPO_INTEIRO_GENERICO;
    }

    if (op === '/') {
      const zeroConstante =
        (expr.direita.tipo === 'LITERAL_DECIMAL' && expr.direita.escalado === 0n) ||
        (expr.direita.tipo === 'LITERAL_INTEIRO' && expr.direita.valor === 0n);
      if (zeroConstante) {
        this.erros.push({ linha: expr.linha, coluna: expr.coluna, mensagem: 'Divisão por constante zero.' });
      }
    }

    if (ehInteiro(esq) && ehInteiro(dir)) return TIPO_INTEIRO_GENERICO;

    const escalas = [esq, dir]
      .filter((t): t is TipoThz => t.categoria === CategoriaTipo.DECIMAL)
      .map((t) => t.escala ?? 0);
    const escala = escalas.length > 0 ? Math.max(...escalas) : 0;
    return { nome: 'DECIMAL(*,' + escala + ')', categoria: CategoriaTipo.DECIMAL, escala };
  }

  /* ---------------- Lint de governança (--estrito) ---------------- */

  private aplicarLintEstrito(): void {
    if (!this.ast.versaoLinguagem) {
      this.erros.push({ linha: 1, coluna: 1, mensagem: '[Governança] Pragma VERSAO_LINGUAGEM ausente.' });
    }
    if (!this.ast.metadados || !this.ast.metadados.sloLatencia) {
      this.erros.push({ linha: 1, coluna: 1, mensagem: '[Governança] METADADOS_ARQUITETURA sem SLO_LATENCIA_MAXIMA.' });
    }
    for (const regra of this.ast.regras) {
      if (!regra.identificador || !regra.rastreioRequisito) {
        this.erros.push({
          linha: 1,
          coluna: 1,
          mensagem: "[Governança] Regra '" + regra.nome + "' exige IDENTIFICADOR_REGRA e RASTREIO_REQUISITO."
        });
      }
    }
  }
}
