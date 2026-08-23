package thz.lang.semantico;

import thz.lang.ast.*;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Analisador semântico — port fiel de {@code src/analisador.ts}.
 * Valida programa tipado e retorna lista de {@link ErroSemantico} sem lançar.
 */
public final class AnalisadorSemantico {

    private final ProgramaAst ast;
    private final List<ErroSemantico> erros = new ArrayList<>();
    private final Map<String, EstruturaAst> estruturas = new LinkedHashMap<>();
    private final Set<String> enumeracoes = new LinkedHashSet<>();

    private static final TipoThz TIPO_LOGICO = Tipos.TIPOS_PRIMITIVOS.get("LOGICO");
    private static final TipoThz TIPO_TEXTO = Tipos.TIPOS_PRIMITIVOS.get("TEXTO");
    private static final TipoThz TIPO_NULO = new TipoThz("<nulo>", CategoriaTipo.PRIMITIVO);
    private static final TipoThz TIPO_INTEIRO_GENERICO = new TipoThz("INTEIRO64", CategoriaTipo.INTEIRO);
    private static final TipoThz TIPO_DATA = Tipos.TIPOS_PRIMITIVOS.get("DATA");
    private static final TipoThz TIPO_DATA_HORA = Tipos.TIPOS_PRIMITIVOS.get("DATA_HORA");

    // ---------------------------------------------------------------- Contexto

    private sealed interface ContextoExec permits ContextoOperacao, ContextoProcedimento {}
    private record ContextoOperacao(RegraNegocioAst regra, OperacaoAst operacao) implements ContextoExec {}
    private record ContextoProcedimento(ProcedimentoAst procedimento) implements ContextoExec {}

    private static boolean ehContextoOperacao(ContextoExec c) {
        return c instanceof ContextoOperacao;
    }

    // ---------------------------------------------------------------- Ctor


    public AnalisadorSemantico(ProgramaAst ast) {
        this.ast = ast;
    }

    public List<ErroSemantico> analisar() {
        return analisar(new OpcoesAnalise(false));
    }

    public List<ErroSemantico> analisar(OpcoesAnalise opcoes) {
        erros.clear();
        estruturas.clear();
        enumeracoes.clear();
        validarPragma();
        coletarEnumeracoes();
        coletarEstruturas();
        validarEstruturas();
        for (RegraNegocioAst regra : ast.regras()) {
            validarRegra(regra);
            if (opcoes.estrito() && regra.clausulasEntrada().isEmpty() && regra.clausulasSaida().isEmpty()) {
                erros.add(new ErroSemantico(1, 1, "[Governança] Regra '" + regra.nome() + "' sem contratos formais (EXIGE/GARANTE)."));
            }
            Set<String> vistas = new LinkedHashSet<>();
            for (OperacaoAst operacao : regra.operacoes()) {
                if (vistas.contains(operacao.nome())) {
                    erros.add(new ErroSemantico(1, 1, "Operação duplicada '" + operacao.nome() + "' na regra '" + regra.nome() + "'."));
                }
                vistas.add(operacao.nome());
            }
        }
        validarProcedimentos();
        if (opcoes.estrito()) aplicarLintEstrito();
        return deduplicar(erros);
    }

    private List<ErroSemantico> deduplicar(List<ErroSemantico> lista) {
        Map<String, ErroSemantico> unicos = new LinkedHashMap<>();
        for (ErroSemantico e : lista) {
            unicos.put(e.linha() + ":" + e.coluna() + ":" + e.mensagem(), e);
        }
        return new ArrayList<>(unicos.values());
    }

    // ---------------- Programa e estruturas ----------------

    private void validarPragma() {
        String v = ast.versaoLinguagem();
        if (v == null) return;
        if (!v.equals("2.2") && !v.equals("2.2.0") && !v.equals("2.3") && !v.equals("2.3.0")) {
            if (!Pattern.compile("^\\d+(\\.\\d+){1,2}$").matcher(v).matches()) {
                erros.add(new ErroSemantico(1, 1, "VERSAO_LINGUAGEM inválida: '" + v + "'. Use semver major.minor.patch."));
            }
        }
    }

    private void coletarEnumeracoes() {
        for (EnumeracaoAst enumeracao : ast.enumeracoes()) {
            if (enumeracoes.contains(enumeracao.nome())) {
                erros.add(new ErroSemantico(1, 1, "Enumeração duplicada: '" + enumeracao.nome() + "'."));
                continue;
            }
            boolean conflita = ast.estruturas().stream().anyMatch(e -> e.nome().equals(enumeracao.nome()));
            if (conflita) {
                erros.add(new ErroSemantico(1, 1, "Nome '" + enumeracao.nome() + "' conflita com estrutura declarada."));
                continue;
            }
            if (enumeracao.membros().isEmpty()) {
                erros.add(new ErroSemantico(1, 1, "Enumeração '" + enumeracao.nome() + "' sem membros."));
            }
            enumeracoes.add(enumeracao.nome());
        }
    }

    private void coletarEstruturas() {
        for (EstruturaAst estrutura : ast.estruturas()) {
            if (estruturas.containsKey(estrutura.nome())) {
                erros.add(new ErroSemantico(1, 1, "Estrutura duplicada: '" + estrutura.nome() + "'."));
                continue;
            }
            estruturas.put(estrutura.nome(), estrutura);
        }
    }

