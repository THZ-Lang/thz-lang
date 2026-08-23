package thz.lang.ir;

import thz.lang.ast.*;
import thz.lang.simd.ResultadoValidacaoSimd;
import thz.lang.simd.ValidadorSimd;
import thz.lang.sintatico.ThzParser;

import java.util.*;

/**
 * Gerador de Representação Intermediária (THZ-IR/1) e Emissor de LLVM IR Preliminar (G5).
 */
public final class GeradorIr {

    public static final String VERSAO_IR = "thz-ir/1";

    private GeradorIr() {}

    /**
     * Baixa a AST do THZ-LANG para a representação intermediária THZ-IR.
     */
    public static IrPrograma baixarParaIr(ProgramaAst ast) {
        if (ast == null) {
            throw new IllegalArgumentException("AST não pode ser nula para geração de IR.");
        }

        Map<String, String> meta = new LinkedHashMap<>();
        if (ast.metadados() != null) {
            if (ast.metadados().dominio() != null) meta.put("dominio", ast.metadados().dominio());
            if (ast.metadados().camada() != null) meta.put("camada", ast.metadados().camada());
            if (ast.metadados().sloLatencia() != null) meta.put("slo", ast.metadados().sloLatencia());
        }

        // Estruturas
        List<IrPrograma.IrEstrutura> estruturas = new ArrayList<>();
        if (ast.estruturas() != null) {
            for (EstruturaAst est : ast.estruturas()) {
                List<IrPrograma.IrCampo> campos = new ArrayList<>();
                for (CampoEstruturaAst c : est.campos()) {
                    campos.add(new IrPrograma.IrCampo(c.nome(), mapearTipoIr(c.tipo()), c.tipo()));
                }
                estruturas.add(new IrPrograma.IrEstrutura(est.nome(), est.layoutColunar(), List.copyOf(campos)));
            }
        }

        // Funções / Operações / Procedimentos
        List<IrPrograma.IrFuncao> funcoes = new ArrayList<>();

        if (ast.regras() != null) {
            for (RegraNegocioAst regra : ast.regras()) {
                if (regra.operacoes() != null) {
                    for (OperacaoAst op : regra.operacoes()) {
                        List<IrPrograma.IrParametro> params = new ArrayList<>();
                        for (ParametroOperacaoAst p : op.parametros()) {
                            params.add(new IrPrograma.IrParametro(p.nome(), mapearTipoIr(p.tipo())));
                        }
                        List<String> instrs = baixarComandosParaIr(op.corpo());
                        boolean idemp = op.idempotente() || regra.idempotente();
                        String chave = op.chaveIdempotencia() != null ? op.chaveIdempotencia() : regra.chaveIdempotencia();
                        funcoes.add(new IrPrograma.IrFuncao(
                                regra.nome() + "_" + op.nome(),
                                op.tipoRetorno() != null ? mapearTipoIr(op.tipoRetorno()) : "void",
                                List.copyOf(params),
                                List.copyOf(instrs),
                                idemp,
                                chave
                        ));
                    }
                }
            }
        }

        if (ast.procedimentos() != null) {
            for (ProcedimentoAst proc : ast.procedimentos()) {
                List<IrPrograma.IrParametro> params = new ArrayList<>();
                for (ParametroOperacaoAst p : proc.parametros()) {
                    params.add(new IrPrograma.IrParametro(p.nome(), mapearTipoIr(p.tipo())));
                }
                List<String> instrs = baixarComandosParaIr(proc.corpo());
                funcoes.add(new IrPrograma.IrFuncao(
                        proc.nome(),
                        "void",
                        List.copyOf(params),
                        List.copyOf(instrs),
                        proc.idempotente(),
                        proc.chaveIdempotencia()
                ));
            }
        }


        // Loops SIMD
        List<IrPrograma.IrSimdLoop> loopsSimd = new ArrayList<>();
        List<ResultadoValidacaoSimd> simdVal = ValidadorSimd.analisarTudo(ast);
        for (ResultadoValidacaoSimd s : simdVal) {
            loopsSimd.add(new IrPrograma.IrSimdLoop(
                    s.loopIdentificador(),
                    s.variavel(),
                    String.join(".", s.fonte()),
                    s.passoSimd(),
                    s.vetorizavel()
            ));
        }

        return new IrPrograma(
                VERSAO_IR,
                ast.nome(),
                ast.versaoLinguagem() != null ? ast.versaoLinguagem() : "2.3.0",
                Map.copyOf(meta),
                List.copyOf(estruturas),
                List.copyOf(funcoes),
                List.copyOf(loopsSimd)
        );
    }

