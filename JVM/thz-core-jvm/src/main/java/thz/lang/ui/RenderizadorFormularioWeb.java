package thz.lang.ui;

import thz.lang.interpretador.ErroContrato;
import thz.lang.interpretador.ErroExecucao;
import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;
import thz.lang.webview.LancadorWebviewNativo;
import thz.lang.webview.ThzJson;
import thz.lang.webview.ThzWebviewBridge;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renderizador WebView autônomo — não depende de Swing/AWT.
 * Compatível com GraalVM Native Image no Windows.
 *
 * Gera HTML via ConversorFormularioUi + ThzUiHtmlEmitter e serve via ThzWebviewBridge (VirtualThreads).
 * Abre janela nativa via LancadorWebviewNativo (Edge/Chrome --app ou fallback rundll32).
 */
public final class RenderizadorFormularioWeb implements ThzRenderer {

    @Override
    public String renderizarFormulario(ValorThz.Registro registro, String operacaoAlvo, InterpretadorThz interpretador) {
        String titulo = ConversorFormularioUi.extrairTitulo(registro);

        if (Boolean.getBoolean("thz.nao_interativo")) {
            return "Formulário '" + titulo + "' preparado com sucesso (Modo Não-Interativo).";
        }

        // Converte registro -> HTML
        String html = ConversorFormularioUi.converterParaHtml(registro, operacaoAlvo, ThzUiTema.escuroGlass());

        // Registra handler RPC para submissão real (Fase 2): JS estado -> ValorThz -> executarOperacao com contratos
        if (interpretador != null && operacaoAlvo != null && !operacaoAlvo.isBlank()) {
            String canal = operacaoAlvo.contains(".") ? operacaoAlvo : operacaoAlvo;
            String nomeOpSimples = canal.contains(".") ? canal.substring(canal.lastIndexOf('.') + 1) : canal;
            final ValorThz.Registro registroOrig = registro;
            ThzWebviewBridge.registrarCanal(canal, payload -> {
                try {
                    Map<String, String> estado = extrairEstado(payload);
                    Map<String, ValorThz> args = construirArgs(estado, registroOrig, nomeOpSimples, interpretador);
                    ValorThz resultado = interpretador.executarOperacao(nomeOpSimples, args);
                    String resStr = resultado != null ? resultado.formatar() : "OK";
                    ThzWebviewBridge.emitirParaJs("thz:operacao_sucesso", ThzJson.stringify(Map.of("operacao", canal, "resultado", resStr)));
                    return ThzJson.stringify(Map.of("status", "ok", "resultado", resStr));
                } catch (ErroContrato ec) {
                    String msg = ec.getMessage();
                    ThzWebviewBridge.emitirParaJs("thz:operacao_erro", ThzJson.stringify(Map.of("operacao", canal, "erro", msg)));
                    return ThzJson.erro(msg);
                } catch (ErroExecucao ee) {
                    String msg = ee.getMessage();
                    ThzWebviewBridge.emitirParaJs("thz:operacao_erro", ThzJson.stringify(Map.of("operacao", canal, "erro", msg)));
                    return ThzJson.erro(msg);
                } catch (Exception e) {
                    return ThzJson.erro(e.getMessage());
                }
            });
            ThzWebviewBridge.registrarCanal("__thz_restaurar__", payload -> ThzJson.okMensagem("restaurado"));
        }

        // Inicia bridge e abre janela
        String url = LancadorWebviewNativo.abrirHtml(titulo, html, 1024, 768);
        System.err.println("[THZ WebView] Formulário '" + titulo + "' disponível em: " + url);
        return "Formulário '" + titulo + "' aberto com sucesso em: " + url;
    }

    public static String renderizar(ValorThz.Registro registro, String operacaoAlvo, InterpretadorThz interpretador) {
        return new RenderizadorFormularioWeb().renderizarFormulario(registro, operacaoAlvo, interpretador);
    }

    private static Map<String, String> extrairEstado(String payload) {
        if (payload == null || payload.isBlank()) return Map.of();
        // payload esperado: {"componenteId":"...","estado":{...}} ou direto {...}
        String estadoBruto = ThzJson.extrairBruto(payload, "estado");
        if (estadoBruto != null && !estadoBruto.isBlank() && !estadoBruto.equals("null") && estadoBruto.startsWith("{")) {
            return ThzJson.parseObjetoRaso(estadoBruto);
        }
        // fallback: tenta parsear payload direto como estado
        if (payload.trim().startsWith("{") && payload.contains(":")) {
            Map<String,String> direto = ThzJson.parseObjetoRaso(payload);
            // se contém "componenteId" é envelope, já tratou; senão retorna direto
            if (!direto.containsKey("canal")) return direto;
        }
        return Map.of();
    }