    private TipoThz tipoValido(String nomeVerbatim, int linha, int coluna) {
        TipoThz resolvido = Tipos.analisarNomeTipo(nomeVerbatim);
        if (resolvido != null) return resolvido;

        if (enumeracoes.contains(nomeVerbatim)) {
            return new TipoThz(nomeVerbatim, CategoriaTipo.ENUMERACAO);
        }
        if (estruturas.containsKey(nomeVerbatim)) {
            return new TipoThz(nomeVerbatim, CategoriaTipo.REGISTRO, null, null, nomeVerbatim, null);
        }
        var fatiaPat = Pattern.compile("^FATIA\\s*\\[\\s*(\\w+)\\s*\\]$").matcher(nomeVerbatim);
        if (fatiaPat.matches()) {
            String inner = fatiaPat.group(1);
            boolean conhecido = estruturas.containsKey(inner)
                    || Tipos.TIPOS_PRIMITIVOS.containsKey(inner)
                    || inner.equals("DATA")
                    || inner.equals("DATA_HORA")
                    || inner.equals("TEXTO");
            if (conhecido) {
                return new TipoThz(nomeVerbatim.replaceAll("\\s+", ""), CategoriaTipo.FATIA, null, null, inner, null);
            }
        }
        erros.add(new ErroSemantico(linha, coluna, "Tipo desconhecido: '" + nomeVerbatim + "'."));
        return null;
    }

    private void validarEstruturas() {
        for (EstruturaAst estrutura : ast.estruturas()) {
            Set<String> vistas = new LinkedHashSet<>();
            for (CampoEstruturaAst campo : estrutura.campos()) {
                if (vistas.contains(campo.nome())) {
                    erros.add(new ErroSemantico(1, 1, "Campo duplicado '" + campo.nome() + "' na estrutura '" + estrutura.nome() + "'."));
                }
                vistas.add(campo.nome());
                tipoValido(campo.tipo(), 1, 1);
            }
            validarInvariantes(estrutura);
        }
    }

    /** INVARIANTE: validadas contra o ambiente dos campos da própria estrutura. */
    private void validarInvariantes(EstruturaAst estrutura) {
        if (estrutura.invariantes().isEmpty()) return;
        EscopoTipos escopo = new EscopoTipos();
        for (CampoEstruturaAst campo : estrutura.campos()) {
            TipoThz t = tipoValido(campo.tipo(), 1, 1);
            if (t == null) t = TIPO_NULO;
            escopo.definir(campo.nome(), t, 1, 1, new ArrayList<>());
        }
        for (InvarianteAst invariante : estrutura.invariantes()) {
            TipoThz tipo = inferir(invariante.expressao(), escopo);
            exigirLogico(tipo, "invariante de '" + estrutura.nome() + "'", invariante.linha(), invariante.coluna());
        }
    }

    // ---------------- Regras ----------------

    private void validarRegra(RegraNegocioAst regra) {
        for (OperacaoAst operacao : regra.operacoes()) {
            ContextoOperacao contexto = new ContextoOperacao(regra, operacao);
            EscopoTipos escopoRaiz = new EscopoTipos();
            for (ParametroOperacaoAst parametro : operacao.parametros()) {
                TipoThz t = tipoValido(parametro.tipo(), 1, 1);
                if (t == null) t = TIPO_NULO;
                escopoRaiz.definir(parametro.nome(), t, 1, 1, erros);
            }
            TipoThz retornoTipo = tipoValido(operacao.tipoRetorno(), 1, 1);

            for (ClausulaContratoAst clausula : regra.clausulasEntrada()) validarClausula(clausula, escopoRaiz);

            EscopoTipos escopoSaida = new EscopoTipos(escopoRaiz);
            if (retornoTipo != null) {
                escopoSaida.definir("RESULTADO", retornoTipo, 1, 1, erros);
            }
            for (ClausulaContratoAst clausula : regra.clausulasSaida()) validarClausula(clausula, escopoSaida);

            validarBlocoFilho(operacao.corpo(), escopoRaiz, contexto);

        }
    }

    private void validarClausula(ClausulaContratoAst clausula, EscopoTipos escopo) {
        TipoThz tipo = inferir(clausula.expressao(), escopo);
        if (tipo != null && !tipo.nome().equals(TIPO_LOGICO.nome()) && !tipo.nome().equals(TIPO_NULO.nome())) {
            erros.add(new ErroSemantico(clausula.linha(), clausula.coluna(),
                    "Cláusula '" + clausula.tipoClausula() + "' deve ser lógica; obtido " + Tipos.descrever(tipo) + "."));
        }
    }

