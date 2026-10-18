import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TelaAssociacoes extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color FUNDO = Tema.FUNDO;
    private static final Color TEXTO = Tema.TEXTO;
    private static final Color AZUL = Tema.CABECALHO;
    private static final Color BORDA = Tema.BORDA;
    private static final Color LINHA = Tema.PAINEL;
    private static final Color ALTERNADA = Tema.LINHA_ALTERNADA;
    private final AgenciaService service;
    private final JComboBox<ClienteOpcao> comboCliente = new JComboBox<>();
    private final JComboBox<VooOpcao> comboVoo = new JComboBox<>();
    private final DefaultTableModel modeloVoos = criarModelo(
            "ID", "Código", "Origem", "Destino", "Data", "Horário", "Status");
    private final DefaultTableModel modeloClientes = criarModelo("ID", "Nome", "CPF", "Telefone", "E-mail");
    private final JTable tabelaVoos = criarTabela(modeloVoos);
    private final JTable tabelaClientes = criarTabela(modeloClientes);
    private final JLabel tituloVoos = criarRotulo("Voos do passageiro — selecione um passageiro");
    private final JLabel tituloClientes = criarRotulo("Passageiros do voo — selecione um voo");
    private final JLabel status = criarRotulo("Selecione o passageiro e o voo para fazer ou cancelar o check-in.");
    private final JButton btnAssociar = criarBotao("Fazer check-in", Tema.SUCESSO);
    private final JButton btnRemover = criarBotao("Cancelar check-in", Tema.PERIGO);
    private final JButton btnVoosCliente = criarBotao("Voos do passageiro", Tema.PRIMARIA);
    private final JButton btnClientesVoo = criarBotao("Passageiros do voo", Tema.PRIMARIA);
    private boolean atualizando;

    public TelaAssociacoes(Runnable voltar, AgenciaService service) {
        this.service = service;
        setLayout(new BorderLayout(12, 12));
        setBackground(FUNDO);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel superior = new JPanel(new BorderLayout(8, 10));
        superior.setOpaque(false);
        JLabel titulo = criarRotulo("Check-in");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        superior.add(titulo, BorderLayout.NORTH);

        configurarCombo(comboCliente);
        configurarCombo(comboVoo);
        comboCliente.setName("comboCliente");
        comboVoo.setName("comboVoo");
        JPanel seletores = new JPanel(new GridBagLayout());
        seletores.setBackground(Tema.PAINEL);
        seletores.setBorder(new EmptyBorder(6, 8, 6, 8));
        adicionarSeletor(seletores, 0, "Passageiro:", comboCliente);
        adicionarSeletor(seletores, 1, "Voo:", comboVoo);
        superior.add(seletores, BorderLayout.CENTER);

        JPanel acoes = new JPanel(new GridLayout(1, 4, 8, 0));
        acoes.setOpaque(false);
        acoes.add(btnAssociar);
        acoes.add(btnRemover);
        acoes.add(btnVoosCliente);
        acoes.add(btnClientesVoo);
        superior.add(acoes, BorderLayout.SOUTH);
        add(superior, BorderLayout.NORTH);

        tabelaVoos.setName("tabelaVoos");
        tabelaClientes.setName("tabelaClientes");
        tabelaVoos.getColumnModel().getColumn(0).setPreferredWidth(50);
        tabelaClientes.getColumnModel().getColumn(0).setPreferredWidth(50);
        tabelaClientes.getColumnModel().getColumn(1).setPreferredWidth(220);
        tabelaClientes.getColumnModel().getColumn(4).setPreferredWidth(240);
        JSplitPane resultados = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                painelTabela(tituloVoos, tabelaVoos), painelTabela(tituloClientes, tabelaClientes));
        resultados.setResizeWeight(0.5);
        resultados.setDividerLocation(160);
        resultados.setDividerSize(8);
        resultados.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                javax.swing.plaf.basic.BasicSplitPaneDivider divisor =
                        new javax.swing.plaf.basic.BasicSplitPaneDivider(this);
                divisor.setBackground(FUNDO);
                divisor.setBorder(BorderFactory.createEmptyBorder());
                return divisor;
            }
        });
        resultados.setBorder(BorderFactory.createEmptyBorder());
        resultados.setBackground(FUNDO);
        add(resultados, BorderLayout.CENTER);

        JPanel inferior = new JPanel(new BorderLayout(8, 10));
        inferior.setOpaque(false);
        status.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        inferior.add(status, BorderLayout.NORTH);
        JLabel regra = criarRotulo("Para excluir um passageiro ou voo, cancele os check-ins correspondentes.");
        regra.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        regra.setForeground(Tema.TEXTO_SECUNDARIO);
        inferior.add(regra, BorderLayout.CENTER);
        JPanel navegacao = new JPanel(new GridLayout(1, 2, 8, 0));
        navegacao.setOpaque(false);
        JButton atualizar = criarBotao("Atualizar", Tema.NEUTRA);
        JButton voltarBotao = criarBotao("Voltar", Tema.VOLTAR);
        voltarBotao.setForeground(Tema.TEXTO_CAMPO);
        navegacao.add(atualizar);
        navegacao.add(voltarBotao);
        inferior.add(navegacao, BorderLayout.SOUTH);
        add(inferior, BorderLayout.SOUTH);

        comboCliente.addActionListener(e -> {
            if (!atualizando) executarConsulta(this::consultarVoos);
            atualizarBotoes();
        });
        comboVoo.addActionListener(e -> {
            if (!atualizando) executarConsulta(this::consultarClientes);
            atualizarBotoes();
        });
        btnVoosCliente.addActionListener(e -> executarConsulta(this::consultarVoos));
        btnClientesVoo.addActionListener(e -> executarConsulta(this::consultarClientes));
        btnAssociar.addActionListener(e -> alterarVinculo(true));
        btnRemover.addActionListener(e -> alterarVinculo(false));
        atualizar.addActionListener(e -> atualizarTela());
        voltarBotao.addActionListener(e -> {
            if (voltar != null) voltar.run();
        });
        carregarOpcoes();
    }

    private void adicionarSeletor(JPanel painel, int linha, String texto, JComboBox<?> combo) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = linha;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        JLabel rotulo = criarRotulo(texto);
        rotulo.setLabelFor(combo);
        painel.add(rotulo, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painel.add(combo, gbc);
    }

    private void alterarVinculo(boolean incluir) {
        ClienteOpcao cliente = (ClienteOpcao) comboCliente.getSelectedItem();
        VooOpcao voo = (VooOpcao) comboVoo.getSelectedItem();
        if (cliente == null || voo == null || cliente.id <= 0 || voo.id <= 0) {
            mostrarErro("Selecione um passageiro e um voo cadastrados.");
            return;
        }
        try {
            boolean alterou = incluir
                    ? service.associarClienteAoVoo(cliente.id, voo.id)
                    : service.desassociarClienteDoVoo(cliente.id, voo.id);
            consultarVoos();
            consultarClientes();
            String mensagem = alterou
                    ? (incluir ? "Check-in realizado" : "Check-in cancelado")
                    : (incluir ? "Este passageiro já está neste voo" : "Não há check-in para este passageiro neste voo");
            status.setText(mensagem + ".");
            status.setForeground(alterou ? new Color(153, 246, 228) : new Color(253, 230, 138));
        } catch (RuntimeException e) {
            mostrarErro(e.getMessage());
        }
    }

    private void consultarVoos() {
        modeloVoos.setRowCount(0);
        ClienteOpcao cliente = (ClienteOpcao) comboCliente.getSelectedItem();
        if (cliente == null || cliente.id <= 0) {
            tituloVoos.setText("Voos do passageiro — selecione um passageiro");
            return;
        }
        List<Voo> voos = service.listarVoosDoCliente(cliente.id);
        for (Voo voo : voos) {
            modeloVoos.addRow(new Object[]{voo.getId(), voo.getCodigo(), voo.getOrigem(), voo.getDestino(),
                    voo.getData(), voo.getHorario(), voo.getStatus()});
        }
        tituloVoos.setText("Voos de " + cliente.nome + " — " + voos.size() + " voo(s)");
        tituloVoos.setToolTipText(tituloVoos.getText());
    }

    private void consultarClientes() {
        modeloClientes.setRowCount(0);
        VooOpcao voo = (VooOpcao) comboVoo.getSelectedItem();
        if (voo == null || voo.id <= 0) {
            tituloClientes.setText("Passageiros do voo — selecione um voo");
            return;
        }
        List<Cliente> clientes = service.listarClientesDoVoo(voo.id);
        for (Cliente cliente : clientes) {
            modeloClientes.addRow(new Object[]{cliente.getId(), cliente.getNome(), cliente.getCpf(),
                    cliente.getTelefone(), cliente.getEmail()});
        }
        String codigo = voo.descricao.split(" \\| ", 2)[0];
        tituloClientes.setText("Passageiros do voo " + codigo + " — " + clientes.size() + " passageiro(s)");
    }

    private void executarConsulta(Runnable consulta) {
        try {
            consulta.run();
        } catch (RuntimeException e) {
            mostrarErro(e.getMessage());
        }
    }

    public void atualizarTela() {
        try {
            carregarOpcoes();
            status.setText("Listas atualizadas. Selecione o passageiro e o voo desejados.");
            status.setForeground(TEXTO);
        } catch (RuntimeException e) {
            mostrarErro(e.getMessage());
        }
    }

    private void carregarOpcoes() {
        ClienteOpcao anteriorCliente = (ClienteOpcao) comboCliente.getSelectedItem();
        VooOpcao anteriorVoo = (VooOpcao) comboVoo.getSelectedItem();
        List<Cliente> clientes = service.listarClientes();
        List<Voo> voos = service.listarVoos();
        atualizando = true;
        try {
            comboCliente.removeAllItems();
            comboCliente.addItem(new ClienteOpcao(-1, "Selecione um passageiro..."));
            for (Cliente cliente : clientes) {
                ClienteOpcao opcao = new ClienteOpcao(cliente.getId(), cliente.getNome());
                comboCliente.addItem(opcao);
                if (anteriorCliente != null && anteriorCliente.id == opcao.id) comboCliente.setSelectedItem(opcao);
            }
            comboVoo.removeAllItems();
            comboVoo.addItem(new VooOpcao(-1, "Selecione um voo..."));
            for (Voo voo : voos) {
                VooOpcao opcao = new VooOpcao(voo.getId(), voo.getCodigo() + " | " + voo.getOrigem()
                        + " → " + voo.getDestino() + " | " + voo.getData() + " " + voo.getHorario());
                comboVoo.addItem(opcao);
                if (anteriorVoo != null && anteriorVoo.id == opcao.id) comboVoo.setSelectedItem(opcao);
            }
        } finally {
            atualizando = false;
        }
        consultarVoos();
        consultarClientes();
        atualizarBotoes();
    }

    private void atualizarBotoes() {
        boolean cliente = comboCliente.getSelectedIndex() > 0;
        boolean voo = comboVoo.getSelectedIndex() > 0;
        btnAssociar.setEnabled(cliente && voo);
        btnRemover.setEnabled(cliente && voo);
        btnVoosCliente.setEnabled(cliente);
        btnClientesVoo.setEnabled(voo);
    }

    private void mostrarErro(String mensagem) {
        status.setText("Não foi possível concluir a operação. Confira os dados selecionados.");
        status.setForeground(new Color(252, 165, 165));
        JOptionPane.showMessageDialog(this, mensagem, "Check-in", JOptionPane.ERROR_MESSAGE);
    }

    private static JPanel painelTabela(JLabel titulo, JTable tabela) {
        JPanel painel = new JPanel(new BorderLayout(4, 6));
        painel.setBackground(Tema.PAINEL);
        painel.setBorder(new EmptyBorder(8, 10, 10, 10));
        painel.add(titulo, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setColumnHeaderView(tabela.getTableHeader());
        scroll.getColumnHeader().setBackground(AZUL);
        scroll.getViewport().setBackground(LINHA);
        scroll.setBackground(Tema.PAINEL);
        JPanel canto = new JPanel();
        canto.setBackground(AZUL);
        scroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, canto);
        scroll.setBorder(BorderFactory.createLineBorder(BORDA));
        scroll.setMinimumSize(new Dimension(300, 100));
        painel.setMinimumSize(new Dimension(320, 130));
        painel.add(scroll, BorderLayout.CENTER);
        return painel;
    }

    private static DefaultTableModel criarModelo(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
    }

    private static JTable criarTabela(DefaultTableModel modelo) {
        JTable tabela = new JTable(modelo);
        tabela.setRowHeight(28);
        tabela.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabela.setBackground(LINHA);
        tabela.setForeground(TEXTO);
        tabela.setSelectionBackground(Tema.SELECAO);
        tabela.setSelectionForeground(Color.WHITE);
        tabela.setGridColor(BORDA);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabela.setFillsViewportHeight(true);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getTableHeader().setOpaque(true);
        tabela.getTableHeader().setBackground(AZUL);
        tabela.getTableHeader().setForeground(Color.WHITE);
        tabela.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabela.getTableHeader().setPreferredSize(new Dimension(0, 32));
        tabela.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable alvo, Object valor, boolean selecionado,
                                                           boolean foco, int linha, int coluna) {
                super.getTableCellRendererComponent(alvo, valor, selecionado, foco, linha, coluna);
                setOpaque(true);
                setBackground(AZUL);
                setForeground(Color.WHITE);
                setFont(alvo.getTableHeader().getFont());
                setBorder(new EmptyBorder(6, 8, 6, 8));
                return this;
            }
        });
        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable alvo, Object valor, boolean selecionado,
                                                           boolean foco, int linha, int coluna) {
                super.getTableCellRendererComponent(alvo, valor, selecionado, foco, linha, coluna);
                setOpaque(true);
                setBackground(selecionado ? Tema.SELECAO : (linha % 2 == 0 ? LINHA : ALTERNADA));
                setForeground(TEXTO);
                setToolTipText(valor == null ? null : String.valueOf(valor));
                setBorder(foco ? BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Tema.FOCO), new EmptyBorder(3, 7, 3, 7))
                        : new EmptyBorder(4, 8, 4, 8));
                return this;
            }
        });
        for (int i = 0; i < modelo.getColumnCount(); i++)
            tabela.getColumnModel().getColumn(i).setPreferredWidth(135);
        return tabela;
    }

    private static JLabel criarRotulo(String texto) {
        JLabel rotulo = new JLabel(texto);
        rotulo.setForeground(TEXTO);
        rotulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return rotulo;
    }

    private static JButton criarBotao(String texto, Color cor) {
        JButton botao = new JButton(texto);
        botao.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            protected void paintText(Graphics g, AbstractButton b, Rectangle area, String texto) {
                if (b.isEnabled()) {
                    super.paintText(g, b, area, texto);
                } else {
                    g.setColor(Tema.TEXTO_SECUNDARIO);
                    javax.swing.plaf.basic.BasicGraphicsUtils.drawStringUnderlineCharAt(
                            g, texto, b.getDisplayedMnemonicIndex(), area.x,
                            area.y + g.getFontMetrics().getAscent());
                }
            }
        });
        botao.setOpaque(true);
        botao.setContentAreaFilled(true);
        botao.setBackground(cor);
        botao.setForeground(Color.WHITE);
        botao.setFont(new Font("Segoe UI", Font.BOLD, 13));
        botao.setBorder(new EmptyBorder(10, 10, 10, 10));
        botao.setFocusPainted(true);
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botao.addPropertyChangeListener("enabled", e -> {
            botao.setBackground(botao.isEnabled() ? cor : Tema.NEUTRA);
            botao.setForeground(botao.isEnabled() ? (cor.equals(Tema.VOLTAR) ? Tema.TEXTO_CAMPO : Color.WHITE)
                    : Tema.TEXTO_SECUNDARIO);
        });
        botao.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                botao.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Tema.FOCO), new EmptyBorder(9, 9, 9, 9)));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                botao.setBorder(new EmptyBorder(10, 10, 10, 10));
            }
        });
        return botao;
    }

    private static void configurarCombo(JComboBox<?> combo) {
        combo.setBackground(Tema.CAMPO);
        combo.setForeground(Tema.TEXTO_CAMPO);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setPreferredSize(new Dimension(300, 30));
        combo.setMaximumRowCount(12);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                           boolean selecionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, selecionado, foco);
                setOpaque(true);
                setForeground(selecionado ? Color.WHITE : Tema.TEXTO_CAMPO);
                setBackground(selecionado ? Tema.SELECAO : Tema.CAMPO);
                setBorder(new EmptyBorder(4, 8, 4, 8));
                return this;
            }
        });
    }

    private static final class ClienteOpcao {
        private final int id;
        private final String nome;

        private ClienteOpcao(int id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        @Override
        public String toString() {
            return nome;
        }
    }

    private static final class VooOpcao {
        private final int id;
        private final String descricao;

        private VooOpcao(int id, String descricao) {
            this.id = id;
            this.descricao = descricao;
        }

        @Override
        public String toString() {
            return descricao;
        }
    }
}
