import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.regex.Pattern;

public class TelaVoos extends JPanel {

    private static final Color COR_TEXTO = new Color(241, 245, 249);
    private static final Color COR_FUNDO = Color.BLACK;
    private static final Color COR_CAIXA = Color.BLACK;
    private static final Color COR_BORDA = new Color(71, 85, 105);
        private static final Pattern CODIGO_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{1,9}");
        private static final Pattern LOCAL_PATTERN = Pattern.compile("[\\p{L}][\\p{L} .'-]{1,59}");
        private static final Pattern VALOR_PATTERN = Pattern.compile("\\d{1,8}([,.]\\d{1,2})?");
        private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter
            .ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
            private static final DateTimeFormatter FORMATO_HORARIO = DateTimeFormatter
                .ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private final Runnable voltarAction;
    private final AgenciaService agenciaService;
    private final ClienteDAO clienteDAO;

    private final JTextField txtId;
    private final JTextField txtCodigo;
    private final JTextField txtOrigem;
    private final JTextField txtDestino;
    private final JTextField txtData;
    private final JTextField txtHorario;
    private final JTextField txtValor;
    private final JComboBox<String> cmbStatus;
    private final JComboBox<ClienteOpcao> cmbCliente;
    private final JTextArea txtResumo;

    public TelaVoos(Runnable voltarAction) {
        this(voltarAction, new AgenciaService());
    }

    public TelaVoos(Runnable voltarAction, AgenciaService agenciaService) {
        this.voltarAction = voltarAction;
        this.agenciaService = agenciaService;
        ClienteDAO dao;
        try {
            dao = new ClienteDAO();
        } catch (Exception e) {
            dao = null;
        }
        clienteDAO = dao;

        setBackground(COR_FUNDO);
        setBorder(new EmptyBorder(18, 18, 18, 18));
        setLayout(new BorderLayout(16, 16));

        JLabel lblTitulo = new JLabel("Gestão de Voos");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitulo.setForeground(COR_TEXTO);
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblTitulo, BorderLayout.NORTH);