    private void validarProcedimentos() {
        List<ProcedimentoAst> procedimentos = ast.procedimentos() != null ? ast.procedimentos() : List.of();
        Set<String> vistas = new LinkedHashSet<>();
        for (ProcedimentoAst proc : procedimentos) {
            if (vistas.contains(proc.nome())) erros.add(new ErroSemantico(1, 1, "Procedimento duplicado: '" + proc.nome() + "'."));
            vistas.add(proc.nome());
            if (ast.regras().stream().anyMatch(r -> r.nome().equals(proc.nome()))) {
                erros.add(new ErroSemantico(1, 1, "Nome '" + proc.nome() + "' conflita com regra declarada."));
            }
            EscopoTipos escopo = new EscopoTipos();
            for (ParametroOperacaoAst p : proc.parametros()) {
                TipoThz t = tipoValido(p.tipo(), 1, 1);
                if (t == null) t = TIPO_NULO;
                escopo.definir(p.nome(), t, 1, 1, erros);
            }
            ContextoProcedimento ctx = new ContextoProcedimento(proc);
            validarBlocoFilho(proc.corpo(), escopo, ctx);
        }
    }

    // ---------------- Comandos ----------------

    private void validarBlocoFilho(List<ComandoAst> comandos, EscopoTipos escopoPai, ContextoExec contexto) {
        EscopoTipos escopo = new EscopoTipos(escopoPai);
        for (ComandoAst comando : comandos) {
            validarComando(comando, escopo, contexto);
        }
    }

