import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Principal {

    private static final Color COR_FUNDO = Color.BLACK;
    private static final Color COR_PRIMARIA = new Color(30, 64, 175);
    private static final Color COR_SECUNDARIA = new Color(16, 185, 129);
    private static final Color COR_TEXTO = new Color(241, 245, 249);
    private static final Color COR_MUTED = new Color(203, 213, 225);

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        UIManager.put("Label.foreground", COR_TEXTO);
        UIManager.put("Button.background", new Color(51, 65, 85));
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("TextField.foreground", new Color(15, 23, 42));
        UIManager.put("ComboBox.foreground", new Color(15, 23, 42));
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("TextArea.foreground", COR_TEXTO);
        UIManager.put("Panel.background", COR_FUNDO);
        UIManager.put("OptionPane.background", COR_FUNDO);
        UIManager.put("OptionPane.foreground", COR_TEXTO);
        UIManager.put("OptionPane.messageForeground", COR_TEXTO);
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", new Font("Segoe UI", Font.BOLD, 13));
        UIManager.put("OptionPane.buttonBackground", new Color(51, 65, 85));
        UIManager.put("OptionPane.buttonForeground", Color.WHITE);

        SwingUtilities.invokeLater(() -> {
            JFrame framePrincipal = new JFrame("Agência de Voos");
            framePrincipal.setSize(1080, 720);
            framePrincipal.setMinimumSize(new Dimension(900, 620));
            framePrincipal.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            framePrincipal.setLocationRelativeTo(null);
            framePrincipal.setResizable(true);

            CardLayout cardLayout = new CardLayout();
            JPanel cards = new JPanel(cardLayout);
            cards.setBackground(COR_FUNDO);

                AgenciaService agenciaService = new AgenciaService();
                TelaClientes painelClientes = new TelaClientes(
                    () -> cardLayout.show(cards, "menu"), agenciaService);
                TelaVoos painelVoos = new TelaVoos(
                    () -> cardLayout.show(cards, "menu"), agenciaService);
                JPanel painelMenu = criarMenu(cardLayout, cards, painelClientes, painelVoos);

            cards.add(painelMenu, "menu");
            cards.add(painelClientes, "clientes");
            cards.add(painelVoos, "voos");

            framePrincipal.setContentPane(cards);
            framePrincipal.setVisible(true);
        });
    }

    private static JPanel criarMenu(CardLayout cardLayout, JPanel cards,
                                    TelaClientes painelClientes, TelaVoos painelVoos) {
        JPanel painelFundo = new JPanel(new BorderLayout());
        painelFundo.setBackground(COR_FUNDO);
        painelFundo.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel painelCabecalho = new JPanel();
        painelCabecalho.setOpaque(false);
        painelCabecalho.setLayout(new BoxLayout(painelCabecalho, BoxLayout.Y_AXIS));

        JLabel lblTitulo = new JLabel("Agência de Voos");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblTitulo.setForeground(COR_TEXTO);
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitulo = new JLabel("Controle de clientes e voos em um só lugar");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitulo.setForeground(COR_MUTED);
        lblSubtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        painelCabecalho.add(Box.createVerticalStrut(8));
        painelCabecalho.add(lblTitulo);
        painelCabecalho.add(Box.createVerticalStrut(6));
        painelCabecalho.add(lblSubtitulo);

        JPanel painelBotoes = new JPanel(new GridLayout(3, 1, 0, 16));
        painelBotoes.setOpaque(false);
        painelBotoes.setBorder(new EmptyBorder(20, 40, 0, 40));

        JButton btnGerenciarClientes = criarBotaoPrimario("Gerenciar Clientes", COR_PRIMARIA);
        JButton btnGerenciarVoos = criarBotaoPrimario("Gerenciar Voos", COR_SECUNDARIA);
        JButton btnSair = criarBotaoSecundario("Sair");

        btnGerenciarClientes.addActionListener(e -> {
            painelClientes.atualizarVoosClienteAtual();
            cardLayout.show(cards, "clientes");
        });
        btnGerenciarVoos.addActionListener(e -> {
            painelVoos.atualizarTela();
            cardLayout.show(cards, "voos");
        });
        btnSair.addActionListener(e -> System.exit(0));

        painelBotoes.add(btnGerenciarClientes);
        painelBotoes.add(btnGerenciarVoos);
        painelBotoes.add(btnSair);

        painelFundo.add(painelCabecalho, BorderLayout.NORTH);
        painelFundo.add(painelBotoes, BorderLayout.CENTER);

        return painelFundo;
    }

    private static JButton criarBotaoPrimario(String texto, Color cor) {
        JButton botao = new JButton(texto);
        botao.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        botao.setOpaque(true);
        botao.setFont(new Font("Segoe UI", Font.BOLD, 16));
        botao.setBackground(cor);
        botao.setForeground(Color.WHITE);
        botao.setContentAreaFilled(true);
        botao.setFocusPainted(false);
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botao.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        botao.setPreferredSize(new Dimension(260, 58));
        return botao;
    }

    private static JButton criarBotaoSecundario(String texto) {
        JButton botao = new JButton(texto);
        botao.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        botao.setOpaque(true);
        botao.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        botao.setBackground(new Color(226, 232, 240));
        botao.setForeground(new Color(15, 23, 42));
        botao.setContentAreaFilled(true);
        botao.setFocusPainted(false);
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botao.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        botao.setPreferredSize(new Dimension(260, 52));
        return botao;
    }
}
