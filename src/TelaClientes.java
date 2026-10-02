import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.regex.Pattern;

public class TelaClientes extends JPanel {

    private static final Color COR_FUNDO = Color.BLACK;
    private static final Color COR_PAINEL = Color.BLACK;
    private static final Color COR_TEXTO = new Color(241, 245, 249);
    private static final Color COR_BORDA = new Color(71, 85, 105);
    private static final Color COR_TABELA = new Color(15, 23, 42);
    private static final Color COR_LINHA_ALTERNADA = new Color(30, 41, 59);
    private static final Color COR_CABECALHO = new Color(30, 64, 175);
    private static final Color COR_SELECAO = new Color(29, 78, 216);

    private final Runnable voltarAction;
    private final AgenciaService agenciaService;
    private ClienteDAO clienteDAO;

    private JTextField txtId;
    private JTextField txtNome;
    private JTextField txtCpf;
    private JTextField txtTelefone;
    private JTextField txtEmail;
    private JTextArea txtVoosCliente;
    private JTable tabelaClientes;
    private DefaultTableModel modeloClientes;

    private JButton btnBuscar;
    private JButton btnCadastrar;
    private JButton btnAlterar;
    private JButton btnExcluir;
    private JButton btnLimpar;
    private JButton btnVoltar;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    private static final Pattern NOME_PATTERN = Pattern.compile("[\\p{L}]+(?:[ .'-][\\p{L}]+)*");

    public TelaClientes(Runnable voltarAction) {
        this(voltarAction, new AgenciaService());
    }

    public TelaClientes(Runnable voltarAction, AgenciaService agenciaService) {
        this.voltarAction = voltarAction;
        this.agenciaService = agenciaService;

        try {
            clienteDAO = new ClienteDAO();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar banco de dados: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }

        setBackground(COR_FUNDO);
        setBorder(new EmptyBorder(18, 18, 18, 18));
        setLayout(new BorderLayout(15, 15));

        JLabel lblTitulo = new JLabel("Gerenciamento de Clientes");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitulo.setForeground(COR_TEXTO);
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblTitulo, BorderLayout.NORTH);

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(COR_PAINEL);
        panelForm.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(COR_BORDA, 1),
                " Cadastro do Cliente ",
                0,
                0,
                new Font("Segoe UI", Font.BOLD, 14),
                COR_TEXTO
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 8, 7, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        panelForm.add(criarRotulo("ID:"), gbc);