    private void validarComando(ComandoAst cmd, EscopoTipos escopo, ContextoExec contexto) {
        switch (cmd) {
            case ComandoAst.DeclVariavel d -> {
                TipoThz declarado = tipoValido(d.tipoDado(), d.linha(), d.coluna());
                TipoThz init = inferir(d.inicializacao(), escopo);
                if (declarado != null && init != null && !Tipos.saoCompativeis(init, declarado)) {
                    erros.add(new ErroSemantico(d.linha(), d.coluna(),
                            "Inicialização de '" + d.nome() + "' incompatível: " + Tipos.descrever(init) + " → " + Tipos.descrever(declarado) + "."));
                }
                escopo.definir(d.nome(), declarado != null ? declarado : TIPO_NULO, d.linha(), d.coluna(), erros);
            }
            case ComandoAst.Atribuicao a -> {
                TipoThz alvo = resolverCaminho(a.alvo(), escopo, a.linha(), a.coluna());
                TipoThz valor = inferir(a.expressao(), escopo);
                if (alvo != null && valor != null && !Tipos.saoCompativeis(valor, alvo)) {
                    erros.add(new ErroSemantico(a.linha(), a.coluna(),
                            "Atribuição incompatível: " + Tipos.descrever(valor) + " → " + Tipos.descrever(alvo) + " em '" + String.join(".", a.alvo()) + "'."));
                }
            }
            case ComandoAst.Se s -> {
                exigirLogico(inferir(s.condicao(), escopo), "condição do 'SE'", s.linha(), s.coluna());
                validarBlocoFilho(s.entao(), escopo, contexto);
                validarBlocoFilho(s.senao(), escopo, contexto);
            }
            case ComandoAst.Enquanto e -> {
                exigirLogico(inferir(e.condicao(), escopo), "condição do 'ENQUANTO'", e.linha(), e.coluna());
                validarBlocoFilho(e.corpo(), escopo, contexto);
            }
            case ComandoAst.Para p -> {
                TipoThz iniTipo = inferir(p.inicio(), escopo);
                TipoThz fimTipo = inferir(p.fim(), escopo);
                if (iniTipo != null && !Tipos.ehInteiro(iniTipo))
                    erros.add(new ErroSemantico(p.linha(), p.coluna(), "'PARA' exige início inteiro; obtido " + Tipos.descrever(iniTipo) + "."));
                if (fimTipo != null && !Tipos.ehInteiro(fimTipo))
                    erros.add(new ErroSemantico(p.linha(), p.coluna(), "'PARA' exige fim inteiro; obtido " + Tipos.descrever(fimTipo) + "."));
                if (p.passo() != null) {
                    TipoThz pp = inferir(p.passo(), escopo);
                    if (pp != null && !Tipos.ehInteiro(pp))
                        erros.add(new ErroSemantico(p.linha(), p.coluna(), "'PASSO' exige inteiro; obtido " + Tipos.descrever(pp) + "."));
                }
                EscopoTipos escopoIter = new EscopoTipos(escopo);
                escopoIter.definir(p.variavel(), TIPO_INTEIRO_GENERICO, p.linha(), p.coluna(), erros);
                validarBlocoFilho(p.corpo(), escopoIter, contexto);
            }
            case ComandoAst.VetorizarPara vp -> {
                TipoThz fonte = escopo.resolver(vp.fonte().get(0));
                if (fonte == null) {
                    erros.add(new ErroSemantico(vp.linha(), vp.coluna(), "Fonte não declarada: '" + vp.fonte().get(0) + "'."));
                    return;
                }
                if (fonte.categoria() != CategoriaTipo.FATIA || fonte.interno() == null) {
                    erros.add(new ErroSemantico(vp.linha(), vp.coluna(), "Fonte do 'VETORIZAR_PARA' deve ser FATIA[T]; obtido " + Tipos.descrever(fonte) + "."));
                    return;
                }
                EscopoTipos escopoIteracao = new EscopoTipos(escopo);
                TipoThz interno;
                if (Tipos.TIPOS_PRIMITIVOS.containsKey(fonte.interno())) {
                    interno = new TipoThz(fonte.interno(), CategoriaTipo.PRIMITIVO);
                } else {
                    interno = new TipoThz(fonte.interno(), CategoriaTipo.REGISTRO, null, null, fonte.interno(), null);
                }
                escopoIteracao.definir(vp.variavel(), interno, vp.linha(), vp.coluna(), erros);
                validarBlocoFilho(vp.corpo(), escopoIteracao, contexto);
            }
            case ComandoAst.BlocoMemoria bm -> validarBlocoFilho(bm.corpo(), escopo, contexto);
            case ComandoAst.Exiba ex -> inferir(ex.expressao(), escopo);
            case ComandoAst.Ler ler -> {
                TipoThz alvo = resolverCaminho(ler.alvo(), escopo, ler.linha(), ler.coluna());
                if (alvo != null) {
                    boolean legivel = List.of(CategoriaTipo.PRIMITIVO, CategoriaTipo.INTEIRO, CategoriaTipo.DECIMAL).contains(alvo.categoria())
                            || "DATA".equals(alvo.nome()) || "DATA_HORA".equals(alvo.nome());
                    if (!legivel && alvo.categoria() != CategoriaTipo.PRIMITIVO) {
                        if (!alvo.nome().equals("DATA") && !alvo.nome().equals("DATA_HORA") && !alvo.nome().equals("TEXTO") && !alvo.nome().equals("LOGICO")) {
                            erros.add(new ErroSemantico(ler.linha(), ler.coluna(), "LER exige alvo TEXTO/numérico/DATA; obtido " + Tipos.descrever(alvo) + "."));
                        }
                    }
                }
            }
            case ComandoAst.Chamada ch -> inferir(ch.expressao(), escopo);
            case ComandoAst.Retorne ret -> {
                if (!ehContextoOperacao(contexto)) {
                    if (ret.expressao() != null) {
                        erros.add(new ErroSemantico(ret.linha(), ret.coluna(), "RETORNE com valor não permitido dentro de PROCEDIMENTO."));
                    }
                    return;
                }
                if (ret.expressao() == null) return;
                ContextoOperacao ctxOp = (ContextoOperacao) contexto;
                TipoThz retornoDeclarado = tipoValido(ctxOp.operacao().tipoRetorno(), 1, 1);
                TipoThz retornado = inferir(ret.expressao(), escopo);
                TipoThz canalSucesso = null;
                if (retornoDeclarado != null && retornoDeclarado.categoria() == CategoriaTipo.RESULTADO) {
                    canalSucesso = tipoValido(retornoDeclarado.interno() != null ? retornoDeclarado.interno() : "NULO", 1, 1);
                } else {
                    canalSucesso = retornoDeclarado;
                }
                if (canalSucesso != null && retornado != null && !Tipos.saoCompativeis(retornado, canalSucesso)) {
                    erros.add(new ErroSemantico(ret.linha(), ret.coluna(),
                            "RETORNE incompatível: " + Tipos.descrever(retornado) + " → " + Tipos.descrever(canalSucesso) + "."));
                }
            }
            case ComandoAst.FalharCom fc -> {
                if (!ehContextoOperacao(contexto)) {
                    erros.add(new ErroSemantico(fc.linha(), fc.coluna(), "FALHAR_COM não permitido dentro de PROCEDIMENTO (exige RESULTADO)."));
                    return;
                }
                ContextoOperacao ctxOp = (ContextoOperacao) contexto;
                TipoThz retornoDeclarado = tipoValido(ctxOp.operacao().tipoRetorno(), fc.linha(), fc.coluna());
                if (retornoDeclarado == null || retornoDeclarado.categoria() != CategoriaTipo.RESULTADO) {
                    erros.add(new ErroSemantico(fc.linha(), fc.coluna(),
                            "FALHAR_COM exige operação com retorno 'RESULTADO[T,E]'; declarado " + Tipos.descrever(retornoDeclarado) + "."));
                    return;
                }
                TipoThz canalErro = tipoValido(retornoDeclarado.internoErro() != null ? retornoDeclarado.internoErro() : "TEXTO", 1, 1);
                TipoThz valorErro = inferir(fc.expressao(), escopo);
                if (canalErro != null && valorErro != null && !Tipos.saoCompativeis(valorErro, canalErro)) {
                    erros.add(new ErroSemantico(fc.linha(), fc.coluna(),
                            "FALHAR_COM incompatível: " + Tipos.descrever(valorErro) + " → " + Tipos.descrever(canalErro) + "."));
                }
            }
        }
    }

    // ---------------- Expressões ----------------

    private void exigirLogico(TipoThz tipo, String contexto, int linha, int coluna) {
        if (tipo != null && !tipo.nome().equals(TIPO_LOGICO.nome()) && !tipo.nome().equals(TIPO_NULO.nome())) {
            erros.add(new ErroSemantico(linha, coluna, "Esperado valor lógico em " + contexto + "; obtido " + Tipos.descrever(tipo) + "."));
        }
    }

    private TipoThz resolverCaminho(List<String> caminho, EscopoTipos escopo, int linha, int coluna) {
        TipoThz atual = escopo.resolver(caminho.get(0));
        if (atual == null) {
            erros.add(new ErroSemantico(linha, coluna, "Identificador não declarado: '" + caminho.get(0) + "'."));
            return null;
        }
        for (int i = 1; i < caminho.size(); i++) {
            TipoThz proximo = campoDe(atual, caminho.get(i), linha, coluna);
            if (proximo == null) return null;
            atual = proximo;
        }
        return atual;
    }

