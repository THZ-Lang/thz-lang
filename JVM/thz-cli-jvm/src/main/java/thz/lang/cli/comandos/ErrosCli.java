package thz.lang.cli;

import java.nio.file.Path;
import java.util.List;

/**
 * Fábrica centralizada de mensagens de erro e alerta para o CLI da THZ-LANG.
 * Padroniza prefixos, formatação e saída para stderr.
 */
public final class ErrosCli {

    private ErrosCli() {}

    // ── Erros fatais (imprimem e saem com código 1) ──────────────────────────

    public static void erroArquivoNaoEncontrado(String arquivo) {
        System.err.println("[ERRO] Arquivo não encontrado: " + arquivo);
        System.exit(1);
    }

    public static void erroNenhumArquivoEspecificado(String uso) {
        System.err.println("[ERRO] Nenhum arquivo .thz ou .thzui especificado. Use: " + uso);
        System.exit(1);
    }

    public static void erroArquivoNaoEncontradoAposBusca(String arquivo) {
        System.err.println("[ERRO] Arquivo não encontrado após pesquisa recursiva: " + arquivo);
        System.exit(1);
    }

    public static void erroDiretorioNaoEncontrado(Path diretorio) {
        System.err.println("[ERRO] Diretório não encontrado: " + diretorio.toAbsolutePath());
        System.exit(1);
    }

    public static void erroComandoDesconhecido(String comando) {
        System.err.println("[ERRO] Comando desconhecido: " + comando
                + " (use: check | run | fmt | ast | audit | doc | ir | repl | gui | --ajuda)");
        System.exit(1);
    }

    public static void erroPrincipalNaoEncontrado(String nome) {
        System.err.println("[ERRO] Entrada '--principal " + nome
                + "' não encontrada como PROCEDIMENTO nem OPERACAO.");
        System.exit(1);
    }

    public static void erroNenhumaOperacaoExecutavel() {
        System.err.println(
                "[ERRO] Nenhuma operação com corpo executável declarada. Adicione um bloco INICIO ... FIM a uma OPERACAO ou declare PROCEDIMENTO Principal.");
        System.exit(1);
    }

    public static void erroFalhaAoIniciarGui(String mensagem) {
        System.err.println("[ERRO] Falha ao iniciar a Desktop IDE Swing: " + mensagem);
    }

    public static void erroGuiNaoEncontrada() {
        System.err.println("[ERRO] Desktop IDE Swing não encontrada (módulo thz-gui-jvm ausente no classpath).");
        System.err.println(
                "       Inicie via: ./gradlew :thz-gui-jvm:gui ou utilize o script scripts/gui.ps1 / scripts/gui.sh");
    }

    // ── Alertas e status (imprimem e retornam) ───────────────────────────────

    public static String formatarArquivoNaoEncontrado(String arquivo) {
        return "[ERRO] Arquivo não encontrado: " + arquivo;
    }

    public static void statusComandoCheck(int totalErros) {
        System.err.println("[THZ CHECK] " + totalErros + " erro(s) semântico(s).");
    }

    public static void statusFmtNaoFormatado() {
        System.err.println("[THZ FMT] Arquivo não está formatado. Use `thz fmt --escrever` para corrigir.");
    }

    public static void linhaDiferenca(int linha, String esperada, String obtida) {
        System.err.println("  Linha " + linha + " esperada: " + CliHelper.q(esperada));
        System.err.println("  Linha " + linha + " obtida:   " + CliHelper.q(obtida));
    }

    public static void statusAuditConformidadeEstrita() {
        System.err.println(
                "\n[THZ AUDIT] Falha de conformidade estrita: o programa possui pendências críticas de governança.");
    }

    public static void falhaEmLote(String nomeArquivo, String mensagem) {
        System.err.println("  [FALHA] " + nomeArquivo + " : " + mensagem);
    }

    public static void alerta(String titulo, String mensagem) {
        System.err.println("[ALERTA] " + titulo + ": " + mensagem);
    }

    public static void entrada(String prompt) {
        System.err.print("[ENTRADA] " + prompt);
        System.err.flush();
    }

    public static void webViewFalha(String mensagem) {
        System.err.println("[THZ WebView] " + mensagem);
    }

    public static void webViewDica() {
        System.err.println("[THZ] Use thz ui --html ou instale a IDE Desktop (./gradlew :thz-gui:gui) para render Swing.");
    }

    public static void displayIndisponivel(String motivo, String fallback) {
        System.err.println("[THZ-UI] Display gráfico indisponível (" + motivo + "). Alternando para modo " + fallback + "...");
    }

    public static void displaySwingIndisponivel(String motivo) {
        System.err.println("[THZ-UI SWING] Display gráfico indisponível (" + motivo + "). Alternando para modo Web...");
    }

    public static void falhaManualPdf(String mensagem) {
        System.err.println("[ERRO] Falha ao compilar manuais PDF: " + mensagem);
    }
}
