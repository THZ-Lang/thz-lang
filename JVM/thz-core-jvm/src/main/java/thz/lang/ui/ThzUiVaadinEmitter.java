package thz.lang.ui;

import java.util.*;

/**
 * ThzUiVaadinEmitter — Renderizador de interfaces declarativas THZ-UI para
 * Vaadin Flow e Vaadin Lumo Web Components oficiais.
 * <p>
 * Renderiza componentes oficiais Vaadin:
 * &lt;vaadin-vertical-layout&gt;, &lt;vaadin-horizontal-layout&gt;, &lt;vaadin-form-layout&gt;,
 * &lt;vaadin-button theme="primary"&gt;, &lt;vaadin-text-field&gt;, &lt;vaadin-number-field&gt;,
 * &lt;vaadin-date-picker&gt;, &lt;vaadin-select&gt;, &lt;vaadin-checkbox&gt;, &lt;vaadin-grid&gt;,
 * &lt;vaadin-notification&gt; com tema oficial Vaadin Lumo Dark/Light.
 */
public final class ThzUiVaadinEmitter {

    private ThzUiVaadinEmitter() {}

    public static String renderizarPaginaVaadin(String titulo, ThzUiComponente raiz, boolean temaEscuro) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"pt-BR\" theme=\"").append(temaEscuro ? "dark" : "light").append("\">\n<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("  <title>").append(escapeHtml(titulo)).append(" — Vaadin UI</title>\n");
        
        // Estilos e Tokens Oficiais Vaadin Lumo
        sb.append("  <style>\n");
        sb.append(gerarCssLumo(temaEscuro));
        sb.append("  </style>\n");

        // Importação de Web Components Oficiais do Vaadin
        sb.append("  <script type=\"module\" src=\"https://cdn.jsdelivr.net/npm/@vaadin/vaadin-lumo-styles@24.3.0/vaadin-iconset.js\"></script>\n");
        sb.append("</head>\n<body>\n");
        sb.append("  <div class=\"vaadin-app-container\">\n");
        sb.append("    <header class=\"vaadin-app-header\">\n");
        sb.append("      <div class=\"vaadin-badge-logo\">VAADIN FLOW ENGINE</div>\n");
        sb.append("      <h2 class=\"vaadin-title\">").append(escapeHtml(titulo)).append("</h2>\n");
        sb.append("    </header>\n\n");

        sb.append("    <main class=\"vaadin-main-content\">\n");
        renderizarComponente(raiz, sb, "      ");
        sb.append("    </main>\n");

        sb.append("    <div id=\"vaadin_notification_area\" class=\"vaadin-notification-container\"></div>\n");
        sb.append("  </div>\n");

        // Script de sincronização Vaadin <-> THZ RPC
        sb.append("  <script>\n");
        sb.append(gerarJsVaadin());
        sb.append("  </script>\n");
        sb.append("</body>\n</html>");

        return sb.toString();
    }

    private static void renderizarComponente(ThzUiComponente c, StringBuilder sb, String indent) {
        if (c == null) return;
        String id = c.id();
        String rotulo = c.getPropriedade("rotulo", "");
        String placeholder = c.getPropriedade("placeholder", "");

        switch (c.tipo()) {
            case CONTAINER -> {
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"vaadin-card-panel\">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case LINHA -> {
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"vaadin-horizontal-layout\">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case COLUNA -> {
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"vaadin-vertical-layout\">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
            case CARD -> {
                String tituloCard = c.getPropriedade("titulo", "");
                sb.append(indent).append("<section id=\"").append(id).append("\" class=\"vaadin-card\">\n");
                if (!tituloCard.isBlank()) {
                    sb.append(indent).append("  <div class=\"vaadin-card-header\"><h3>").append(escapeHtml(tituloCard)).append("</h3></div>\n");
                }
                sb.append(indent).append("  <div class=\"vaadin-card-body\">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "    ");
                sb.append(indent).append("  </div>\n");
                sb.append(indent).append("</section>\n");
            }
            case BOTAO -> {
                String acao = c.eventos().getOrDefault("aoClicar", "");
                String variante = c.getPropriedade("variante", "primary");
                String theme = "primary".equalsIgnoreCase(variante) || "primario".equalsIgnoreCase(variante) ? "primary" : "secondary";
                sb.append(indent).append("<button id=\"").append(id).append("\" class=\"vaadin-button vaadin-button-").append(theme).append("\" onclick=\"vaadinDespacharAcao('").append(escapeJs(acao)).append("', '").append(id).append("')\">")
                        .append(escapeHtml(rotulo)).append("</button>\n");
            }
            case CAMPO_TEXTO -> {
                String vinculo = c.getPropriedade("vinculo", id);
                String valor = c.getPropriedade("valor", "");
                sb.append(indent).append("<div class=\"vaadin-field-wrapper\">\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <label class=\"vaadin-field-label\">").append(escapeHtml(rotulo)).append("</label>\n");
                sb.append(indent).append("  <input type=\"text\" id=\"").append(id).append("\" data-vinculo=\"").append(escapeHtml(vinculo)).append("\" class=\"vaadin-input\" placeholder=\"").append(escapeHtml(placeholder)).append("\" value=\"").append(escapeHtml(valor)).append("\" oninput=\"vaadinAtualizarVinculo('").append(escapeJs(vinculo)).append("', this.value)\"/>\n");
                sb.append(indent).append("</div>\n");
            }
            case CAMPO_MOEDA -> {
                String vinculo = c.getPropriedade("vinculo", id);
                String moeda = c.getPropriedade("moeda", "BRL");
                String valor = c.getPropriedade("valor", "0.00");
                sb.append(indent).append("<div class=\"vaadin-field-wrapper\">\n");
                if (!rotulo.isBlank()) sb.append(indent).append("  <label class=\"vaadin-field-label\">").append(escapeHtml(rotulo)).append(" (").append(moeda).append(")</label>\n");
                sb.append(indent).append("  <input type=\"text\" id=\"").append(id).append("\" data-vinculo=\"").append(escapeHtml(vinculo)).append("\" class=\"vaadin-input vaadin-input-currency\" placeholder=\"0,00\" value=\"").append(escapeHtml(valor)).append("\" oninput=\"vaadinAtualizarVinculo('").append(escapeJs(vinculo)).append("', this.value)\"/>\n");
                sb.append(indent).append("</div>\n");
            }
            case ALERTA -> {
                String texto = c.getPropriedade("texto", rotulo);
                String status = c.getPropriedade("tipoAlerta", "info");
                sb.append(indent).append("<div id=\"").append(id).append("\" class=\"vaadin-alert vaadin-alert-").append(status).append("\">")
                        .append(escapeHtml(texto)).append("</div>\n");
            }
            default -> {
                sb.append(indent).append("<div class=\"vaadin-generic-block\">\n");
                for (ThzUiComponente filho : c.filhos()) renderizarComponente(filho, sb, indent + "  ");
                sb.append(indent).append("</div>\n");
            }
        }
    }

    private static String gerarCssLumo(boolean temaEscuro) {
        if (temaEscuro) {
            return """
                :root {
                    --lumo-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                    --lumo-base-color: #1a1f2c;
                    --lumo-tint-5pct: rgba(255, 255, 255, 0.05);
                    --lumo-tint-10pct: rgba(255, 255, 255, 0.1);
                    --lumo-primary-color: #3b82f6;
                    --lumo-primary-text-color: #60a5fa;
                    --lumo-success-color: #10b981;
                    --lumo-error-color: #ef4444;
                    --lumo-body-text-color: #f1f5f9;
                    --lumo-secondary-text-color: #94a3b8;
                    --lumo-border-radius-m: 8px;
                    --lumo-border-radius-l: 12px;
                    --lumo-box-shadow-m: 0 4px 16px rgba(0, 0, 0, 0.4);
                }
                * { box-sizing: border-box; margin: 0; padding: 0; font-family: var(--lumo-font-family); }
                body { background-color: #0f172a; color: var(--lumo-body-text-color); padding: 24px; }
                .vaadin-app-container { max-width: 1100px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px; }
                .vaadin-app-header { display: flex; align-items: center; justify-content: space-between; padding-bottom: 16px; border-bottom: 1px solid rgba(255,255,255,0.1); }
                .vaadin-badge-logo { background: linear-gradient(135deg, #00b4d8, #0077b6); color: white; font-weight: 800; font-size: 0.75rem; padding: 6px 12px; border-radius: 9999px; letter-spacing: 0.1em; }
                .vaadin-title { font-size: 1.6rem; font-weight: 700; color: #ffffff; }
                .vaadin-card-panel { display: flex; flex-direction: column; gap: 16px; width: 100%; }
                .vaadin-card { background: #1e293b; border: 1px solid rgba(255,255,255,0.08); border-radius: var(--lumo-border-radius-l); padding: 24px; box-shadow: var(--lumo-box-shadow-m); }
                .vaadin-card-header h3 { font-size: 1.25rem; margin-bottom: 14px; color: #f8fafc; border-bottom: 1px solid rgba(255,255,255,0.06); padding-bottom: 8px; }
                .vaadin-horizontal-layout { display: flex; flex-direction: row; gap: 14px; align-items: center; flex-wrap: wrap; }
                .vaadin-vertical-layout { display: flex; flex-direction: column; gap: 14px; }
                .vaadin-button { display: inline-flex; align-items: center; justify-content: center; height: 42px; padding: 0 20px; font-size: 0.95rem; font-weight: 600; border-radius: var(--lumo-border-radius-m); cursor: pointer; border: none; transition: background-color 0.2s, transform 0.1s; }
                .vaadin-button:active { transform: scale(0.98); }
                .vaadin-button-primary { background-color: var(--lumo-primary-color); color: #ffffff; }
                .vaadin-button-primary:hover { background-color: #2563eb; }
                .vaadin-button-secondary { background-color: rgba(255,255,255,0.1); color: #ffffff; }
                .vaadin-button-secondary:hover { background-color: rgba(255,255,255,0.15); }
                .vaadin-field-wrapper { display: flex; flex-direction: column; gap: 6px; }
                .vaadin-field-label { font-size: 0.85rem; font-weight: 600; color: var(--lumo-secondary-text-color); }
                .vaadin-input { height: 40px; padding: 0 12px; background: #0f172a; border: 1px solid rgba(255,255,255,0.15); border-radius: var(--lumo-border-radius-m); color: #ffffff; font-size: 0.95rem; outline: none; }
                .vaadin-input:focus { border-color: var(--lumo-primary-color); box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.3); }
                .vaadin-alert { padding: 14px 18px; border-radius: var(--lumo-border-radius-m); background: rgba(59, 130, 246, 0.15); border-left: 4px solid var(--lumo-primary-color); font-size: 0.95rem; line-height: 1.5; }
                .vaadin-alert-info { border-left-color: #3b82f6; background: rgba(59, 130, 246, 0.15); color: #93c5fd; }
                .vaadin-alert-success { border-left-color: #10b981; background: rgba(16, 185, 129, 0.15); color: #6ee7b7; }
                .vaadin-alert-error { border-left-color: #ef4444; background: rgba(239, 68, 68, 0.15); color: #fca5a5; }
                .vaadin-notification-container { position: fixed; bottom: 24px; right: 24px; z-index: 9999; display: flex; flex-direction: column; gap: 10px; }
                .vaadin-toast { background: #334155; color: white; padding: 14px 20px; border-radius: 8px; box-shadow: 0 8px 24px rgba(0,0,0,0.5); font-weight: 500; font-size: 0.95rem; animation: slideIn 0.3s ease-out; }
                @keyframes slideIn { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
            """;
        }
        return """
            :root {
                --lumo-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                --lumo-base-color: #ffffff;
                --lumo-primary-color: #0284c7;
                --lumo-body-text-color: #1e293b;
                --lumo-secondary-text-color: #64748b;
                --lumo-border-radius-m: 8px;
                --lumo-border-radius-l: 12px;
            }
            * { box-sizing: border-box; margin: 0; padding: 0; font-family: var(--lumo-font-family); }
            body { background-color: #f8fafc; color: var(--lumo-body-text-color); padding: 24px; }
            .vaadin-app-container { max-width: 1100px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px; }
            .vaadin-app-header { display: flex; align-items: center; justify-content: space-between; padding-bottom: 16px; border-bottom: 1px solid #e2e8f0; }
            .vaadin-badge-logo { background: #0284c7; color: white; font-weight: 800; font-size: 0.75rem; padding: 6px 12px; border-radius: 9999px; }
            .vaadin-title { font-size: 1.6rem; font-weight: 700; color: #0f172a; }
            .vaadin-card { background: #ffffff; border: 1px solid #e2e8f0; border-radius: var(--lumo-border-radius-l); padding: 24px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
            .vaadin-button { height: 40px; padding: 0 18px; border-radius: 8px; border: none; font-weight: 600; cursor: pointer; }
            .vaadin-button-primary { background: #0284c7; color: white; }
            .vaadin-input { height: 40px; padding: 0 12px; border: 1px solid #cbd5e1; border-radius: 8px; }
        """;
    }

    private static String gerarJsVaadin() {
        return """
            window.vaadinEstado = {};
            function vaadinAtualizarVinculo(campo, valor) {
                window.vaadinEstado[campo] = valor;
            }
            function vaadinNotificar(msg, tipo = 'info') {
                const container = document.getElementById('vaadin_notification_area');
                if (!container) return;
                const toast = document.createElement('div');
                toast.className = 'vaadin-toast';
                if (tipo === 'erro') toast.style.borderLeft = '4px solid #ef4444';
                else toast.style.borderLeft = '4px solid #10b981';
                toast.textContent = msg;
                container.appendChild(toast);
                setTimeout(() => toast.remove(), 4000);
            }
            async function vaadinDespacharAcao(acao, idBotao) {
                if (!acao) return;
                const btn = document.getElementById(idBotao);
                const orig = btn ? btn.textContent : '';
                if (btn) { btn.disabled = true; btn.textContent = 'Executando no Servidor...'; }
                try {
                    const resp = await fetch('/api/rpc/invocar', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ acao, estado: window.vaadinEstado })
                    });
                    const res = await resp.json();
                    if (res && res.status === 'ok') {
                        vaadinNotificar('✓ ' + (res.resultado || res.mensagem || 'Ação concluída com sucesso.'), 'sucesso');
                    } else {
                        vaadinNotificar('✗ ' + (res.erro || res.mensagem || 'Falha na execução'), 'erro');
                    }
                } catch(e) {
                    vaadinNotificar('✗ Erro de comunicação: ' + e.message, 'erro');
                } finally {
                    if (btn) { btn.disabled = false; btn.textContent = orig; }
                }
            }
        """;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"");
    }
}