    private String donoDoMembro(String membro) {
        for (EnumeracaoAst e : ast.enumeracoes()) {
            if (e.membros().contains(membro)) return e.nome();
        }
        return null;
    }

    private TipoThz campoDe(TipoThz tipo, String campo, int linha, int coluna) {
        if (tipo == null) return null;
        String registro = null;
        if (tipo.categoria() == CategoriaTipo.REGISTRO) registro = tipo.interno() != null ? tipo.interno() : tipo.nome();
        else if (tipo.categoria() == CategoriaTipo.FATIA) registro = tipo.interno();

        if (registro == null) {
            erros.add(new ErroSemantico(linha, coluna, "Acesso a campo '" + campo + "' em não-registro (" + Tipos.descrever(tipo) + ").\""));
            return null;
        }
        EstruturaAst estrutura = estruturas.get(registro);
        if (estrutura == null) return null;
        Optional<CampoEstruturaAst> campoAst = estrutura.campos().stream().filter(c -> c.nome().equals(campo)).findFirst();
        if (campoAst.isEmpty()) {
            erros.add(new ErroSemantico(linha, coluna, "Campo '" + campo + "' inexistente na estrutura '" + registro + "'."));
            return null;
        }
        return tipoValido(campoAst.get().tipo(), linha, coluna);
    }

    private TipoThz inferir(ExprAst expr, EscopoTipos escopo) {
        return switch (expr) {
            case ExprAst.LiteralInteiro _ -> Tipos.TIPO_LITERAL_INTEIRO;
            case ExprAst.LiteralDecimal ld -> new TipoThz("DECIMAL(*," + ld.escala() + ")", CategoriaTipo.DECIMAL, ld.escala(), null, null, null);
            case ExprAst.LiteralTexto _ -> TIPO_TEXTO;
            case ExprAst.LiteralLogico _ -> TIPO_LOGICO;
            case ExprAst.Nulo _ -> TIPO_NULO;
            case ExprAst.AcessoCampo ac -> {
                if (ac.caminho().size() == 1) {
                    String membro = ac.caminho().get(0);
                    if (escopo.resolver(membro) == null) {
                        String dono = donoDoMembro(membro);
                        if (dono != null) yield new TipoThz(dono, CategoriaTipo.ENUMERACAO);
                    }
                }
                yield resolverCaminho(ac.caminho(), escopo, ac.linha(), ac.coluna());
            }
            case ExprAst.Chamada ch -> inferirChamada(ch, escopo);
            case ExprAst.Indexacao idx -> inferirIndexacao(idx, escopo);
            case ExprAst.FatiaLiteral fl -> inferirFatiaLiteral(fl, escopo);
            case ExprAst.CriarRegistro cr -> inferirCriarRegistro(cr, escopo);
            case ExprAst.OpUnaria ou -> inferirOpUnaria(ou, escopo);
            case ExprAst.OpBinaria ob -> inferirBinaria(ob, escopo);
        };
    }

    private TipoThz inferirIndexacao(ExprAst.Indexacao idx, EscopoTipos escopo) {
        TipoThz alvo = inferir(idx.alvo(), escopo);
        TipoThz indice = inferir(idx.indice(), escopo);
        if (indice != null && !Tipos.ehInteiro(indice))
            erros.add(new ErroSemantico(idx.linha(), idx.coluna(), "Índice deve ser inteiro; obtido " + Tipos.descrever(indice) + "."));
        if (alvo == null) return null;
        if (alvo.categoria() == CategoriaTipo.FATIA) {
            String interno = alvo.interno();
            if (interno == null) return null;
            if (Tipos.TIPOS_PRIMITIVOS.containsKey(interno)) return Tipos.TIPOS_PRIMITIVOS.get(interno);
            if (interno.equals("DATA")) return TIPO_DATA;
            if (interno.equals("DATA_HORA")) return TIPO_DATA_HORA;
            if (estruturas.containsKey(interno)) return new TipoThz(interno, CategoriaTipo.REGISTRO, null, null, interno, null);
            return new TipoThz(interno, CategoriaTipo.PRIMITIVO);
        }
        if (alvo.nome().equals("TEXTO")) return TIPO_TEXTO;
        erros.add(new ErroSemantico(idx.linha(), idx.coluna(), "Indexação exige FATIA ou TEXTO; obtido " + Tipos.descrever(alvo) + "."));
        return null;
    }

