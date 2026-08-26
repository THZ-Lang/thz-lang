package thz.lang.gui;

import thz.lang.io.ThzLocalizadorRecursos;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Construtor e gerenciador da galeria de exemplos (.thz e .thzui) para o menu da IDE Swing.
 * Organiza em submenus temáticos por capacidade: v3.0, Analytics/DAX, Brasil Digital,
 * Bancos & Mensageria, SIMD/Performance, UI Declarativa e Lista Completa.
 */
public final class GaleriaExemplos {

    private GaleriaExemplos() {}

    /**
     * Localiza a raiz de exemplos em cascata e monta o menu categorizado.
     */
    public static JMenu criarMenuExemplos(Consumer<File> onCarregarExemplo) {
        JMenu menu = new JMenu("Exemplos");
        menu.setToolTipText("Galeria de programas e demonstrações — clique para carregar no editor");

        File raiz = localizarPastaExemplos();
        if (raiz == null || !raiz.isDirectory()) {
            JMenuItem vazio = new JMenuItem("(pasta 'exemplos' não encontrada)");
            vazio.setEnabled(false);
            menu.add(vazio);
            return menu;
        }

        List<File> todosArquivos = coletarTodosExemplos(raiz);
        if (todosArquivos.isEmpty()) {
            JMenuItem vazio = new JMenuItem("(nenhum arquivo .thz ou .thzui encontrado)");
            vazio.setEnabled(false);
            menu.add(vazio);
            return menu;
        }

        // 1. Submenu: 🚀 Novidades v3.0 & Recursos Recentes
        JMenu menuNovidades = new JMenu("🚀 Novidades v3.0 & Recursos Recentes");
        menuNovidades.setToolTipText("Demonstrações dos recursos adicionados da v2.6 até a v3.0");
        adicionarPorPadrao(menuNovidades, todosArquivos, onCarregarExemplo, Set.of(
                "brasil_enderecos_ceps_thzdbi.thz",
                "brasil_pix_boletos_dia_a_dia.thz",
                "snapshot_compactacao_workspace.thz",
                "dax_kpis_analytics.thz",
                "excel_planilhas_procv.thz",
                "estatistica_e_previsao.thz",
                "limpeza_dados_caoticos_etl.thz",
                "banco_dados_conectores.thz",
                "raw_sql_consultas_diretas.thz",
                "mensageria_rabbitmq_kafka.thz",
                "rust_embutido.thz"
        ));
        menu.add(menuNovidades);

        // 2. Submenu: 📊 Analytics, DAX, Excel & Estatística
        JMenu menuAnalytics = new JMenu("📊 Analytics, DAX, Excel & Estatística");
        adicionarPorFiltro(menuAnalytics, todosArquivos, onCarregarExemplo,
                nome -> nome.contains("dax") || nome.contains("excel") || nome.contains("estatistica")
                        || nome.contains("limpeza") || nome.contains("analytics") || nome.contains("metricas")
                        || nome.contains("etl") || nome.contains("linq") || nome.contains("relatorio"));
        menu.add(menuAnalytics);

        // 3. Submenu: 🇧🇷 Brasil Digital, CEPs & .thzdbi
        JMenu menuBrasil = new JMenu("🇧🇷 Brasil Digital, CEPs & .thzdbi");
        adicionarPorFiltro(menuBrasil, todosArquivos, onCarregarExemplo,
                nome -> nome.contains("brasil") || nome.contains("cep") || nome.contains("pix")
                        || nome.contains("boleto") || nome.contains("folha") || nome.contains("imposto")
                        || nome.contains("faturamento"));
        menu.add(menuBrasil);

        // 4. Submenu: 💾 Bancos de Dados, SQL & Mensageria
        JMenu menuDados = new JMenu("💾 Bancos de Dados, SQL & Mensageria");
        adicionarPorFiltro(menuDados, todosArquivos, onCarregarExemplo,
                nome -> nome.contains("banco") || nome.contains("sql") || nome.contains("mensageria")
                        || nome.contains("rabbitmq") || nome.contains("kafka") || nome.contains("persistencia")
                        || nome.contains("streaming") || nome.contains("consulta"));
        menu.add(menuDados);

        // 5. Submenu: ⚡ Performance, SIMD & Rust Nativo
        JMenu menuPerf = new JMenu("⚡ Performance, SIMD & Memória Arena");
        adicionarPorFiltro(menuPerf, todosArquivos, onCarregarExemplo,
                nome -> nome.contains("simd") || nome.contains("rust") || nome.contains("bloco_memoria")
                        || nome.contains("arena") || nome.contains("lote") || nome.contains("wasm")
                        || nome.contains("performance"));
        menu.add(menuPerf);

        // 6. Submenu: 🖥️ Interfaces Gráficas & Telas (.thzui / Swing)
        JMenu menuUi = new JMenu("🖥️ Interfaces Declarativas & Telas (.thzui)");
        adicionarPorFiltro(menuUi, todosArquivos, onCarregarExemplo,
                nome -> nome.endsWith(".thzui") || nome.contains("tela") || nome.contains("formulario")
                        || nome.contains("ui") || nome.contains("studio") || nome.contains("cadastro"));
        menu.add(menuUi);

        // 7. Submenu: 🏛️ Governança, Contratos & Core
        JMenu menuCore = new JMenu("🏛️ Governança, Contratos & Core");
        adicionarPorFiltro(menuCore, todosArquivos, onCarregarExemplo,
                nome -> nome.contains("pedidos") || nome.contains("contrato") || nome.contains("governanca")
                        || nome.contains("auditoria") || nome.contains("self_hosting") || nome.contains("regra"));
        menu.add(menuCore);

        menu.addSeparator();

        // 8. Submenu: 📂 Todos os Exemplos (Lista Completa A-Z)
        JMenu menuTodos = new JMenu("📂 Todos os Exemplos (" + todosArquivos.size() + " programas)");
        for (File f : todosArquivos) {
            menuTodos.add(criarItemExemplo(f, onCarregarExemplo));
        }
        menu.add(menuTodos);

        return menu;
    }