        JPanel painelFormulario = new JPanel(new GridBagLayout());
        painelFormulario.setBackground(COR_CAIXA);
        painelFormulario.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(COR_BORDA, 1),
                " Cadastro de Voo ",
                0,
                0,
                new Font("Segoe UI", Font.BOLD, 14),
                COR_TEXTO
        ));

        GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblId = new JLabel("ID (PK):");
        lblId.setForeground(COR_TEXTO);
        JLabel lblCodigo = new JLabel("Código:");
        lblCodigo.setForeground(COR_TEXTO);
        JLabel lblOrigem = new JLabel("Origem:");
        lblOrigem.setForeground(COR_TEXTO);
        JLabel lblDestino = new JLabel("Destino:");
        lblDestino.setForeground(COR_TEXTO);
        JLabel lblData = new JLabel("Data (dd/MM/aaaa):");
        lblData.setForeground(COR_TEXTO);
        JLabel lblHorario = new JLabel("Horário (HH:mm):");
        lblHorario.setForeground(COR_TEXTO);
        JLabel lblValor = new JLabel("Valor (ex.: 250,00):");
        lblValor.setForeground(COR_TEXTO);
        JLabel lblStatus = new JLabel("Status:");
        lblStatus.setForeground(COR_TEXTO);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        painelFormulario.add(lblId, gbc);
        txtId = new JTextField();
        configurarCampo(txtId);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
        painelFormulario.add(txtId, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        painelFormulario.add(lblCodigo, gbc);
        txtCodigo = new JTextField();
        configurarCampo(txtCodigo);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
        painelFormulario.add(txtCodigo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        painelFormulario.add(lblOrigem, gbc);
        txtOrigem = new JTextField();
        configurarCampo(txtOrigem);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1;
        painelFormulario.add(txtOrigem, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        painelFormulario.add(lblDestino, gbc);
        txtDestino = new JTextField();
        configurarCampo(txtDestino);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1;
        painelFormulario.add(txtDestino, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        painelFormulario.add(lblData, gbc);
        txtData = new JTextField();
        configurarCampo(txtData);
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1;
        painelFormulario.add(txtData, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        painelFormulario.add(lblHorario, gbc);
        txtHorario = new JTextField();
        configurarCampo(txtHorario);
        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 1;
        painelFormulario.add(txtHorario, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0;
        painelFormulario.add(lblValor, gbc);
        txtValor = new JTextField();
        configurarCampo(txtValor);
        gbc.gridx = 1; gbc.gridy = 6; gbc.weightx = 1;
        painelFormulario.add(txtValor, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0;
        painelFormulario.add(lblStatus, gbc);
        cmbStatus = new JComboBox<>(new String[]{"Programado", "Confirmado", "Cancelado", "Embarque"});
        configurarComboBox(cmbStatus);
        gbc.gridx = 1; gbc.gridy = 7; gbc.weightx = 1;
        painelFormulario.add(cmbStatus, gbc);

        JLabel lblCliente = new JLabel("Cliente (FK):");
        lblCliente.setForeground(COR_TEXTO);
        gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0;
        painelFormulario.add(lblCliente, gbc);
        cmbCliente = new JComboBox<>();
        configurarComboBox(cmbCliente);
        gbc.gridx = 1; gbc.gridy = 8; gbc.weightx = 1;
        painelFormulario.add(cmbCliente, gbc);

        JPanel painelBotoes = new JPanel(new GridLayout(3, 3, 8, 6));
        painelBotoes.setOpaque(false);
        painelBotoes.setBorder(new EmptyBorder(0, 0, 0, 0));

        JButton btnSalvar = criarBotao("Salvar", new Color(30, 64, 175));
        JButton btnBuscar = criarBotao("Buscar", new Color(59, 130, 246));
        JButton btnExcluir = criarBotao("Excluir", new Color(239, 68, 68));
        JButton btnLimpar = criarBotao("Limpar", new Color(100, 116, 139));
        JButton btnAssociar = criarBotao("Associar cliente", new Color(22, 163, 74));
        JButton btnOrdenar = criarBotao("Ordenar externamente", new Color(124, 58, 237));
        JButton btnVoltar = criarBotao("Voltar", new Color(226, 232, 240));

        btnSalvar.addActionListener(e -> salvarVoo());
        btnBuscar.addActionListener(e -> buscarVoo());
        btnExcluir.addActionListener(e -> excluirVoo());
        btnLimpar.addActionListener(e -> limparCampos());
        btnAssociar.addActionListener(e -> associarCliente());
        btnOrdenar.addActionListener(e -> ordenarExternamente());
        btnVoltar.addActionListener(e -> {
            if (voltarAction != null) {
                voltarAction.run();
            }
        });

        painelBotoes.add(btnSalvar);
        painelBotoes.add(btnBuscar);
        painelBotoes.add(btnExcluir);
        painelBotoes.add(btnLimpar);
        painelBotoes.add(btnAssociar);
        painelBotoes.add(btnOrdenar);
        painelBotoes.add(btnVoltar);

        JPanel painelCentral = new JPanel(new BorderLayout(16, 16));
        painelCentral.setOpaque(false);
        painelCentral.add(painelFormulario, BorderLayout.CENTER);
        painelCentral.add(painelBotoes, BorderLayout.SOUTH);

        add(painelCentral, BorderLayout.CENTER);

        txtResumo = new JTextArea();
        txtResumo.setEditable(false);
        txtResumo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtResumo.setBackground(COR_CAIXA);
        txtResumo.setForeground(COR_TEXTO);
        txtResumo.setCaretColor(COR_TEXTO);
        txtResumo.setLineWrap(true);
        txtResumo.setWrapStyleWord(true);
        txtResumo.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(COR_BORDA, 1),
                " Voos Cadastrados ",
                0,
                0,
                new Font("Segoe UI", Font.BOLD, 14),
                COR_TEXTO
        ));

        JScrollPane scroll = new JScrollPane(txtResumo);
            scroll.getViewport().setBackground(COR_CAIXA);
            scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.EAST);
        scroll.setPreferredSize(new Dimension(330, 300));

        atualizarResumo();
        atualizarClientes();
    }

    private void salvarVoo() {
        String idTexto = txtId.getText().trim();
        String codigo = txtCodigo.getText().trim();
        String origem = txtOrigem.getText().trim();
        String destino = txtDestino.getText().trim();
        String data = txtData.getText().trim();
        String horario = txtHorario.getText().trim();
        String valor = txtValor.getText().trim();

        if (!validarDadosVoo(codigo, origem, destino, data, horario, valor)) {
            return;
        }
        if (!idTexto.isEmpty() && !idTexto.matches("\\d{1,9}")) {
            mostrarAlerta("O ID deve ser um número inteiro positivo.");
            txtId.requestFocus();
            return;
        }

        String valorFormatado = new BigDecimal(valor.replace(',', '.'))
                .setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        ClienteOpcao clienteSelecionado = (ClienteOpcao) cmbCliente.getSelectedItem();
        Cliente cliente = null;
        if (clienteSelecionado != null && clienteSelecionado.id > 0) {
            try {
                cliente = clienteDAO == null ? null : clienteDAO.buscarCliente(clienteSelecionado.id);
            } catch (Exception e) {
                mostrarAlerta("Não foi possível confirmar o cliente selecionado: " + e.getMessage());
                return;
            }
            if (cliente == null) {
                atualizarClientes();
                mostrarAlerta("O cliente selecionado não está mais cadastrado. Escolha outro.");
                return;
            }
        }
        boolean atualizando = !idTexto.isEmpty()
                ? agenciaService.buscarVoo(Integer.parseInt(idTexto)) != null
                : agenciaService.buscarVoo(codigo) != null;
        Voo voo = new Voo(codigo, origem, destino, data, horario, valorFormatado,
                cmbStatus.getSelectedItem().toString());
        if (!idTexto.isEmpty()) {
            voo.setId(Integer.parseInt(idTexto));
        }
        voo.setIdCliente(cliente == null ? -1 : cliente.getId());
        agenciaService.salvarVoo(voo);
        txtId.setText(String.valueOf(voo.getId()));
        String mensagem = atualizando ? "Voo atualizado com sucesso!" : "Voo cadastrado com sucesso!";
        if (cliente != null) {
            mensagem += "\nCliente associado: " + cliente.getNome() + ".";
        }
        JOptionPane.showMessageDialog(this,
                mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        atualizarResumo();
    }

    private boolean validarDadosVoo(String codigo, String origem, String destino,
                                    String data, String horario, String valor) {
        if (!CODIGO_PATTERN.matcher(codigo).matches()) {
            mostrarAlerta("Código inválido. Use de 2 a 10 letras, números ou hífen.");
            txtCodigo.requestFocus();
            return false;
        }
        if (!LOCAL_PATTERN.matcher(origem).matches() || !LOCAL_PATTERN.matcher(destino).matches()) {
            mostrarAlerta("Origem e destino devem conter de 2 a 60 letras; espaços e hífen são permitidos.");
            return false;
        }
        if (origem.equalsIgnoreCase(destino)) {
            mostrarAlerta("A origem e o destino não podem ser iguais.");
            txtDestino.requestFocus();
            return false;
        }
        try {
            LocalDate.parse(data, FORMATO_DATA);
        } catch (Exception e) {
            mostrarAlerta("Data inválida. Use o formato dd/MM/aaaa e informe uma data existente.");
            txtData.requestFocus();
            return false;
        }
        try {
            LocalTime.parse(horario, FORMATO_HORARIO);
        } catch (Exception e) {
            mostrarAlerta("Horário inválido. Use o formato HH:mm, entre 00:00 e 23:59.");
            txtHorario.requestFocus();
            return false;
        }
        if (!VALOR_PATTERN.matcher(valor).matches()) {
            mostrarAlerta("Valor inválido. Use um valor positivo com até duas casas decimais, por exemplo 250,00.");
            txtValor.requestFocus();
            return false;
        }
        if (new BigDecimal(valor.replace(',', '.')).compareTo(BigDecimal.ZERO) <= 0) {
            mostrarAlerta("O valor do voo deve ser maior que zero.");
            txtValor.requestFocus();
            return false;
        }
        return true;
    }

    private void mostrarAlerta(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Validação", JOptionPane.WARNING_MESSAGE);
    }

    private void buscarVoo() {
        String idTexto = txtId.getText().trim();
        String codigo = txtCodigo.getText().trim();
        Voo voo;
        if (!idTexto.isEmpty()) {
            if (!idTexto.matches("\\d{1,9}") || Integer.parseInt(idTexto) <= 0) {
                mostrarAlerta("Informe um ID inteiro positivo.");
                return;
            }
            voo = agenciaService.buscarVoo(Integer.parseInt(idTexto));
        } else if (CODIGO_PATTERN.matcher(codigo).matches()) {
            voo = agenciaService.buscarVoo(codigo);
        } else {
            mostrarAlerta("Informe o ID (busca por Hash) ou um código de voo válido.");
            return;
        }

        if (voo != null) {
            txtId.setText(String.valueOf(voo.getId()));
            txtCodigo.setText(voo.getCodigo());
            txtOrigem.setText(voo.getOrigem());
            txtDestino.setText(voo.getDestino());
            txtData.setText(voo.getData());
            txtHorario.setText(voo.getHorario());
            txtValor.setText(voo.getValor());
            cmbStatus.setSelectedItem(voo.getStatus());
            selecionarClientePorId(voo.getIdCliente());
            return;
        }

        JOptionPane.showMessageDialog(this, "Voo não encontrado.", "Resultado", JOptionPane.INFORMATION_MESSAGE);
    }

    private void excluirVoo() {
        String idTexto = txtId.getText().trim();
        String codigo = txtCodigo.getText().trim();
        boolean excluido;
        if (!idTexto.isEmpty()) {
            if (!idTexto.matches("\\d{1,9}") || Integer.parseInt(idTexto) <= 0) {
                mostrarAlerta("Informe um ID inteiro positivo.");
                return;
            }
            excluido = agenciaService.excluirVoo(Integer.parseInt(idTexto));
        } else if (CODIGO_PATTERN.matcher(codigo).matches()) {
            excluido = agenciaService.excluirVoo(codigo);
        } else {
            mostrarAlerta("Informe o ID ou um código de voo válido.");
            return;
        }

        if (excluido) {
            limparCampos();
            atualizarResumo();
            JOptionPane.showMessageDialog(this, "Voo removido com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Voo não encontrado para exclusão.", "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private void limparCampos() {
        txtId.setText("");
        txtCodigo.setText("");
        txtOrigem.setText("");
        txtDestino.setText("");
        txtData.setText("");
        txtHorario.setText("");
        txtValor.setText("");
        if (cmbCliente.getItemCount() > 0) {
            cmbCliente.setSelectedIndex(0);
        }
        cmbStatus.setSelectedIndex(0);
        txtCodigo.requestFocus();
    }

    private void associarCliente() {
        String idTexto = txtId.getText().trim();
        String codigo = txtCodigo.getText().trim();
        ClienteOpcao clienteSelecionado = (ClienteOpcao) cmbCliente.getSelectedItem();
        if (clienteSelecionado == null || clienteSelecionado.id <= 0) {
            mostrarAlerta("Selecione um cliente cadastrado.");
            return;
        }
        Voo vooEncontrado;
        if (!idTexto.isEmpty()) {
            if (!idTexto.matches("\\d{1,9}") || Integer.parseInt(idTexto) <= 0) {
                mostrarAlerta("Informe um ID inteiro positivo.");
                txtId.requestFocus();
                return;
            }
            vooEncontrado = agenciaService.buscarVoo(Integer.parseInt(idTexto));
        } else if (CODIGO_PATTERN.matcher(codigo).matches()) {
            vooEncontrado = agenciaService.buscarVoo(codigo);
        } else {
            mostrarAlerta("Informe o ID do voo ou um código válido antes de associar o cliente.");
            txtCodigo.requestFocus();
            return;
        }
        if (vooEncontrado == null) {
            mostrarAlerta("Cadastre ou busque o voo antes de associar um cliente.");
            return;
        }

        try {
            Cliente cliente = clienteDAO.buscarCliente(clienteSelecionado.id);
            if (cliente == null) {
                atualizarClientes();
                mostrarAlerta("Esse cliente não está mais cadastrado. Atualize a lista e selecione outro.");
                return;
            }
            vooEncontrado.setIdCliente(cliente.getId());
            agenciaService.salvarVoo(vooEncontrado);
            txtId.setText(String.valueOf(vooEncontrado.getId()));
            txtCodigo.setText(vooEncontrado.getCodigo());
            atualizarResumo();
            JOptionPane.showMessageDialog(this, "Cliente " + cliente.getNome() + " associado ao voo " + vooEncontrado.getCodigo() + ".", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao consultar cliente: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atualizarResumo() {
        List<Voo> voos = agenciaService.listarVoos();
        if (voos.isEmpty()) {
            txtResumo.setText("Nenhum voo cadastrado.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Voo voo : voos) {
            sb.append("ID: ").append(voo.getId()).append(" | ").append(voo).append("\n");
            if (voo.getIdCliente() > 0) {
                try {
                    Cliente cliente = clienteDAO == null ? null : clienteDAO.buscarCliente(voo.getIdCliente());
                    sb.append("Cliente (FK): ")
                            .append(cliente == null ? "ID " + voo.getIdCliente() : cliente.getNome())
                            .append("\n");
                } catch (Exception e) {
                    sb.append("Cliente (FK): ID ").append(voo.getIdCliente()).append("\n");
                }
            }
            sb.append("\n");
        }
        txtResumo.setText(sb.toString());
    }

    private void ordenarExternamente() {
        try {
            java.io.File resultado = agenciaService.ordenarVoosPorDataHora();
            JOptionPane.showMessageDialog(this,
                    "Ordenação externa por intercalação concluída.\nArquivo gerado: " + resultado.getPath(),
                    "Ordenação externa", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao ordenar: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void atualizarTela() {
        atualizarClientes();
        atualizarResumo();
    }

    private void atualizarClientes() {
        DefaultComboBoxModel<ClienteOpcao> modelo = new DefaultComboBoxModel<>();
        modelo.addElement(new ClienteOpcao(-1, "Selecione um cliente..."));
        try {
            if (clienteDAO != null) {
                for (Cliente cliente : clienteDAO.listarClientes()) {
                    modelo.addElement(new ClienteOpcao(cliente.getId(), cliente.getNome()));
                }
            }
        } catch (Exception e) {
            mostrarAlerta("Não foi possível carregar os clientes: " + e.getMessage());
        }
        cmbCliente.setModel(modelo);
    }

    private void selecionarClientePorId(int idCliente) {
        for (int i = 0; i < cmbCliente.getItemCount(); i++) {
            ClienteOpcao opcao = cmbCliente.getItemAt(i);
            if (opcao.id == idCliente) {
                cmbCliente.setSelectedIndex(i);
                return;
            }
        }
        cmbCliente.setSelectedIndex(0);
    }

    private static class ClienteOpcao {
        private final int id;
        private final String nome;

        private ClienteOpcao(int id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        @Override
        public String toString() {
            return id <= 0 ? nome : nome + " (ID " + id + ")";
        }
    }

    private JButton criarBotao(String texto, Color cor) {
        JButton botao = new JButton(texto);
        botao.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        botao.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botao.setOpaque(true);
        botao.setBackground(cor);
        botao.setForeground(texto.equals("Voltar") ? new Color(15, 23, 42) : getTextoBotao(cor));
        botao.setContentAreaFilled(true);
        botao.setFocusPainted(false);
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botao.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        return botao;
    }

    private void configurarCampo(JTextField campo) {
        campo.setBackground(Color.WHITE);
        campo.setForeground(new Color(15, 23, 42));
        campo.setCaretColor(new Color(15, 23, 42));
        campo.setSelectedTextColor(Color.WHITE);
        campo.setSelectionColor(new Color(59, 130, 246));
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COR_BORDA),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
    }

    private <T> void configurarComboBox(JComboBox<T> combo) {
        Color textoEscuro = new Color(15, 23, 42);
        Color azulSelecao = new Color(30, 64, 175);
        combo.setBackground(Color.WHITE);
        combo.setForeground(textoEscuro);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> lista, Object valor, int indice, boolean selecionado, boolean foco) {
                JLabel item = (JLabel) super.getListCellRendererComponent(
                        lista, valor, indice, selecionado, foco);
                item.setOpaque(true);
                item.setBackground(selecionado ? azulSelecao : Color.WHITE);
                item.setForeground(selecionado ? Color.WHITE : textoEscuro);
                item.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return item;
            }
        });
    }

    private Color getTextoBotao(Color cor) {
        int media = (cor.getRed() + cor.getGreen() + cor.getBlue()) / 3;
        return media < 160 ? Color.WHITE : COR_TEXTO;
    }
}