    private TipoThz inferirFatiaLiteral(ExprAst.FatiaLiteral fl, EscopoTipos escopo) {
        if (fl.elementos().isEmpty()) return new TipoThz("FATIA[TEXTO]", CategoriaTipo.FATIA, null, null, "TEXTO", null);
        List<TipoThz> tipos = new ArrayList<>();
        for (ExprAst e : fl.elementos()) tipos.add(inferir(e, escopo));
        TipoThz primeiro = tipos.get(0);
        if (primeiro == null) return new TipoThz("FATIA[TEXTO]", CategoriaTipo.FATIA, null, null, "TEXTO", null);
        for (int i = 1; i < tipos.size(); i++) {
            TipoThz ti = tipos.get(i);
            if (ti != null && !Tipos.saoCompativeis(ti, primeiro) && !Tipos.saoCompativeis(primeiro, ti)) {
                erros.add(new ErroSemantico(fl.linha(), fl.coluna(), "Elementos de fatia heterogêneos: " + Tipos.descrever(primeiro) + " vs " + Tipos.descrever(ti) + "."));
            }
        }
        String internoNome;
        if (primeiro.categoria() == CategoriaTipo.REGISTRO) {
            internoNome = primeiro.interno() != null ? primeiro.interno() : primeiro.nome();
        } else if (primeiro.nome().equals("<literal-inteiro>")) {
            internoNome = "INTEIRO64";
        } else {
            internoNome = primeiro.nome();
        }
        String internoCan = internoNome.startsWith("DECIMAL") ? "DECIMAL" : internoNome;
        return new TipoThz("FATIA[" + internoCan + "]", CategoriaTipo.FATIA, null, null, internoCan, null);
    }

    private TipoThz inferirCriarRegistro(ExprAst.CriarRegistro cr, EscopoTipos escopo) {
        EstruturaAst estrutura = estruturas.get(cr.nomeEstrutura());
        if (estrutura == null) {
            erros.add(new ErroSemantico(cr.linha(), cr.coluna(), "Estrutura '" + cr.nomeEstrutura() + "' não declarada."));
            return null;
        }
        if (cr.campos().size() != estrutura.campos().size()) {
            erros.add(new ErroSemantico(cr.linha(), cr.coluna(),
                    "CRIAR '" + cr.nomeEstrutura() + "' exige " + estrutura.campos().size() + " campos, recebidos " + cr.campos().size() + "."));
        }
        for (CampoEstruturaAst campo : estrutura.campos()) {
            Optional<ExprAst.CampoValor> fornecido = cr.campos().stream().filter(c -> c.nome().equals(campo.nome())).findFirst();
            if (fornecido.isEmpty()) {
                erros.add(new ErroSemantico(cr.linha(), cr.coluna(), "Campo '" + campo.nome() + "' não fornecido em CRIAR '" + cr.nomeEstrutura() + "'."));
                continue;
            }
            TipoThz esperado = tipoValido(campo.tipo(), cr.linha(), cr.coluna());
            TipoThz obtido = inferir(fornecido.get().valor(), escopo);
            if (esperado != null && obtido != null && !Tipos.saoCompativeis(obtido, esperado)) {
                erros.add(new ErroSemantico(cr.linha(), cr.coluna(), "Campo '" + campo.nome() + "' incompatível: " + Tipos.descrever(obtido) + " → " + Tipos.descrever(esperado) + "."));
            }
        }
        return new TipoThz(cr.nomeEstrutura(), CategoriaTipo.REGISTRO, null, null, cr.nomeEstrutura(), null);
    }

    private TipoThz inferirOpUnaria(ExprAst.OpUnaria ou, EscopoTipos escopo) {
        TipoThz operando = inferir(ou.operando(), escopo);
        if (ou.operador().equals("NAO")) {
            exigirLogico(operando, "operando do 'NAO'", ou.linha(), ou.coluna());
            return TIPO_LOGICO;
        }
        if (!Tipos.ehNumerico(operando)) {
            erros.add(new ErroSemantico(ou.linha(), ou.coluna(), "Negação aritmética exige numérico."));
        }
        return Tipos.ehInteiro(operando) ? TIPO_INTEIRO_GENERICO : operando;
    }