    private static List<String> baixarComandosParaIr(List<ComandoAst> comandos) {
        List<String> out = new ArrayList<>();
        if (comandos == null) return out;
        for (ComandoAst c : comandos) {
            switch (c) {
                case ComandoAst.DeclVariavel d -> out.add("%" + d.nome() + " = alloca " + mapearTipoIr(d.tipoDado()) + " <- " + ThzParser.textoCanonicoDe(d.inicializacao()));
                case ComandoAst.Atribuicao a -> out.add("store " + ThzParser.textoCanonicoDe(a.expressao()) + " -> " + String.join(".", a.alvo()));
                case ComandoAst.Se s -> out.add("branch " + ThzParser.textoCanonicoDe(s.condicao()) + " ? then(" + (s.entao()!=null?s.entao().size():0) + ") : else(" + (s.senao()!=null?s.senao().size():0) + ")");
                case ComandoAst.Enquanto e -> out.add("loop while " + ThzParser.textoCanonicoDe(e.condicao()));
                case ComandoAst.Para p -> out.add("loop for " + p.variavel() + " in [" + ThzParser.textoCanonicoDe(p.inicio()) + ".." + ThzParser.textoCanonicoDe(p.fim()) + "]");
                case ComandoAst.VetorizarPara vp -> out.add("vector_loop " + vp.variavel() + " in " + String.join(".", vp.fonte()) + " step_simd " + vp.passoSimd());
                case ComandoAst.BlocoMemoria _ -> out.add("scoped_arena_alloc 1024");
                case ComandoAst.Exiba ex -> out.add("call @thz_exiba(" + ThzParser.textoCanonicoDe(ex.expressao()) + ")");
                case ComandoAst.Ler ler -> out.add("call @thz_ler -> " + String.join(".", ler.alvo()));
                case ComandoAst.Chamada ch -> out.add("call @" + ThzParser.textoCanonicoDe(ch.expressao()));
                case ComandoAst.Retorne r -> out.add("ret " + (r.expressao() != null ? ThzParser.textoCanonicoDe(r.expressao()) : "void"));
                case ComandoAst.FalharCom fc -> out.add("fail " + ThzParser.textoCanonicoDe(fc.expressao()));
            }
        }
        return out;
    }

    private static String mapearTipoIr(String tipoThz) {
        if (tipoThz == null) return "i64";
        String t = tipoThz.toUpperCase();
        if (t.contains("DECIMAL") || t.contains("MONETARIO")) return "i128_fixed";
        if (t.contains("INTEIRO64") || t.equals("INTEIRO")) return "i64";
        if (t.contains("INTEIRO32") || t.contains("NATURAL32")) return "i32";
        if (t.contains("TEXTO")) return "ptr_utf8";
        if (t.contains("LOGICO")) return "i1";
        if (t.contains("FATIA")) return "soa_vector";
        return "ptr";
    }

    /**
     * Serializa o THZ-IR em formato JSON canônico.
     */
    public static String serializarIr(IrPrograma ir) {
        return serializarIrJson(ir);
    }

    public static String serializarIrJson(IrPrograma ir) {

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"versaoIr\": \"").append(ir.versaoIr()).append("\",\n");
        sb.append("  \"nomePrograma\": \"").append(ir.nomePrograma()).append("\",\n");
        sb.append("  \"versaoFonte\": \"").append(ir.versaoFonte()).append("\",\n");
        sb.append("  \"metadados\": {\n");
        int count = 0;
        for (Map.Entry<String, String> e : ir.metadados().entrySet()) {
            sb.append("    \"").append(e.getKey()).append("\": \"").append(e.getValue()).append("\"").append(++count < ir.metadados().size() ? "," : "").append("\n");
        }
        sb.append("  },\n");

        // Estruturas
        sb.append("  \"estruturas\": [\n");
        for (int i = 0; i < ir.estruturas().size(); i++) {
            IrPrograma.IrEstrutura e = ir.estruturas().get(i);
            sb.append("    {\n");
            sb.append("      \"nome\": \"").append(e.nome()).append("\",\n");
            sb.append("      \"layoutColunar\": ").append(e.layoutColunar()).append(",\n");
            sb.append("      \"campos\": [\n");
            for (int j = 0; j < e.campos().size(); j++) {
                IrPrograma.IrCampo c = e.campos().get(j);
                sb.append("        {\"nome\": \"").append(c.nome()).append("\", \"tipoIr\": \"").append(c.tipoPrimitivoIr()).append("\", \"tipoOriginal\": \"").append(c.tipoOriginal()).append("\"}").append(j + 1 < e.campos().size() ? "," : "").append("\n");
            }
            sb.append("      ]\n");
            sb.append("    }").append(i + 1 < ir.estruturas().size() ? "," : "").append("\n");
        }
        sb.append("  ],\n");

