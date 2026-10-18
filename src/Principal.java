import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Principal {

    private static final Color COR_FUNDO = Tema.FUNDO;
    private static final Color COR_PRIMARIA = Tema.PRIMARIA;
    private static final Color COR_SECUNDARIA = Tema.CABECALHO;
    private static final Color COR_TEXTO = Tema.TEXTO;
    private static final Color COR_MUTED = Tema.TEXTO_SECUNDARIO;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        Tema.aplicar();

        SwingUtilities.invokeLater(Principal::iniciarAplicacao);
    }

    private static void iniciarAplicacao() {
        AgenciaService agenciaService = null;
        JFrame framePrincipal = null;
        try {
            agenciaService = new AgenciaService();
            AgenciaService serviceAberto = agenciaService;
            framePrincipal = new JFrame("Agência de Voos");
            JFrame janela = framePrincipal;
            framePrincipal.setSize(1080, 720);
            framePrincipal.setMinimumSize(new Dimension(900, 620));
            framePrincipal.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            framePrincipal.setLocationRelativeTo(null);
            framePrincipal.setResizable(true);

            CardLayout cardLayout = new CardLayout();
            JPanel cards = new JPanel(cardLayout);
            cards.setBackground(COR_FUNDO);

            Runnable voltar = () -> cardLayout.show(cards, "menu");
            TelaClientes painelClientes = new TelaClientes(voltar, agenciaService);
            TelaVoos painelVoos = new TelaVoos(voltar, agenciaService);
            TelaAssociacoes painelAssociacoes = new TelaAssociacoes(voltar, agenciaService);
            Runnable encerrar = () -> {
                try {
                    serviceAberto.close();
                } catch (RuntimeException e) {
                    JOptionPane.showMessageDialog(janela, "Falha ao fechar os arquivos:\n" + e.getMessage(),
                            "Encerramento", JOptionPane.ERROR_MESSAGE);
                } finally {
                    janela.dispose();
                }
            };
            framePrincipal.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    encerrar.run();
                }
            });
            JPanel painelMenu = criarMenu(cardLayout, cards, painelClientes, painelVoos,
                    painelAssociacoes, encerrar);

            cards.add(painelMenu, "menu");
            cards.add(painelClientes, "clientes");
            cards.add(painelVoos, "voos");
            cards.add(painelAssociacoes, "associacoes");

            framePrincipal.setContentPane(cards);
            framePrincipal.setVisible(true);
            if (!agenciaService.getAvisosInicializacao().isEmpty()) {
                JOptionPane.showMessageDialog(framePrincipal,
                        String.join("\n", agenciaService.getAvisosInicializacao()),
                        "Verificação dos dados", JOptionPane.WARNING_MESSAGE);
            }
        } catch (RuntimeException e) {
            if (agenciaService != null) {
                try {
                    agenciaService.close();
                } catch (RuntimeException falhaFechamento) {
                    e.addSuppressed(falhaFechamento);
                }
            }
            if (framePrincipal != null) framePrincipal.dispose();
            JOptionPane.showMessageDialog(null,
                    "Não foi possível iniciar a Agência de Voos.\n" + e.getMessage()
                    + "\nVerifique a permissão de acesso à pasta dados e tente novamente.",
                    "Erro de inicialização", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JPanel criarMenu(CardLayout cardLayout, JPanel cards,
                                    TelaClientes painelClientes, TelaVoos painelVoos,
                                    TelaAssociacoes painelAssociacoes, Runnable encerrar) {
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

        JLabel lblSubtitulo = new JLabel("Gerencie clientes, voos e check-in em um só lugar");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitulo.setForeground(COR_MUTED);
        lblSubtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        painelCabecalho.add(Box.createVerticalStrut(8));
        painelCabecalho.add(lblTitulo);
        painelCabecalho.add(Box.createVerticalStrut(6));
        painelCabecalho.add(lblSubtitulo);

        JPanel painelBotoes = new JPanel(new GridLayout(4, 1, 0, 14));
        painelBotoes.setOpaque(false);
        painelBotoes.setBorder(new EmptyBorder(20, 40, 0, 40));

        JButton btnGerenciarClientes = criarBotaoPrimario("Gerenciar Clientes", COR_PRIMARIA);
        JButton btnGerenciarVoos = criarBotaoPrimario("Gerenciar Voos", COR_SECUNDARIA);
        JButton btnAssociacoes = criarBotaoPrimario("Check-in", Tema.SUCESSO);
        JButton btnSair = criarBotaoSecundario("Sair");

        btnGerenciarClientes.addActionListener(e -> {
            painelClientes.atualizarTela();
            cardLayout.show(cards, "clientes");
        });
        btnGerenciarVoos.addActionListener(e -> {
            painelVoos.atualizarTela();
            cardLayout.show(cards, "voos");
        });
        btnAssociacoes.addActionListener(e -> {
            painelAssociacoes.atualizarTela();
            cardLayout.show(cards, "associacoes");
        });
        btnSair.addActionListener(e -> encerrar.run());

        painelBotoes.add(btnGerenciarClientes);
        painelBotoes.add(btnGerenciarVoos);
        painelBotoes.add(btnAssociacoes);
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
        botao.setBackground(Tema.VOLTAR);
        botao.setForeground(Tema.TEXTO_CAMPO);
        botao.setContentAreaFilled(true);
        botao.setFocusPainted(false);
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botao.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        botao.setPreferredSize(new Dimension(260, 52));
        return botao;
    }
}