    private TipoThz inferirChamada(ExprAst.Chamada expr, EscopoTipos escopo) {
        String nomeQ = String.join(".", expr.caminho());
        AssinaturasStdlib.Assinatura sig = AssinaturasStdlib.obter(nomeQ);
        if (sig != null) {
            if (expr.argumentos().size() < sig.paramMin() || expr.argumentos().size() > sig.paramMax()) {
                erros.add(new ErroSemantico(expr.linha(), expr.coluna(),
                        "Função '" + nomeQ + "' exige " + sig.paramMin() + (sig.paramMax() != sig.paramMin() ? " a " + sig.paramMax() : "") + " arg(s), recebidos " + expr.argumentos().size() + "."));
            }
            List<TipoThz> tiposArgs = new ArrayList<>();
            for (ExprAst a : expr.argumentos()) {
                TipoThz ta = inferir(a, escopo);
                tiposArgs.add(ta != null ? ta : TIPO_NULO);
            }
            return sig.retornoFn().apply(tiposArgs);
        }

        if (expr.caminho().size() == 2) {
            String nomeRegra = expr.caminho().get(0);
            String nomeOp = expr.caminho().get(1);
            for (RegraNegocioAst regra : ast.regras()) {
                if (regra.nome().equals(nomeRegra)) {
                    Optional<OperacaoAst> opOpt = regra.operacoes().stream().filter(o -> o.nome().equals(nomeOp)).findFirst();
                    if (opOpt.isPresent()) {
                        OperacaoAst op = opOpt.get();
                        if (expr.argumentos().size() != op.parametros().size()) {
                            erros.add(new ErroSemantico(expr.linha(), expr.coluna(),
                                    "Operação '" + op.nome() + "' exige " + op.parametros().size() + " arg(s), recebidos " + expr.argumentos().size() + "."));
                        } else {
                            for (int i = 0; i < op.parametros().size(); i++) {
                                ParametroOperacaoAst p = op.parametros().get(i);
                                TipoThz esperado = tipoValido(p.tipo(), expr.linha(), expr.coluna());
                                TipoThz obtido = inferir(expr.argumentos().get(i), escopo);
                                if (esperado != null && obtido != null && !Tipos.saoCompativeis(obtido, esperado))
                                    erros.add(new ErroSemantico(expr.linha(), expr.coluna(), "Arg " + (i + 1) + " de '" + op.nome() + "' incompatível: " + Tipos.descrever(obtido) + " → " + Tipos.descrever(esperado) + "."));
                            }
                        }
                        TipoThz ret = tipoValido(op.tipoRetorno(), expr.linha(), expr.coluna());
                        return ret != null ? ret : TIPO_NULO;
                    }
                }
            }
        }

        if (expr.caminho().size() == 1) {
            String simples = expr.caminho().get(0);
            List<ProcedimentoAst> procs = ast.procedimentos() != null ? ast.procedimentos() : List.of();
            Optional<ProcedimentoAst> procOpt = procs.stream().filter(p -> p.nome().equals(simples)).findFirst();
            if (procOpt.isPresent()) {
                ProcedimentoAst proc = procOpt.get();
                if (expr.argumentos().size() != proc.parametros().size()) {
                    erros.add(new ErroSemantico(expr.linha(), expr.coluna(),
                            "Procedimento '" + proc.nome() + "' exige " + proc.parametros().size() + " arg(s), recebidos " + expr.argumentos().size() + "."));
                } else {
                    for (int i = 0; i < proc.parametros().size(); i++) {
                        ParametroOperacaoAst p = proc.parametros().get(i);
                        TipoThz esperado = tipoValido(p.tipo(), expr.linha(), expr.coluna());
                        TipoThz obtido = inferir(expr.argumentos().get(i), escopo);
                        if (esperado != null && obtido != null && !Tipos.saoCompativeis(obtido, esperado))
                            erros.add(new ErroSemantico(expr.linha(), expr.coluna(), "Arg " + (i + 1) + " de '" + proc.nome() + "' incompatível: " + Tipos.descrever(obtido) + " → " + Tipos.descrever(esperado) + "."));
                    }
                }
                return TIPO_NULO;
            }
            for (RegraNegocioAst regra : ast.regras()) {
                Optional<OperacaoAst> opOpt = regra.operacoes().stream().filter(o -> o.nome().equals(simples)).findFirst();
                if (opOpt.isPresent()) {
                    OperacaoAst op = opOpt.get();
                    if (expr.argumentos().size() != op.parametros().size()) {
                        erros.add(new ErroSemantico(expr.linha(), expr.coluna(),
                                "Operação '" + op.nome() + "' exige " + op.parametros().size() + " arg(s), recebidos " + expr.argumentos().size() + "."));
                    } else {
                        for (int i = 0; i < op.parametros().size(); i++) {
                            ParametroOperacaoAst p = op.parametros().get(i);
                            TipoThz esperado = tipoValido(p.tipo(), expr.linha(), expr.coluna());
                            TipoThz obtido = inferir(expr.argumentos().get(i), escopo);
                            if (esperado != null && obtido != null && !Tipos.saoCompativeis(obtido, esperado))
                                erros.add(new ErroSemantico(expr.linha(), expr.coluna(), "Arg " + (i + 1) + " de '" + op.nome() + "' incompatível: " + Tipos.descrever(obtido) + " → " + Tipos.descrever(esperado) + "."));
                        }
                    }
                    TipoThz ret = tipoValido(op.tipoRetorno(), expr.linha(), expr.coluna());
                    return ret != null ? ret : TIPO_NULO;
                }
            }
        }
        erros.add(new ErroSemantico(expr.linha(), expr.coluna(), "Chamada desconhecida: '" + nomeQ + "'."));
        for (ExprAst a : expr.argumentos()) inferir(a, escopo);
        return null;
    }


    private TipoThz inferirBinaria(ExprAst.OpBinaria expr, EscopoTipos escopo) {
        TipoThz esq = inferir(expr.esquerda(), escopo);
        TipoThz dir = inferir(expr.direita(), escopo);

        if (expr.operador().equals("E") || expr.operador().equals("OU")) {
            exigirLogico(esq, "conectivo '" + expr.operador() + "'", expr.linha(), expr.coluna());
            exigirLogico(dir, "conectivo '" + expr.operador() + "'", expr.linha(), expr.coluna());
            return TIPO_LOGICO;
        }
        if (List.of("=", "<>", "<", "<=", ">", ">=").contains(expr.operador())) {
            validarComparacao(esq, dir, expr.linha(), expr.coluna());
            return TIPO_LOGICO;
        }
        return validarAritmetica(esq, dir, expr);
    }

