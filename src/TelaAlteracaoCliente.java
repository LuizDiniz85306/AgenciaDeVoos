import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.regex.Pattern;

public class TelaAlteracaoCliente extends JDialog {

    private static final Color COR_PAINEL = Color.BLACK;
    private static final Color COR_TEXTO = new Color(241, 245, 249);
    private static final Color COR_BORDA = new Color(71, 85, 105);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
        private static final Pattern NOME_PATTERN = Pattern.compile("[\\p{L}]+(?:[ .'-][\\p{L}]+)*");

    private final ClienteDAO clienteDAO;
    private final Cliente clienteAtual;

    private final JTextField txtId;
    private final JTextField txtNome;
    private final JTextField txtCpf;
    private final JTextField txtTelefone;
    private final JTextField txtEmail;

    public TelaAlteracaoCliente(Frame owner, ClienteDAO clienteDAO, Cliente clienteAtual) {
        super(owner, "Alterar Cliente", true);
        this.clienteDAO = clienteDAO;
        this.clienteAtual = clienteAtual;

        setSize(650, 500);
        setMinimumSize(new Dimension(600, 460));
        setLocationRelativeTo(owner);
        setResizable(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel painel = new JPanel(new BorderLayout(16, 16));
        painel.setBackground(Color.BLACK);
        painel.setBorder(new EmptyBorder(18, 18, 18, 18));
        setContentPane(painel);

        JLabel lblTitulo = new JLabel("Alteração de Cliente");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitulo.setForeground(COR_TEXTO);
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        painel.add(lblTitulo, BorderLayout.NORTH);

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(COR_PAINEL);
        panelForm.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(COR_BORDA, 1),
                " Dados do Cliente ",
                0,
                0,
                new Font("Segoe UI", Font.BOLD, 14),
                COR_TEXTO
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panelForm.add(criarRotulo("ID:"), gbc);

        txtId = new JTextField();
        configurarCampo(txtId);
        txtId.setEditable(false);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
        panelForm.add(txtId, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panelForm.add(criarRotulo("Nome Completo:"), gbc);

        txtNome = new JTextField();
        configurarCampo(txtNome);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
        panelForm.add(txtNome, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panelForm.add(criarRotulo("CPF:"), gbc);

        txtCpf = new JTextField();
        configurarCampo(txtCpf);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1;
        panelForm.add(txtCpf, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        panelForm.add(criarRotulo("Telefone:"), gbc);

        txtTelefone = new JTextField();
        configurarCampo(txtTelefone);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1;
        panelForm.add(txtTelefone, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        panelForm.add(criarRotulo("E-mail:"), gbc);

        txtEmail = new JTextField();
        configurarCampo(txtEmail);
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1;
        panelForm.add(txtEmail, gbc);

        painel.add(panelForm, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        botoes.setOpaque(false);

        JButton btnSalvar = criarBotao("Salvar Alteração", new Color(30, 64, 175));
        JButton btnCancelar = criarBotao("Cancelar", new Color(148, 163, 184));

        btnSalvar.addActionListener(e -> salvarAlteracao());
        btnCancelar.addActionListener(e -> dispose());

        botoes.add(btnSalvar);
        botoes.add(btnCancelar);
        painel.add(botoes, BorderLayout.SOUTH);

        carregarDadosCliente();
    }

    private void carregarDadosCliente() {
        txtId.setText(String.valueOf(clienteAtual.getId()));
        txtNome.setText(clienteAtual.getNome());
        txtCpf.setText(clienteAtual.getCpf());
        txtTelefone.setText(clienteAtual.getTelefone());
        txtEmail.setText(clienteAtual.getEmail());
    }

    private void salvarAlteracao() {
        if (!validarCampos()) {
            return;
        }

        try {
            int id = clienteAtual.getId();
            Cliente clienteAtualizado = new Cliente(
                    id,
                    txtNome.getText().trim(),
                    txtCpf.getText().trim(),
                    txtTelefone.getText().trim(),
                    txtEmail.getText().trim()
            );

            if (clienteDAO.alterarCliente(clienteAtualizado)) {
                JOptionPane.showMessageDialog(this, "Dados do cliente alterados com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Não foi possível alterar o cliente.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao alterar cliente: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validarCampos() {
        String nome = txtNome.getText().trim();
        String cpf = txtCpf.getText().trim();
        String telefone = txtTelefone.getText().trim();
        String email = txtEmail.getText().trim();

        if (nome.length() < 3 || nome.length() > 80 || !NOME_PATTERN.matcher(nome).matches()) {
            JOptionPane.showMessageDialog(this, "Nome inválido. Use de 3 a 80 letras, espaços ou hífen.", "Validação", JOptionPane.WARNING_MESSAGE);
            txtNome.requestFocus();
            return false;
        }

        if (!cpf.matches("\\d{11}") || !validarCPF(cpf)) {
            JOptionPane.showMessageDialog(this, "CPF inválido. Informe exatamente 11 números, sem pontuação.", "Validação", JOptionPane.WARNING_MESSAGE);
            txtCpf.requestFocus();
            return false;
        }

        try {
            for (Cliente cliente : clienteDAO.listarClientes()) {
                if (cliente.getId() != clienteAtual.getId() && cliente.getCpf().equals(cpf)) {
                    JOptionPane.showMessageDialog(this, "Já existe outro cliente cadastrado com esse CPF.", "Validação", JOptionPane.WARNING_MESSAGE);
                    txtCpf.requestFocus();
                    return false;
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Não foi possível validar o CPF: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!telefone.matches("[1-9]{2}\\d{8,9}")) {
            JOptionPane.showMessageDialog(this, "Telefone inválido. Informe somente DDD + número (10 ou 11 dígitos).", "Validação", JOptionPane.WARNING_MESSAGE);
            txtTelefone.requestFocus();
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            JOptionPane.showMessageDialog(this, "E-mail inválido. Use o formato usuario@dominio.com", "Validação", JOptionPane.WARNING_MESSAGE);
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

    private JButton criarBotao(String texto, Color cor) {
        JButton botao = new JButton(texto);
        botao.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        botao.setOpaque(true);
        botao.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botao.setBackground(cor);
        botao.setForeground(texto.equals("Cancelar") ? new Color(15, 23, 42) : Color.WHITE);
        botao.setContentAreaFilled(true);
        botao.setFocusPainted(false);
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botao.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        return botao;
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
}
