import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.regex.Pattern;

public class TelaVoos extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color COR_TEXTO = Tema.TEXTO;
    private static final Color COR_FUNDO = Tema.FUNDO;
    private static final Color COR_BORDA = Tema.BORDA;
    private static final Pattern CODIGO_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{1,9}");
    private static final Pattern LOCAL_PATTERN = Pattern.compile("[\\p{L}][\\p{L} .'-]{1,59}");
    private static final Pattern VALOR_PATTERN = Pattern.compile("\\d{1,8}([,.]\\d{1,2})?");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMATO_HORARIO = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private final AgenciaService agenciaService;
    private final JTextField txtId = new JTextField();
    private final JTextField txtCodigo = new JTextField();
    private final JTextField txtOrigem = new JTextField();
    private final JTextField txtDestino = new JTextField();
    private final JTextField txtData = new JTextField();
    private final JTextField txtHorario = new JTextField();
    private final JTextField txtValor = new JTextField();
    private final JComboBox<String> cmbStatus = new JComboBox<>(
            new String[]{"Programado", "Confirmado", "Cancelado", "Embarque"});
    private final JTextPane txtResumo = new JTextPane();

    public TelaVoos(Runnable voltarAction) {
        this(voltarAction, new AgenciaService());
    }

    public TelaVoos(Runnable voltarAction, AgenciaService agenciaService) {
        this.agenciaService = agenciaService;
        setBackground(COR_FUNDO);
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setLayout(new BorderLayout(12, 12));

        JPanel cabecalho = new JPanel(new GridLayout(2, 1, 0, 4));
        cabecalho.setOpaque(false);
        JLabel titulo = new JLabel("Gestão de Voos", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titulo.setForeground(COR_TEXTO);
        JLabel orientacao = new JLabel("Consulte os passageiros no menu Check-in.", SwingConstants.CENTER);
        orientacao.setForeground(Tema.TEXTO_SECUNDARIO);
        cabecalho.add(titulo);
        cabecalho.add(orientacao);
        add(cabecalho, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Tema.PAINEL);
        formulario.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COR_BORDA), " Cadastro de Voo ", 0, 0,
                new Font("Segoe UI", Font.BOLD, 14), COR_TEXTO));
        adicionarCampo(formulario, 0, "ID (busca/alteração):", txtId);
        adicionarCampo(formulario, 1, "Código:", txtCodigo);
        adicionarCampo(formulario, 2, "Origem:", txtOrigem);
        adicionarCampo(formulario, 3, "Destino:", txtDestino);
        adicionarCampo(formulario, 4, "Data (dd/MM/aaaa):", txtData);
        adicionarCampo(formulario, 5, "Horário (HH:mm):", txtHorario);
        adicionarCampo(formulario, 6, "Valor (ex.: 250,00):", txtValor);
        configurarComboBox(cmbStatus);
        adicionarCampo(formulario, 7, "Status:", cmbStatus);
        GridBagConstraints preenchimento = new GridBagConstraints();
        preenchimento.gridy = 8;
        preenchimento.weighty = 1;
        formulario.add(Box.createVerticalGlue(), preenchimento);

        JScrollPane rolagemFormulario = new JScrollPane(formulario);
        rolagemFormulario.setBorder(BorderFactory.createEmptyBorder());
        rolagemFormulario.setBackground(Tema.PAINEL);
        rolagemFormulario.getViewport().setBackground(Tema.PAINEL);
        rolagemFormulario.setMinimumSize(new Dimension(390, 240));
        rolagemFormulario.getVerticalScrollBar().setUnitIncrement(16);

        txtResumo.setEditable(false);
        txtResumo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtResumo.setBackground(Tema.PAINEL);
        txtResumo.setForeground(COR_TEXTO);
        txtResumo.setCaretColor(COR_TEXTO);
        txtResumo.setBorder(new EmptyBorder(12, 14, 12, 14));
        JScrollPane rolagemResumo = new JScrollPane(txtResumo);
        rolagemResumo.setMinimumSize(new Dimension(260, 240));
        rolagemResumo.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COR_BORDA), " Voos e passageiros ", 0, 0,
                new Font("Segoe UI", Font.BOLD, 13), COR_TEXTO));
        rolagemResumo.setBackground(Tema.PAINEL);
        rolagemResumo.getViewport().setBackground(Tema.PAINEL);

        JSplitPane divisao = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rolagemFormulario, rolagemResumo);
        divisao.setResizeWeight(0.62);
        divisao.setDividerLocation(580);
        divisao.setBorder(BorderFactory.createEmptyBorder());
        divisao.setBackground(COR_FUNDO);
        add(divisao, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new GridLayout(2, 3, 8, 8));
        botoes.setOpaque(false);
        adicionarBotao(botoes, "Salvar", Tema.PRIMARIA, this::salvarVoo);
        adicionarBotao(botoes, "Buscar", Tema.CABECALHO, this::buscarVoo);
        adicionarBotao(botoes, "Excluir", Tema.PERIGO, this::excluirVoo);
        adicionarBotao(botoes, "Limpar", Tema.NEUTRA, this::limparCampos);
        adicionarBotao(botoes, "Ordenar voos", Tema.SUCESSO, this::ordenarExternamente);
        adicionarBotao(botoes, "Voltar", Tema.VOLTAR, () -> {
            if (voltarAction != null) voltarAction.run();
        });
        add(botoes, BorderLayout.SOUTH);
        atualizarResumo();
    }

    private void adicionarCampo(JPanel formulario, int linha, String texto, JComponent campo) {
        if (campo instanceof JTextField textoCampo) configurarCampo(textoCampo);
        JLabel rotulo = new JLabel(texto);
        rotulo.setForeground(COR_TEXTO);
        rotulo.setLabelFor(campo);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = linha;
        formulario.add(rotulo, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formulario.add(campo, gbc);
    }

    private void salvarVoo() {
        String idTexto = txtId.getText().trim();
        String codigo = txtCodigo.getText().trim();
        String origem = txtOrigem.getText().trim();
        String destino = txtDestino.getText().trim();
        String data = txtData.getText().trim();
        String horario = txtHorario.getText().trim();
        String valor = txtValor.getText().trim();
        if (!validarDadosVoo(codigo, origem, destino, data, horario, valor)) return;
        try {
            if (!idTexto.isEmpty()) validarId(idTexto);
            boolean atualizando = !idTexto.isEmpty()
                    ? agenciaService.buscarVoo(Integer.parseInt(idTexto)) != null
                    : agenciaService.buscarVoo(codigo) != null;
            String valorFormatado = new BigDecimal(valor.replace(',', '.'))
                    .setScale(2, RoundingMode.UNNECESSARY).toPlainString();
            Voo voo = new Voo(codigo, origem, destino, data, horario, valorFormatado,
                    String.valueOf(cmbStatus.getSelectedItem()));
            if (!idTexto.isEmpty()) voo.setId(Integer.parseInt(idTexto));
            agenciaService.salvarVoo(voo);
            txtId.setText(String.valueOf(voo.getId()));
            atualizarResumo();
            JOptionPane.showMessageDialog(this,
                    (atualizando ? "Voo atualizado." : "Voo cadastrado.")
                    + "\nGerencie os passageiros no menu Check-in.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            mostrarErro("Não foi possível salvar o voo", e);
        }
    }

    private boolean validarDadosVoo(String codigo, String origem, String destino,
                                    String data, String horario, String valor) {
        if (!CODIGO_PATTERN.matcher(codigo).matches()) {
            mostrarAlerta("Código inválido. Use de 2 a 10 letras, números ou hífen.");
            txtCodigo.requestFocusInWindow();
            return false;
        }
        if (!LOCAL_PATTERN.matcher(origem).matches() || !LOCAL_PATTERN.matcher(destino).matches()) {
            mostrarAlerta("Origem e destino devem conter de 2 a 60 caracteres: letras, espaços, ponto, apóstrofo ou hífen.");
            return false;
        }
        if (origem.equalsIgnoreCase(destino)) {
            mostrarAlerta("A origem e o destino não podem ser iguais.");
            return false;
        }
        try {
            LocalDate.parse(data, FORMATO_DATA);
        } catch (RuntimeException e) {
            mostrarAlerta("Data inválida. Use dd/MM/aaaa e informe uma data existente.");
            txtData.requestFocusInWindow();
            return false;
        }
        try {
            LocalTime.parse(horario, FORMATO_HORARIO);
        } catch (RuntimeException e) {
            mostrarAlerta("Horário inválido. Use HH:mm, entre 00:00 e 23:59.");
            txtHorario.requestFocusInWindow();
            return false;
        }
        if (!VALOR_PATTERN.matcher(valor).matches()
                || new BigDecimal(valor.replace(',', '.')).signum() <= 0) {
            mostrarAlerta("Informe um valor maior que zero, com até duas casas decimais, por exemplo 250,00.");
            txtValor.requestFocusInWindow();
            return false;
        }
        return true;
    }

    private void validarId(String id) {
        if (!id.matches("\\d{1,9}") || Integer.parseInt(id) <= 0)
            throw new IllegalArgumentException("Informe um ID inteiro positivo de até nove dígitos.");
    }

    private Voo localizarVoo() {
        String id = txtId.getText().trim();
        if (!id.isEmpty()) {
            validarId(id);
            return agenciaService.buscarVoo(Integer.parseInt(id));
        }
        String codigo = txtCodigo.getText().trim();
        if (!CODIGO_PATTERN.matcher(codigo).matches())
            throw new IllegalArgumentException("Informe o ID ou um código de voo válido.");
        return agenciaService.buscarVoo(codigo);
    }

    private void buscarVoo() {
        try {
            Voo voo = localizarVoo();
            if (voo == null) {
                mostrarAlerta("Voo não encontrado.");
                return;
            }
            txtId.setText(String.valueOf(voo.getId()));
            txtCodigo.setText(voo.getCodigo());
            txtOrigem.setText(voo.getOrigem());
            txtDestino.setText(voo.getDestino());
            txtData.setText(voo.getData());
            txtHorario.setText(voo.getHorario());
            txtValor.setText(voo.getValor());
            cmbStatus.setSelectedItem(voo.getStatus());
        } catch (RuntimeException e) {
            mostrarErro("Não foi possível buscar o voo", e);
        }
    }

    private void excluirVoo() {
        try {
            Voo voo = localizarVoo();
            if (voo == null) {
                mostrarAlerta("Voo não encontrado.");
                return;
            }
            int resposta = JOptionPane.showConfirmDialog(this,
                    "Excluir o voo " + voo.getCodigo() + " (ID " + voo.getId() + ")?"
                    + "\nCancele os check-ins deste voo antes de excluí-lo.",
                    "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (resposta != JOptionPane.YES_OPTION) return;
            if (!agenciaService.excluirVoo(voo.getId())) {
                mostrarAlerta("O voo não foi encontrado para exclusão.");
                return;
            }
            limparCampos();
            atualizarResumo();
            JOptionPane.showMessageDialog(this, "Voo removido com sucesso.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            mostrarErro("Não foi possível excluir o voo", e);
        }
    }

    private void limparCampos() {
        for (JTextField campo : new JTextField[]{txtId, txtCodigo, txtOrigem, txtDestino, txtData, txtHorario, txtValor})
            campo.setText("");
        cmbStatus.setSelectedIndex(0);
        txtCodigo.requestFocusInWindow();
    }

    private void atualizarResumo() {
        List<Voo> voos = agenciaService.listarVoos();
        if (voos.isEmpty()) {
            exibirResumo("Nenhum voo cadastrado.");
            return;
        }
        StringBuilder resumo = new StringBuilder();
        for (Voo voo : voos) {
            adicionarVooAoResumo(resumo, voo);
        }
        exibirResumo(resumo.toString());
    }

    private void adicionarVooAoResumo(StringBuilder resumo, Voo voo) {
        resumo.append("Código: ").append(voo.getCodigo()).append("   •   ID: ").append(voo.getId()).append('\n');
        resumo.append("Origem: ").append(voo.getOrigem()).append("  →  Destino: ").append(voo.getDestino()).append('\n');
        resumo.append("Data: ").append(voo.getData()).append("   |   Horário: ").append(voo.getHorario()).append('\n');
        resumo.append("Valor: R$ ").append(voo.getValor()).append("   |   Status: ").append(voo.getStatus()).append('\n');
        List<Cliente> clientes = agenciaService.listarClientesDoVoo(voo.getId());
        resumo.append("Check-ins (").append(clientes.size()).append("): ");
        if (clientes.isEmpty()) resumo.append("nenhum");
        for (int i = 0; i < clientes.size(); i++) {
            if (i > 0) resumo.append("; ");
            Cliente cliente = clientes.get(i);
            resumo.append(cliente.getNome()).append(" (ID ").append(cliente.getId()).append(')');
        }
        resumo.append("\n\n");
    }

    private void exibirResumo(String texto) {
        txtResumo.setText(texto);
        StyledDocument documento = txtResumo.getStyledDocument();
        SimpleAttributeSet corpo = new SimpleAttributeSet();
        StyleConstants.setFontFamily(corpo, "Segoe UI");
        StyleConstants.setFontSize(corpo, 13);
        StyleConstants.setForeground(corpo, Tema.TEXTO);
        documento.setCharacterAttributes(0, documento.getLength(), corpo, true);
        SimpleAttributeSet titulo = new SimpleAttributeSet(corpo);
        StyleConstants.setBold(titulo, true);
        StyleConstants.setForeground(titulo, Tema.FOCO);
        SimpleAttributeSet paragrafo = new SimpleAttributeSet();
        StyleConstants.setSpaceAbove(paragrafo, 7);
        int posicao = 0;
        for (String linha : texto.split("\n", -1)) {
            if (linha.startsWith("Código: ") || linha.startsWith("Voos ordenados por data e horário:")) {
                documento.setCharacterAttributes(posicao, linha.length(), titulo, true);
                documento.setParagraphAttributes(posicao, linha.length(), paragrafo, false);
            }
            posicao += linha.length() + 1;
        }
        txtResumo.setCaretPosition(0);
    }

    private void ordenarExternamente() {
        try {
            java.io.File resultado = agenciaService.ordenarVoosPorDataHora();
            StringBuilder resumo = new StringBuilder("Voos ordenados por data e horário:\n\n");
            int quantidade = 0;
            try (RandomAccessFile arquivo = new RandomAccessFile(resultado, "r")) {
                if (arquivo.length() < 12) throw new IOException("Arquivo ordenado incompleto.");
                arquivo.seek(12);
                while (arquivo.getFilePointer() < arquivo.length()) {
                    if (arquivo.length() - arquivo.getFilePointer() < 3 || arquivo.readByte() != ' ')
                        throw new IOException("Registro inválido no arquivo ordenado.");
                    int tamanho = arquivo.readUnsignedShort();
                    if (tamanho > arquivo.length() - arquivo.getFilePointer())
                        throw new IOException("Registro incompleto no arquivo ordenado.");
                    byte[] dados = new byte[tamanho];
                    arquivo.readFully(dados);
                    Voo voo = new Voo();
                    voo.fromByteArray(dados);
                    adicionarVooAoResumo(resumo, voo);
                    quantidade++;
                }
            }
            if (quantidade == 0) resumo.append("Nenhum voo cadastrado.");
            exibirResumo(resumo.toString());
        } catch (Exception e) {
            mostrarErro("Não foi possível ordenar os voos", e);
        }
    }

    public void atualizarTela() {
        try {
            atualizarResumo();
        } catch (RuntimeException e) {
            mostrarErro("Não foi possível atualizar a lista de voos", e);
        }
    }

    private void mostrarAlerta(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Validação", JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarErro(String operacao, Exception e) {
        JOptionPane.showMessageDialog(this, operacao + ":\n" + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private void adicionarBotao(JPanel painel, String texto, Color cor, Runnable acao) {
        JButton botao = new JButton(texto);
        botao.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        botao.setFont(new Font("Segoe UI", Font.BOLD, 12));
        botao.setOpaque(true);
        botao.setBackground(cor);
        botao.setForeground(texto.equals("Voltar") ? Tema.TEXTO_CAMPO : Tema.TEXTO);
        botao.setContentAreaFilled(true);
        botao.setFocusPainted(false);
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botao.setBorder(new EmptyBorder(10, 10, 10, 10));
        botao.addActionListener(e -> acao.run());
        painel.add(botao);
    }

    private void configurarCampo(JTextField campo) {
        campo.setBackground(Tema.CAMPO);
        campo.setForeground(Tema.TEXTO_CAMPO);
        campo.setCaretColor(Tema.TEXTO_CAMPO);
        campo.setSelectedTextColor(Tema.TEXTO);
        campo.setSelectionColor(Tema.SELECAO);
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(COR_BORDA),
                new EmptyBorder(4, 8, 4, 8)));
        campo.setPreferredSize(new Dimension(180, 30));
    }

    private void configurarComboBox(JComboBox<String> combo) {
        combo.setBackground(Tema.CAMPO);
        combo.setForeground(Tema.TEXTO_CAMPO);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setPreferredSize(new Dimension(180, 30));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                           boolean selecionado, boolean foco) {
                Component item = super.getListCellRendererComponent(lista, valor, indice, selecionado, foco);
                item.setBackground(selecionado ? Tema.SELECAO : Tema.CAMPO);
                item.setForeground(selecionado ? Tema.TEXTO : Tema.TEXTO_CAMPO);
                return item;
            }
        });
    }
}