    private void validarComparacao(TipoThz esq, TipoThz dir, int linha, int coluna) {
        if (esq == null || dir == null) return;
        if (esq.nome().equals(TIPO_NULO.nome()) || dir.nome().equals(TIPO_NULO.nome())) return;
        if (Tipos.saoCompativeis(esq, dir) || Tipos.saoCompativeis(dir, esq)) return;
        erros.add(new ErroSemantico(linha, coluna, "Comparação entre tipos incompatíveis: " + Tipos.descrever(esq) + " e " + Tipos.descrever(dir) + "."));
    }

    private TipoThz validarAritmetica(TipoThz esq, TipoThz dir, ExprAst.OpBinaria expr) {
        String op = expr.operador();
        if (esq == null || dir == null) return null;

        if (op.equals("+")) {
            boolean textoEsq = esq.categoria() == CategoriaTipo.PRIMITIVO && esq.nome().equals(TIPO_TEXTO.nome());
            boolean textoDir = dir.categoria() == CategoriaTipo.PRIMITIVO && dir.nome().equals(TIPO_TEXTO.nome());
            if (textoEsq || textoDir) return TIPO_TEXTO;
        }

        boolean monetarioEsq = esq.categoria() == CategoriaTipo.MONETARIO;
        boolean monetarioDir = dir.categoria() == CategoriaTipo.MONETARIO;
        if (monetarioEsq || monetarioDir) {
            if (op.equals("+") || op.equals("-")) {
                if (!monetarioEsq || !monetarioDir) {
                    erros.add(new ErroSemantico(expr.linha(), expr.coluna(), "'" + op + "' exige MONETARIO nos dois lados."));
                } else if (!Objects.equals(esq.moeda(), dir.moeda())) {
                    erros.add(new ErroSemantico(expr.linha(), expr.coluna(),
                            "Impossível '" + op + "' MONETARIO('" + esq.moeda() + "') com MONETARIO('" + dir.moeda() + "') — moedas distintas."));
                }
            }
            if (monetarioEsq && monetarioDir && op.equals("*")) {
                erros.add(new ErroSemantico(expr.linha(), expr.coluna(), "Multiplicação de MONETARIO por MONETARIO é dimensionalmente inválida."));
            }
            if ((op.equals("*") || op.equals("%")) && monetarioEsq != monetarioDir) {
                TipoThz outro = monetarioEsq ? dir : esq;
                if (op.equals("%") || !Tipos.ehNumerico(outro)) {
                    erros.add(new ErroSemantico(expr.linha(), expr.coluna(), "'" + op + "' exige fator/divisor numérico junto a MONETARIO."));
                }
            }
            return monetarioEsq ? esq : dir;
        }

        if (!Tipos.ehNumerico(esq) || !Tipos.ehNumerico(dir)) {
            erros.add(new ErroSemantico(expr.linha(), expr.coluna(), "Operador '" + op + "' exige operandos numéricos; obtido " + Tipos.descrever(esq) + " e " + Tipos.descrever(dir) + "."));
            return null;
        }

        if (op.equals("%")) {
            if (!Tipos.ehInteiro(esq) || !Tipos.ehInteiro(dir)) {
                erros.add(new ErroSemantico(expr.linha(), expr.coluna(), "Resto '%' exige operandos inteiros."));
            }
            return TIPO_INTEIRO_GENERICO;
        }

        if (op.equals("/")) {
            boolean zeroConstante = false;
            if (expr.direita() instanceof ExprAst.LiteralDecimal ld && ld.escalado().equals(BigInteger.ZERO)) zeroConstante = true;
            if (expr.direita() instanceof ExprAst.LiteralInteiro li && li.valor().equals(BigInteger.ZERO)) zeroConstante = true;
            if (zeroConstante) {
                erros.add(new ErroSemantico(expr.linha(), expr.coluna(), "Divisão por constante zero."));
            }
        }

        if (Tipos.ehInteiro(esq) && Tipos.ehInteiro(dir)) return TIPO_INTEIRO_GENERICO;

        List<TipoThz> decs = new ArrayList<>();
        if (esq.categoria() == CategoriaTipo.DECIMAL) decs.add(esq);
        if (dir.categoria() == CategoriaTipo.DECIMAL) decs.add(dir);
        int escala = 0;
        if (!decs.isEmpty()) {
            escala = decs.stream().mapToInt(t -> t.escala() != null ? t.escala() : 0).max().orElse(0);
        }
        return new TipoThz("DECIMAL(*," + escala + ")", CategoriaTipo.DECIMAL, escala, null, null, null);
    }

    // ---------------- Lint de governança (--estrito) ----------------

    private void aplicarLintEstrito() {
        if (ast.versaoLinguagem() == null) {
            erros.add(new ErroSemantico(1, 1, "[Governança] Pragma VERSAO_LINGUAGEM ausente."));
        }
        if (ast.metadados() == null || ast.metadados().sloLatencia() == null) {
            erros.add(new ErroSemantico(1, 1, "[Governança] METADADOS_ARQUITETURA sem SLO_LATENCIA_MAXIMA."));
        }
        for (RegraNegocioAst regra : ast.regras()) {
            if (regra.identificador() == null || regra.rastreioRequisito() == null) {
                erros.add(new ErroSemantico(1, 1, "[Governança] Regra '" + regra.nome() + "' exige IDENTIFICADOR_REGRA e RASTREIO_REQUISITO."));
            }
        }
    }
}
