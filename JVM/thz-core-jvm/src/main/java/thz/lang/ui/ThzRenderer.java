package thz.lang.ui;

import thz.lang.interpretador.InterpretadorThz;
import thz.lang.interpretador.ValorThz;

/**
 * Contrato de renderização desacoplado — permite múltiplas implementações
 * (Swing, WebView, Headless) sem o thz-core depender de thz-gui.
 *
 * Implementação padrão em CLI é webview (HTML + bridge); IDE Desktop
 * injeta a variante Swing via ServiceLoader / registro manual.
 */
public interface ThzRenderer {

    /**
     * Renderiza um formulário THZ (Registro + operação alvo) e retorna mensagem de status.
     * Implementações não devem lançar exceção para o usuário final; devem retornar string amigável.
     */
    String renderizarFormulario(ValorThz.Registro registro, String operacaoAlvo, InterpretadorThz interpretador);
}
