package thz.lang.cli;

import java.nio.file.Path;

/**
 * Fábrica centralizada de mensagens de erro e alerta para o CLI da THZ-LANG.
 * Padroniza prefixos, formatação e saída via CliLogger (ThzLog backend).
 */
public final class ErrosCli {

    private ErrosCli() {}

    // ── Erros fatais (imprimem e saem com código 1) ──────────────────────────

    public static void erroArquivoNaoEncontrado(String arquivo) {
        CliLogger.erro("[ERRO] Arquivo não encontrado: " + arquivo);
        System.exit(1);
    }

    public static void erroNenhumArquivoEspecificado(String uso) {
        CliLogger.erro("[ERRO] Nenhum arquivo .thz ou .thzui especificado. Use: " + uso);
        System.exit(1);
    }

    public static void erroArquivoNaoEncontradoAposBusca(String arquivo) {
        CliLogger.erro("[ERRO] Arquivo não encontrado após pesquisa recursiva: " + arquivo);
        System.exit(1);
    }

    public static void erroDiretorioNaoEncontrado(Path diretorio) {
        CliLogger.erro("[ERRO] Diretório não encontrado: " + diretorio.toAbsolutePath());
        System.exit(1);
    }

    public static void erroComandoDesconhecido(String comando) {
        CliLogger.erro("[ERRO] Comando desconhecido: " + comando
                + " (use: check | run | fmt | ast | audit | doc | ir | repl | gui | --ajuda)");
        System.exit(1);
    }

    public static void erroPrincipalNaoEncontrado(String nome) {
        CliLogger.erro("[ERRO] Entrada '--principal " + nome
                + "' não encontrada como PROCEDIMENTO nem OPERACAO.");
        System.exit(1);
    }

    public static void erroNenhumaOperacaoExecutavel() {
        CliLogger.erro(
                "[ERRO] Nenhuma operação com corpo executável declarada. Adicione um bloco INICIO ... FIM a uma OPERACAO ou declare PROCEDIMENTO Principal.");
        System.exit(1);
    }

    public static void erroFalhaAoIniciarGui(String mensagem) {
        CliLogger.erro("[ERRO] Falha ao iniciar a Desktop IDE Swing: " + mensagem);
    }

    public static void erroGuiNaoEncontrada() {
        CliLogger.erro("[ERRO] Desktop IDE Swing não encontrada (módulo thz-gui-jvm ausente no classpath).");
        CliLogger.erro("       Inicie via: ./gradlew :thz-gui-jvm:gui ou utilize o script scripts/gui.ps1 / scripts/gui.sh");
    }

    // ── Alertas e status ─────────────────────────────────────────────────────

    public static String formatarArquivoNaoEncontrado(String arquivo) {
        return "[ERRO] Arquivo não encontrado: " + arquivo;
    }

    public static void statusComandoCheck(int totalErros) {
        CliLogger.aviso("[THZ CHECK] " + totalErros + " erro(s) semântico(s).");
    }

    public static void statusFmtNaoFormatado() {
        CliLogger.aviso("[THZ FMT] Arquivo não está formatado. Use `thz fmt --escrever` para corrigir.");
    }

    public static void linhaDiferenca(int linha, String esperada, String obtida) {
        CliLogger.aviso("  Linha " + linha + " esperada: " + CliHelper.q(esperada));
        CliLogger.aviso("  Linha " + linha + " obtida:   " + CliHelper.q(obtida));
    }

    public static void statusAuditConformidadeEstrita() {
        CliLogger.erro(
                "\n[THZ AUDIT] Falha de conformidade estrita: o programa possui pendências críticas de governança.");
    }

    public static void falhaEmLote(String nomeArquivo, String mensagem) {
        CliLogger.aviso("  [FALHA] " + nomeArquivo + " : " + mensagem);
    }

    public static void alerta(String titulo, String mensagem) {
        CliLogger.aviso("[ALERTA] " + titulo + ": " + mensagem);
    }

    public static void entrada(String prompt) {
        System.err.print("[ENTRADA] " + prompt);
        System.err.flush();
    }

    public static void webViewFalha(String mensagem) {
        CliLogger.aviso("[THZ WebView] " + mensagem);
    }

    public static void webViewDica() {
        CliLogger.info("[THZ] Use thz ui --html ou instale a IDE Desktop (./gradlew :thz-gui:gui) para render Swing.");
    }

    public static void displayIndisponivel(String motivo, String fallback) {
        CliLogger.aviso("[THZ-UI] Display gráfico indisponível (" + motivo + "). Alternando para modo " + fallback + "...");
    }

    public static void displaySwingIndisponivel(String motivo) {
        CliLogger.aviso("[THZ-UI SWING] Display gráfico indisponível (" + motivo + "). Alternando para modo Web...");
    }

    public static void falhaManualPdf(String mensagem) {
        CliLogger.erro("[ERRO] Falha ao compilar manuais PDF: " + mensagem);
    }
}