    private static Map<String, ValorThz> construirArgs(Map<String, String> estado, ValorThz.Registro registroOrig, String nomeOp, InterpretadorThz interp) {
        Map<String, ValorThz> args = new LinkedHashMap<>();
        // Tenta resolver operação para mapear tipos; fallback usa tipos do registro
        var alvo = interp.listarOperacoesExecutaveis().stream()
                .filter(o -> o.operacao().nome().equalsIgnoreCase(nomeOp)).findFirst().orElse(null);

        if (alvo != null && alvo.operacao().parametros() != null) {
            for (var param : alvo.operacao().parametros()) {
                String nomeParam = param.nome();
                String tipo = param.tipo();
                String raw = estado.get(nomeParam);
                if (raw == null && registroOrig.campos().containsKey(nomeParam)) {
                    // usa valor original do registro se não veio do form
                    args.put(nomeParam, registroOrig.campos().get(nomeParam));
                    continue;
                }
                if (raw == null && alvo.operacao().parametros().size() == 1 && (tipo.equalsIgnoreCase(registroOrig.nomeEstrutura()) || tipo.equalsIgnoreCase("REGISTRO"))) {
                    // param único do tipo do registro -> reconstrói registro com estado mesclado
                    Map<String, ValorThz> camposMerge = new LinkedHashMap<>(registroOrig.campos());
                    for (var e : estado.entrySet()) {
                        ValorThz orig = camposMerge.get(e.getKey());
                        if (orig != null) camposMerge.put(e.getKey(), coerceValor(orig, e.getValue()));
                    }
                    args.put(nomeParam, new ValorThz.Registro(registroOrig.nomeEstrutura(), camposMerge));
                    continue;
                }
                if (raw != null) {
                    args.put(nomeParam, coercePorTipo(tipo, raw));
                } else if (registroOrig.campos().containsKey(nomeParam)) {
                    args.put(nomeParam, registroOrig.campos().get(nomeParam));
                }
            }
        } else {
            // sem metadados de operação: mescla estado em registro
            for (var e : registroOrig.campos().entrySet()) {
                String raw = estado.get(e.getKey());
                args.put(e.getKey(), raw != null ? coerceValor(e.getValue(), raw) : e.getValue());
            }
        }
        return args;
    }

    private static ValorThz coerceValor(ValorThz prototipo, String raw) {
        return switch (prototipo) {
            case ValorThz.Texto ignored -> ValorThz.TEXTO(raw);
            case ValorThz.Inteiro ignored -> {
                try { yield ValorThz.INTEIRO(new java.math.BigInteger(raw.trim())); } catch (Exception e) { yield ValorThz.TEXTO(raw); }
            }
            case ValorThz.Decimal ignored -> {
                try { yield ValorThz.DECIMAL(raw); } catch (Exception e) { yield ValorThz.TEXTO(raw); }
            }
            case ValorThz.Monetario m -> {
                try { yield ValorThz.MONETARIO(thz.lang.runtime.Monetario.deTexto(raw, m.valor().moeda.codigo())); } catch (Exception e) { yield prototipo; }
            }
            case ValorThz.Logico ignored -> ValorThz.LOGICO("true".equalsIgnoreCase(raw) || "verdadeiro".equalsIgnoreCase(raw) || "1".equals(raw) || "on".equalsIgnoreCase(raw));
            case ValorThz.Enumerado en -> ValorThz.ENUMERADO(en.nomeEnumeracao(), raw);
            default -> ValorThz.TEXTO(raw);
        };
    }

    private static ValorThz coercePorTipo(String tipo, String raw) {
        String t = tipo.toUpperCase();
        try {
            if (t.contains("TEXTO")) return ValorThz.TEXTO(raw);
            if (t.contains("INTEIRO") || t.contains("NATURAL")) return ValorThz.INTEIRO(new java.math.BigInteger(raw.trim()));
            if (t.contains("DECIMAL")) return ValorThz.DECIMAL(raw);
            if (t.contains("MONETARIO")) {
                var m = java.util.regex.Pattern.compile("\"?([A-Z]{3})\"?").matcher(t);
                String cod = m.find() ? m.group(1) : "BRL";
                return ValorThz.MONETARIO(thz.lang.runtime.Monetario.deTexto(raw, cod));
            }
            if (t.contains("LOGICO")) return ValorThz.LOGICO("true".equalsIgnoreCase(raw) || "verdadeiro".equalsIgnoreCase(raw) || "1".equals(raw));
            if (t.contains("DATA_HORA")) return ValorThz.DATA_HORA(thz.lang.runtime.DataHoraThz.deTexto(raw));
            if (t.contains("DATA")) return ValorThz.DATA(thz.lang.runtime.DataThz.deTexto(raw));
        } catch (Exception ignore) {}
        return ValorThz.TEXTO(raw);
    }
}