    private static File localizarPastaExemplos() {
        File melhor = null;
        int maxThz = 0;

        File current = new File(".").getAbsoluteFile();
        while (current != null) {
            File ex = new File(current, "exemplos");
            if (ex.isDirectory()) {
                File[] fs = ex.listFiles((d, n) -> n.toLowerCase().endsWith(".thz") || n.toLowerCase().endsWith(".thzui"));
                int count = fs != null ? fs.length : 0;
                if (count > maxThz) {
                    maxThz = count;
                    melhor = ex;
                }
            }
            current = current.getParentFile();
        }
        if (melhor != null) return melhor;

        String[] candidatos = {
                "exemplos",
                "../exemplos",
                "../../exemplos",
                "../../../exemplos",
                "JVM/exemplos",
                "thz-lang-engine-JVM/exemplos"
        };
        for (String c : candidatos) {
            File f = new File(c);
            if (f.isDirectory()) return f;
        }

        var achado = ThzLocalizadorRecursos.localizarArquivo("faturamento.thz", Path.of("."), List.of(".thz"));
        if (achado.isPresent() && achado.get().getParent() != null) {
            return achado.get().getParent().toFile();
        }
        return null;
    }

    private static List<File> coletarTodosExemplos(File raiz) {
        Set<File> unicos = new TreeSet<>(Comparator.comparing(File::getName));
        coletarRecursivo(raiz, unicos);
        return new ArrayList<>(unicos);
    }

    private static void coletarRecursivo(File pasta, Set<File> unicos) {
        File[] fs = pasta.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory() && !f.getName().startsWith(".") && !f.getName().equalsIgnoreCase("target") && !f.getName().equalsIgnoreCase("build")) {
                coletarRecursivo(f, unicos);
            } else if (f.isFile()) {
                String n = f.getName().toLowerCase();
                if (n.endsWith(".thz") || n.endsWith(".thzui")) {
                    unicos.add(f);
                }
            }
        }
    }

    private static void adicionarPorPadrao(JMenu menu, List<File> todos, Consumer<File> onCarregarExemplo, Set<String> nomes) {
        boolean algum = false;
        for (File f : todos) {
            String fName = f.getName().toLowerCase();
            for (String n : nomes) {
                if (fName.equals(n.toLowerCase()) || fName.contains(n.toLowerCase().replace(".thz", ""))) {
                    menu.add(criarItemExemplo(f, onCarregarExemplo));
                    algum = true;
                    break;
                }
            }
        }
        if (!algum) {
            for (File f : todos) {
                if (f.getName().toLowerCase().startsWith("v30_") || f.getName().toLowerCase().startsWith("v29_")) {
                    menu.add(criarItemExemplo(f, onCarregarExemplo));
                    algum = true;
                }
            }
        }
    }

    private static void adicionarPorFiltro(JMenu menu, List<File> todos, Consumer<File> onCarregarExemplo, java.util.function.Predicate<String> filtro) {
        boolean algum = false;
        for (File f : todos) {
            String nomeMin = f.getName().toLowerCase();
            if (filtro.test(nomeMin)) {
                menu.add(criarItemExemplo(f, onCarregarExemplo));
                algum = true;
            }
        }
        if (!algum) {
            JMenuItem vazio = new JMenuItem("(nenhum exemplo nesta categoria)");
            vazio.setEnabled(false);
            menu.add(vazio);
        }
    }

    private static JMenuItem criarItemExemplo(File f, Consumer<File> onCarregarExemplo) {
        String nome = f.getName();
        String rotulo = nome.replaceFirst("^\\d+-", "")
                .replace(".thzui", " [UI]")
                .replace(".thz", "")
                .replace('_', ' ');
        JMenuItem item = new JMenuItem(rotulo);
        item.setToolTipText(f.getAbsolutePath());
        item.addActionListener(e -> onCarregarExemplo.accept(f));
        return item;
    }
}

