package thz.lang.gui.config;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Diálogo moderno de Configuração e Seleção de JVM para a interface THZ-LANG Desktop.
 */
public final class DialogoConfiguracaoJvm extends JDialog {

    private final JComboBox<DetectorJvm.InfoJvm> comboJvms;
    private final JTextField txtCaminhoPersonalizado;
    private final JTextArea txtDetalhes;
    private final JButton btnSalvar;
    private final JButton btnCancelar;
    private final JButton btnProcurar;
    private final JButton btnTestar;

    private boolean salvo = false;
    private String jvmSelecionada = "";

    public DialogoConfiguracaoJvm(Frame proprietario, ConfiguracaoDesktop configAtual) {
        super(proprietario, "Configurações de JVM — THZ-LANG Desktop", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(650, 480);
        setLocationRelativeTo(proprietario);
        setLayout(new BorderLayout(12, 12));

        JPanel painelPrincipal = new JPanel();
        painelPrincipal.setLayout(new BoxLayout(painelPrincipal, BoxLayout.Y_AXIS));
        painelPrincipal.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Cabeçalho
        JLabel lblTitulo = new JLabel("⚙ Seleção do Java Runtime Environment (JVM)");
        lblTitulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        JLabel lblDescricao = new JLabel("Escolha qual JVM você deseja utilizar para executar os programas .thz.");
        lblDescricao.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblDescricao.setForeground(Color.GRAY);

        painelPrincipal.add(lblTitulo);
        painelPrincipal.add(Box.createVerticalStrut(4));
        painelPrincipal.add(lblDescricao);
        painelPrincipal.add(Box.createVerticalStrut(16));

        // Combo de JVMs detectadas
        JLabel lblCombo = new JLabel("JVMs Detectadas no Sistema:");
        lblCombo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        painelPrincipal.add(lblCombo);
        painelPrincipal.add(Box.createVerticalStrut(6));

        List<DetectorJvm.InfoJvm> jvms = DetectorJvm.detectarJvmsDisponiveis();
        comboJvms = new JComboBox<>(jvms.toArray(new DetectorJvm.InfoJvm[0]));
        comboJvms.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        // Selecionar atual ou salva
        String salva = configAtual != null ? configAtual.caminhoJvm() : "";
        if (salva != null && !salva.isBlank()) {
            for (int i = 0; i < jvms.size(); i++) {
                if (jvms.get(i).caminho().equalsIgnoreCase(salva)) {
                    comboJvms.setSelectedIndex(i);
                    break;
                }
            }
        }

        painelPrincipal.add(comboJvms);
        painelPrincipal.add(Box.createVerticalStrut(12));

        // Caminho personalizado
        JLabel lblManual = new JLabel("Ou especifique o caminho da pasta JDK/JRE:");
        lblManual.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        painelPrincipal.add(lblManual);
        painelPrincipal.add(Box.createVerticalStrut(6));

        JPanel painelManual = new JPanel(new BorderLayout(8, 0));
        painelManual.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        txtCaminhoPersonalizado = new JTextField(salva);
        btnProcurar = new JButton("Procurar…");
        painelManual.add(txtCaminhoPersonalizado, BorderLayout.CENTER);
        painelManual.add(btnProcurar, BorderLayout.EAST);
        painelPrincipal.add(painelManual);
        painelPrincipal.add(Box.createVerticalStrut(12));

        // Painel de Detalhes e Teste
        JLabel lblDetalhes = new JLabel("Detalhes e Diagnóstico da JVM:");
        lblDetalhes.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        painelPrincipal.add(lblDetalhes);
        painelPrincipal.add(Box.createVerticalStrut(6));

        txtDetalhes = new JTextArea(6, 40);
        txtDetalhes.setEditable(false);
        txtDetalhes.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollDetalhes = new JScrollPane(txtDetalhes);
        painelPrincipal.add(scrollDetalhes);
        painelPrincipal.add(Box.createVerticalStrut(8));

        btnTestar = new JButton("🧪 Testar JVM Selecionada");
        btnTestar.setAlignmentX(Component.LEFT_ALIGNMENT);
        painelPrincipal.add(btnTestar);

        add(painelPrincipal, BorderLayout.CENTER);

        // Barra inferior de botões
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnCancelar = new JButton("Cancelar");
        btnSalvar = new JButton("Salvar e Aplicar");
        btnSalvar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        painelBotoes.add(btnCancelar);
        painelBotoes.add(btnSalvar);
        add(painelBotoes, BorderLayout.SOUTH);

        // Listeners
        comboJvms.addActionListener(e -> {
            DetectorJvm.InfoJvm sel = (DetectorJvm.InfoJvm) comboJvms.getSelectedItem();
            if (sel != null) {
                txtCaminhoPersonalizado.setText(sel.caminho());
                atualizarDetalhes(sel.caminho());
            }
        });

        btnProcurar.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Selecionar Pasta do JDK / JRE");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File dir = fc.getSelectedFile();
                txtCaminhoPersonalizado.setText(dir.getAbsolutePath());
                atualizarDetalhes(dir.getAbsolutePath());
            }
        });

        btnTestar.addActionListener(e -> testarJvm(txtCaminhoPersonalizado.getText()));

        btnSalvar.addActionListener(e -> {
            String caminho = txtCaminhoPersonalizado.getText().trim();
            if (!caminho.isBlank() && !DetectorJvm.ehDiretorioJvmValido(caminho)) {
                int res = JOptionPane.showConfirmDialog(this,
                        "O caminho informado não parece conter um executável 'bin/java' válido.\nDeseja salvar mesmo assim?",
                        "Aviso de Validação", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (res != JOptionPane.YES_OPTION) return;
            }
            this.jvmSelecionada = caminho;
            this.salvo = true;
            dispose();
        });

        btnCancelar.addActionListener(e -> dispose());

        // Inicializar detalhes
        if (!txtCaminhoPersonalizado.getText().isBlank()) {
            atualizarDetalhes(txtCaminhoPersonalizado.getText());
        } else if (comboJvms.getSelectedItem() != null) {
            atualizarDetalhes(((DetectorJvm.InfoJvm) comboJvms.getSelectedItem()).caminho());
        }
    }

    private void atualizarDetalhes(String caminho) {
        if (caminho == null || caminho.isBlank()) {
            txtDetalhes.setText("Nenhuma JVM selecionada (será utilizado o padrão do sistema).");
            return;
        }
        DetectorJvm.InfoJvm info = DetectorJvm.inspecionarJvm("Selecionada", caminho);
        StringBuilder sb = new StringBuilder();
        sb.append("Caminho:     ").append(info.caminho()).append("\n");
        sb.append("Versão:      ").append(info.versao()).append("\n");
        if (info.fornecedor() != null && !info.fornecedor().isBlank()) {
            sb.append("Fornecedor:  ").append(info.fornecedor()).append("\n");
        }
        sb.append("Status:      ").append(DetectorJvm.ehDiretorioJvmValido(caminho) ? "✓ JDK/JRE Válido" : "✗ Executável java não encontrado").append("\n");
        sb.append("Ativa Agora: ").append(info.ehAtual() ? "Sim" : "Não");
        txtDetalhes.setText(sb.toString());
    }

    private void testarJvm(String caminho) {
        if (caminho == null || caminho.isBlank() || !DetectorJvm.ehDiretorioJvmValido(caminho)) {
            txtDetalhes.setText("Erro: Não foi possível testar. Caminho inválido ou java não encontrado.");
            return;
        }
        txtDetalhes.setText("Executando teste no binário java...");
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                File exe = new File(caminho, "bin/java.exe");
                if (!exe.exists()) exe = new File(caminho, "bin/java");
                ProcessBuilder pb = new ProcessBuilder(exe.getAbsolutePath(), "-version");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder out = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        out.append(line).append("\n");
                    }
                    p.waitFor();
                    return out.toString();
                }
            }

            @Override
            protected void done() {
                try {
                    String saida = get();
                    txtDetalhes.setText("✓ Teste bem-sucedido na JVM selecionada:\n" + saida);
                } catch (Exception ex) {
                    txtDetalhes.setText("Falha ao executar teste: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    public boolean foiSalvo() {
        return salvo;
    }

    public String getJvmSelecionada() {
        return jvmSelecionada;
    }

    public static String exibir(Frame proprietario, ConfiguracaoDesktop configAtual) {
        DialogoConfiguracaoJvm dlg = new DialogoConfiguracaoJvm(proprietario, configAtual);
        dlg.setVisible(true);
        if (dlg.foiSalvo()) {
            return dlg.getJvmSelecionada();
        }
        return null;
    }
}