        txtId = new JTextField();
        configurarCampo(txtId);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        panelForm.add(txtId, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        panelForm.add(criarRotulo("Nome Completo: *"), gbc);

        txtNome = new JTextField();
        configurarCampo(txtNome);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        panelForm.add(txtNome, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        panelForm.add(criarRotulo("CPF (11 dígitos): *"), gbc);

        txtCpf = new JTextField();
        configurarCampo(txtCpf);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        panelForm.add(txtCpf, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        panelForm.add(criarRotulo("Telefone (DDD + número): *"), gbc);

        txtTelefone = new JTextField();
        configurarCampo(txtTelefone);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        panelForm.add(txtTelefone, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
        panelForm.add(criarRotulo("E-mail: *"), gbc);

        txtEmail = new JTextField();
        configurarCampo(txtEmail);
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0;
        panelForm.add(txtEmail, gbc);

        JPanel painelConteudo = new JPanel(new BorderLayout(16, 0));
        painelConteudo.setOpaque(false);
        painelConteudo.add(panelForm, BorderLayout.CENTER);

        txtVoosCliente = new JTextArea();
        txtVoosCliente.setEditable(false);
        txtVoosCliente.setLineWrap(true);
        txtVoosCliente.setWrapStyleWord(true);
        txtVoosCliente.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtVoosCliente.setBackground(Color.BLACK);
        txtVoosCliente.setForeground(COR_TEXTO);
        txtVoosCliente.setText("Informe o ID do cliente para consultar os voos.");
        txtVoosCliente.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(COR_BORDA, 1),
            " Voos do Cliente ", 0, 0,
            new Font("Segoe UI", Font.BOLD, 14), COR_TEXTO));
        JScrollPane scrollVoos = new JScrollPane(txtVoosCliente);
        scrollVoos.setPreferredSize(new Dimension(350, 300));
        scrollVoos.getViewport().setBackground(Color.BLACK);
        painelConteudo.add(scrollVoos, BorderLayout.EAST);
        criarTabelaClientes();

        JPanel painelCentral = new JPanel(new BorderLayout(12, 12));
        painelCentral.setOpaque(false);
        painelCentral.add(painelConteudo, BorderLayout.CENTER);
        painelCentral.add(criarPainelTabelaClientes(), BorderLayout.SOUTH);
        add(painelCentral, BorderLayout.CENTER);

        JPanel panelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panelBotoes.setOpaque(false);

        btnBuscar = criarBotao("Buscar", new Color(59, 130, 246));
        btnCadastrar = criarBotao("Cadastrar", new Color(34, 197, 94));
        btnAlterar = criarBotao("Alterar", new Color(245, 158, 11));
        btnExcluir = criarBotao("Excluir", new Color(239, 68, 68));
        btnLimpar = criarBotao("Limpar", new Color(107, 114, 128));
        btnVoltar = criarBotao("Voltar", new Color(148, 163, 184));
        JButton btnBuscarVoos = criarBotao("Buscar voos", new Color(22, 163, 74));

        panelBotoes.add(btnBuscar);
        panelBotoes.add(btnCadastrar);
        panelBotoes.add(btnAlterar);
        panelBotoes.add(btnExcluir);
        panelBotoes.add(btnLimpar);
        panelBotoes.add(btnVoltar);
        panelBotoes.add(btnBuscarVoos);

        add(panelBotoes, BorderLayout.SOUTH);

        btnBuscar.addActionListener(e -> buscarCliente());
        btnBuscarVoos.addActionListener(e -> buscarVoosCliente());
        btnCadastrar.addActionListener(e -> cadastrarCliente());
        btnAlterar.addActionListener(e -> alterarCliente());
        btnExcluir.addActionListener(e -> excluirCliente());
        btnLimpar.addActionListener(e -> limparCampos());
        btnVoltar.addActionListener(e -> {
            if (voltarAction != null) {
                voltarAction.run();
            }
        });

        carregarClientesCadastrados();
    }

    private void criarTabelaClientes() {
        modeloClientes = new DefaultTableModel(
                new String[]{"ID", "Nome", "CPF", "Telefone", "E-mail"}, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
        tabelaClientes = new JTable(modeloClientes);
        tabelaClientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaClientes.setRowHeight(28);
        tabelaClientes.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabelaClientes.setBackground(COR_TABELA);
        tabelaClientes.setForeground(COR_TEXTO);
        tabelaClientes.setGridColor(COR_BORDA);
        tabelaClientes.setSelectionBackground(COR_SELECAO);
        tabelaClientes.setSelectionForeground(Color.WHITE);
        tabelaClientes.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tabela, Object valor,
                    boolean selecionado, boolean foco, int linha, int coluna) {
                super.getTableCellRendererComponent(tabela, valor, selecionado, foco, linha, coluna);
                setOpaque(true);
                setBackground(selecionado ? COR_SELECAO
                        : linha % 2 == 0 ? COR_TABELA : COR_LINHA_ALTERNADA);
                setForeground(selecionado ? Color.WHITE : COR_TEXTO);
                setBorder(foco ? BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(147, 197, 253)),
                        BorderFactory.createEmptyBorder(3, 7, 3, 7))
                        : BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return this;
            }
        });
        DefaultTableCellRenderer cabecalho = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tabela, Object valor,
                    boolean selecionado, boolean foco, int linha, int coluna) {
                super.getTableCellRendererComponent(tabela, valor, selecionado, foco, linha, coluna);
                setOpaque(true);
                setBackground(COR_CABECALHO);
                setForeground(Color.WHITE);
                setFont(tabela.getTableHeader().getFont());
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, COR_BORDA),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
                return this;
            }
        };
        tabelaClientes.getTableHeader().setDefaultRenderer(cabecalho);
        tabelaClientes.getTableHeader().setBackground(COR_CABECALHO);
        tabelaClientes.getTableHeader().setForeground(Color.WHITE);
        tabelaClientes.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabelaClientes.getTableHeader().setPreferredSize(new Dimension(0, 32));
        tabelaClientes.getColumnModel().getColumn(0).setPreferredWidth(45);
        tabelaClientes.getColumnModel().getColumn(1).setPreferredWidth(180);
        tabelaClientes.getColumnModel().getColumn(2).setPreferredWidth(105);
        tabelaClientes.getColumnModel().getColumn(3).setPreferredWidth(120);
        tabelaClientes.getColumnModel().getColumn(4).setPreferredWidth(210);

        tabelaClientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selecionarClienteDaTabela();
            }
        });
    }

    private JScrollPane criarPainelTabelaClientes() {
        JScrollPane scroll = new JScrollPane(tabelaClientes);
        scroll.setPreferredSize(new Dimension(0, 165));
        scroll.setOpaque(true);
        scroll.setBackground(COR_PAINEL);
        scroll.getViewport().setBackground(COR_TABELA);
        scroll.setColumnHeaderView(tabelaClientes.getTableHeader());
        scroll.getColumnHeader().setBackground(COR_CABECALHO);
        JPanel cantoTabela = new JPanel();
        cantoTabela.setBackground(COR_CABECALHO);
        scroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cantoTabela);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COR_BORDA, 1),
                " Clientes Cadastrados - selecione uma linha para consultar ",
                0, 0, new Font("Segoe UI", Font.BOLD, 14), COR_TEXTO));
        return scroll;
    }

    private JButton criarBotao(String texto, Color corFundo) {
        JButton btn = new JButton(texto);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btn.setOpaque(true);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(corFundo);
        btn.setForeground(texto.equals("Voltar") ? new Color(15, 23, 42) : Color.WHITE);
        btn.setContentAreaFilled(true);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        return btn;
    }

    private JLabel criarRotulo(String texto) {
        JLabel rotulo = new JLabel(texto);
        rotulo.setForeground(COR_TEXTO);
        return rotulo;
    }

    private void configurarCampo(JTextField campo) {
        campo.setBackground(Color.WHITE);
        campo.setForeground(new Color(15, 23, 42));
        campo.setCaretColor(new Color(15, 23, 42));
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    private void carregarClientesCadastrados() {
        if (clienteDAO == null || modeloClientes == null) {
            return;
        }
        try {
            modeloClientes.setRowCount(0);
            for (Cliente cliente : clienteDAO.listarClientes()) {
                modeloClientes.addRow(new Object[]{
                        cliente.getId(),
                        cliente.getNome(),
                        cliente.getCpf(),
                        cliente.getTelefone(),
                        cliente.getEmail()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Não foi possível carregar os clientes cadastrados: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selecionarClienteDaTabela() {
        if (tabelaClientes == null || clienteDAO == null) {
            return;
        }
        int linha = tabelaClientes.getSelectedRow();
        if (linha < 0) {
            return;
        }
        try {
            int id = Integer.parseInt(modeloClientes.getValueAt(linha, 0).toString());
            Cliente cliente = clienteDAO.buscarCliente(id);
            if (cliente != null) {
                preencherDadosCliente(cliente);
                atualizarVoosCliente(id);
            }
        } catch (Exception e) {
            mostrarAlerta("Não foi possível carregar o cliente selecionado: " + e.getMessage());
        }
    }

    private void preencherDadosCliente(Cliente cliente) {
        txtId.setText(String.valueOf(cliente.getId()));
        txtNome.setText(cliente.getNome());
        txtCpf.setText(cliente.getCpf());
        txtTelefone.setText(cliente.getTelefone());
        txtEmail.setText(cliente.getEmail());
    }

    private boolean validarCampos() {
        String nome = txtNome.getText().trim();
        String cpf = txtCpf.getText().trim();
        String telefone = txtTelefone.getText().trim();
        String email = txtEmail.getText().trim();

        if (nome.length() < 3 || nome.length() > 80 || !NOME_PATTERN.matcher(nome).matches()) {
            mostrarAlerta("Nome inválido. Use de 3 a 80 letras, espaços ou hífen.");
            txtNome.requestFocus();
            return false;
        }

        if (!cpf.matches("\\d{11}") || !validarCPF(cpf)) {
            mostrarAlerta("CPF inválido. Informe exatamente 11 números, sem pontuação.");
            txtCpf.requestFocus();
            return false;
        }

        if (!telefone.matches("[1-9]{2}\\d{8,9}")) {
            mostrarAlerta("Telefone inválido. Informe somente DDD + número (10 ou 11 dígitos).");
            txtTelefone.requestFocus();
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            mostrarAlerta("Formato de E-mail inválido! Exemplo: usuario@dominio.com");
            txtEmail.requestFocus();
            return false;
        }

        return true;
    }

    private boolean validarCPF(String cpf) {
        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            int soma = 0, resto;
            for (int i = 1; i <= 9; i++) {
                soma += Integer.parseInt(cpf.substring(i - 1, i)) * (11 - i);
            }
            resto = (soma * 10) % 11;
            if (resto == 10 || resto == 11) resto = 0;
            if (resto != Integer.parseInt(cpf.substring(9, 10))) return false;

            soma = 0;
            for (int i = 1; i <= 10; i++) {
                soma += Integer.parseInt(cpf.substring(i - 1, i)) * (12 - i);
            }
            resto = (soma * 10) % 11;
            if (resto == 10 || resto == 11) resto = 0;
            return resto == Integer.parseInt(cpf.substring(10, 11));
        } catch (Exception e) {
            return false;
        }
    }

    private void mostrarAlerta(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Validação de Entrada", JOptionPane.WARNING_MESSAGE);
    }

    private void buscarCliente() {
        try {
            String strId = txtId.getText().trim();
            if (!strId.matches("\\d{1,9}")) {
                mostrarAlerta("Informe um ID inteiro positivo para realizar a busca.");
                txtId.requestFocus();
                return;
            }

            int id = Integer.parseInt(strId);
            if (id <= 0) {
                mostrarAlerta("O ID deve ser maior que zero.");
                return;
            }
            Cliente cliente = clienteDAO.buscarCliente(id);

            if (cliente != null) {
                preencherDadosCliente(cliente);
                atualizarVoosCliente(id);
            } else {
                txtVoosCliente.setText("Cliente não encontrado.");
                JOptionPane.showMessageDialog(this, "Cliente não encontrado.", "Resultado", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("O campo ID aceita apenas números inteiros.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao buscar cliente: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cadastrarCliente() {
        if (!validarCampos()) return;

        try {
            for (Cliente cadastrado : clienteDAO.listarClientes()) {
                if (cadastrado.getCpf().equals(txtCpf.getText().trim())) {
                    mostrarAlerta("Já existe um cliente cadastrado com esse CPF.");
                    txtCpf.requestFocus();
                    return;
                }
            }

            Cliente cliente = new Cliente(
                txtNome.getText().trim(),
                txtCpf.getText().trim(),
                txtTelefone.getText().trim(),
                txtEmail.getText().trim()
            );

            if (clienteDAO.incluirCliente(cliente)) {
                txtId.setText(String.valueOf(cliente.getId()));
                carregarClientesCadastrados();
                JOptionPane.showMessageDialog(this, "Cliente cadastrado com sucesso!\nID Gerado: " + cliente.getId(), "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Não foi possível realizar o cadastro.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao cadastrar: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void alterarCliente() {
        String strId = txtId.getText().trim();
        if (!strId.matches("\\d{1,9}")) {
            mostrarAlerta("Para alterar, informe um ID inteiro positivo.");
            return;
        }

        try {
            int id = Integer.parseInt(strId);
            if (id <= 0) {
                mostrarAlerta("O ID deve ser maior que zero.");
                return;
            }
            Cliente cliente = clienteDAO.buscarCliente(id);

            if (cliente == null) {
                JOptionPane.showMessageDialog(this, "Cliente não encontrado para alteração.", "Resultado", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            TelaAlteracaoCliente telaAlteracao = new TelaAlteracaoCliente((Frame) SwingUtilities.getWindowAncestor(this), clienteDAO, cliente);
            telaAlteracao.setVisible(true);

            Cliente clienteAtualizado = clienteDAO.buscarCliente(id);
            if (clienteAtualizado != null) {
                preencherDadosCliente(clienteAtualizado);
                carregarClientesCadastrados();
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("O ID deve ser um número inteiro válido.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao alterar: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void excluirCliente() {
        String strId = txtId.getText().trim();
        if (!strId.matches("\\d{1,9}")) {
            mostrarAlerta("Informe um ID inteiro positivo para excluir o cliente.");
            return;
        }

        try {
            int id = Integer.parseInt(strId);
            if (id <= 0) {
                mostrarAlerta("O ID deve ser maior que zero.");
                return;
            }

            if (agenciaService.possuiVoosDoCliente(id)) {
                mostrarAlerta("O cliente possui voos associados e não pode ser excluído. Reatribua ou exclua os voos antes.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Tem certeza de que deseja remover o cliente de ID " + id + "?",
                "Confirmar Exclusão",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                if (clienteDAO.excluirCliente(id)) {
                    JOptionPane.showMessageDialog(this, "Cliente removido com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    limparCampos();
                    carregarClientesCadastrados();
                } else {
                    JOptionPane.showMessageDialog(this, "Registro não encontrado para exclusão.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("O ID informado deve ser numérico.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao excluir: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limparCampos() {
        txtId.setText("");
        txtNome.setText("");
        txtCpf.setText("");
        txtTelefone.setText("");
        txtEmail.setText("");
        txtVoosCliente.setText("Informe o ID do cliente para consultar os voos.");
        txtNome.requestFocus();
    }

    private void buscarVoosCliente() {
        String idTexto = txtId.getText().trim();
        if (!idTexto.matches("\\d{1,9}") || Integer.parseInt(idTexto) <= 0) {
            mostrarAlerta("Informe o ID numérico do cliente para consultar os voos.");
            txtId.requestFocus();
            return;
        }
        int id = Integer.parseInt(idTexto);
        try {
            if (clienteDAO.buscarCliente(id) == null) {
                txtVoosCliente.setText("Cliente não encontrado.");
                mostrarAlerta("Cliente não encontrado.");
                return;
            }
            atualizarVoosCliente(id);
        } catch (Exception e) {
            mostrarAlerta("Erro ao consultar cliente: " + e.getMessage());
        }
    }

    public void atualizarVoosClienteAtual() {
        String idTexto = txtId.getText().trim();
        if (idTexto.matches("\\d{1,9}") && Integer.parseInt(idTexto) > 0) {
            atualizarVoosCliente(Integer.parseInt(idTexto));
        }
    }

    private void atualizarVoosCliente(int idCliente) {
        List<Voo> voos = agenciaService.listarVoosDoCliente(idCliente);
        if (voos.isEmpty()) {
            txtVoosCliente.setText("Nenhum voo associado a este cliente.");
            return;
        }
        StringBuilder resumo = new StringBuilder();
        for (Voo voo : voos) {
            resumo.append(voo).append("\n\n");
        }
        txtVoosCliente.setText(resumo.toString());
        txtVoosCliente.setCaretPosition(0);
    }
}