        // Funções
        sb.append("  \"funcoes\": [\n");
        for (int i = 0; i < ir.funcoes().size(); i++) {
            IrPrograma.IrFuncao f = ir.funcoes().get(i);
            sb.append("    {\n");
            sb.append("      \"nome\": \"").append(f.nome()).append("\",\n");
            sb.append("      \"retorno\": \"").append(f.tipoRetornoIr()).append("\",\n");
            sb.append("      \"idempotente\": ").append(f.idempotente()).append(",\n");
            if (f.chaveIdempotencia() != null) {
                sb.append("      \"chaveIdempotencia\": \"").append(f.chaveIdempotencia()).append("\",\n");
            }
            sb.append("      \"parametros\": [");
            for (int p = 0; p < f.parametros().size(); p++) {
                IrPrograma.IrParametro param = f.parametros().get(p);
                sb.append("{\"nome\": \"").append(param.nome()).append("\", \"tipo\": \"").append(param.tipoIr()).append("\"}").append(p + 1 < f.parametros().size() ? ", " : "");
            }
            sb.append("],\n");
            sb.append("      \"instrucoes\": [\n");
            for (int k = 0; k < f.instrucoes().size(); k++) {
                sb.append("        \"").append(f.instrucoes().get(k).replace("\"", "\\\"")).append("\"").append(k + 1 < f.instrucoes().size() ? "," : "").append("\n");
            }
            sb.append("      ]\n");
            sb.append("    }").append(i + 1 < ir.funcoes().size() ? "," : "").append("\n");
        }
        sb.append("  ],\n");

        // SIMD Loops
        sb.append("  \"loopsSimd\": [\n");
        for (int i = 0; i < ir.loopsSimd().size(); i++) {
            IrPrograma.IrSimdLoop s = ir.loopsSimd().get(i);
            sb.append("    {\"contexto\": \"").append(s.contexto()).append("\", \"variavel\": \"").append(s.variavel()).append("\", \"fonte\": \"").append(s.fonte()).append("\", \"passoSimd\": ").append(s.passoSimd()).append(", \"vetorizavel\": ").append(s.vetorizavel()).append("}").append(i + 1 < ir.loopsSimd().size() ? "," : "").append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Emite representação textual LLVM IR preliminar para o backend AOT / Rust Inkwell.
     */
    public static String emitirLlvm(ProgramaAst ast) {
        StringBuilder sb = new StringBuilder();
        sb.append("; ModuleID = 'thz.").append(ast.nome()).append("'\n");
        sb.append("source_filename = \"").append(ast.nome()).append(".thz\"\n");
        sb.append("target datalayout = \"e-m:w-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128\"\n");
        sb.append("target triple = \"x86_64-pc-windows-msvc\"\n\n");

        sb.append("; Declarações de runtime THZ-LANG O(1) Arena & IO & Idempotência\n");
        sb.append("declare ptr @thz_arena_alloc(i64 %bytes)\n");
        sb.append("declare void @thz_arena_free_all(ptr %arena)\n");
        sb.append("declare void @thz_exiba_str(ptr %msg)\n");
        sb.append("declare void @thz_exiba_i128(i128 %val, i32 %scale)\n\n");

        // Estruturas
        if (ast.estruturas() != null) {
            for (EstruturaAst est : ast.estruturas()) {
                sb.append("%struct.").append(est.nome()).append(" = type { ");
                for (int i = 0; i < est.campos().size(); i++) {
                    CampoEstruturaAst c = est.campos().get(i);
                    sb.append(mapearTipoLlvm(c.tipo()));
                    if (i + 1 < est.campos().size()) sb.append(", ");
                }
                sb.append(" }\n");
            }
            sb.append("\n");
        }

        // Função Principal / Main
        sb.append("define i32 @main() {\n");
        sb.append("entry:\n");
        sb.append("  %arena = call ptr @thz_arena_alloc(i64 1048576)\n");
        sb.append("  ; Execução do programa principal: ").append(ast.nome()).append("\n");
        sb.append("  call void @thz_arena_free_all(ptr %arena)\n");
        sb.append("  ret i32 0\n");
        sb.append("}\n");

        return sb.toString();
    }

    private static String mapearTipoLlvm(String tipo) {
        if (tipo == null) return "i64";
        String t = tipo.toUpperCase();
        if (t.contains("DECIMAL") || t.contains("MONETARIO")) return "i128";
        if (t.contains("INTEIRO64") || t.equals("INTEIRO")) return "i64";
        if (t.contains("INTEIRO32") || t.contains("NATURAL32")) return "i32";
        if (t.contains("TEXTO") || t.contains("UUID")) return "ptr";
        if (t.contains("LOGICO")) return "i1";
        if (t.contains("FATIA")) return "ptr";
        return "ptr";
    }
}
