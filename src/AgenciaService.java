import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AgenciaService implements AutoCloseable {

    private final Arquivo<Voo> arquivoVoos;
    private final ClienteDAO clienteDAO;
    private final ClienteVooDAO associacoes;
    private final List<String> avisosInicializacao = new ArrayList<>();
    private boolean fechado;

    public AgenciaService() {
        ClienteVooDAO vinculosAbertos = null;
        ClienteDAO clientesAbertos = null;
        Arquivo<Voo> voosAbertos = null;
        try {
            verificarEstadoDaMigracao();
            vinculosAbertos = new ClienteVooDAO();
            clientesAbertos = new ClienteDAO();
            voosAbertos = new Arquivo<>("voos", Voo.class.getConstructor());
            associacoes = vinculosAbertos;
            clienteDAO = clientesAbertos;
            arquivoVoos = voosAbertos;
            migrarFaseII();
            conferirIntegridade();
        } catch (Exception e) {
            fecharAposFalha(voosAbertos, clientesAbertos, vinculosAbertos, e);
            throw new IllegalStateException("Não foi possível abrir a agência: " + e.getMessage(), e);
        }
    }

    public synchronized ClienteDAO getClienteDAO() {
        verificarAberto();
        return clienteDAO;
    }

    public synchronized List<String> getAvisosInicializacao() {
        return List.copyOf(avisosInicializacao);
    }

    public synchronized List<Cliente> listarClientes() {
        verificarAberto();
        try {
            return clienteDAO.listarClientes();
        } catch (Exception e) {
            throw falha("listar os clientes", e);
        }
    }

    public synchronized Cliente buscarCliente(int id) {
        verificarAberto();
        try {
            return clienteDAO.buscarCliente(id);
        } catch (Exception e) {
            throw falha("buscar o cliente", e);
        }
    }

    public synchronized boolean excluirCliente(int id) {
        verificarAberto();
        synchronized (ClienteDAO.TRAVA_DADOS) {
            if (possuiVoosDoCliente(id)) {
                throw new IllegalStateException("Este cliente possui check-ins. Cancele-os no menu Check-in antes de excluir o cadastro.");
            }
            try {
                return clienteDAO.excluirCliente(id);
            } catch (Exception e) {
                throw falha("excluir o cliente", e);
            }
        }
    }

    public synchronized List<Voo> listarVoos() {
        verificarAberto();
        try {
            return arquivoVoos.readAll();
        } catch (Exception e) {
            throw falha("listar os voos", e);
        }
    }

    public synchronized Voo buscarVoo(int id) {
        verificarAberto();
        try {
            return arquivoVoos.read(id);
        } catch (Exception e) {
            throw falha("buscar o voo pelo ID", e);
        }
    }

    public synchronized Voo buscarVoo(String codigo) {
        String codigoNormalizado = normalizarCodigo(codigo);
        for (Voo voo : listarVoos()) {
            if (normalizarCodigo(voo.getCodigo()).equals(codigoNormalizado)) return voo;
        }
        return null;
    }

    public synchronized void salvarVoo(Voo voo) {
        verificarAberto();
        if (voo == null || !normalizarCodigo(voo.getCodigo()).matches("[A-Z0-9][A-Z0-9-]{1,9}")) {
            throw new IllegalArgumentException("Informe um código de voo válido, com 2 a 10 letras, números ou hífen.");
        }
        if (voo.getId() == 0 || voo.getId() < -1) {
            throw new IllegalArgumentException("O ID deve ser positivo, ou -1 para um novo voo.");
        }
        if (voo.getIdCliente() != -1) {
            throw new IllegalArgumentException("Use o menu Check-in para adicionar passageiros ao voo.");
        }
        synchronized (ClienteDAO.TRAVA_DADOS) {
            Voo existente = voo.getId() > 0 ? buscarVoo(voo.getId()) : buscarVoo(voo.getCodigo());
            if (voo.getId() > 0 && existente == null) {
                throw new IllegalArgumentException("O voo informado para alteração não existe.");
            }
            Voo mesmoCodigo = buscarVoo(voo.getCodigo());
            if (mesmoCodigo != null && (existente == null || mesmoCodigo.getId() != existente.getId())) {
                throw new IllegalArgumentException("Já existe um voo com este código.");
            }
            voo.setCodigo(normalizarCodigo(voo.getCodigo()));
            try {
                if (existente == null) {
                    arquivoVoos.create(voo);
                } else {
                    voo.setId(existente.getId());
                    if (!arquivoVoos.update(voo)) throw new IllegalStateException("O voo não pôde ser atualizado.");
                }
            } catch (Exception e) {
                throw falha("salvar o voo", e);
            }
        }
    }

    public synchronized boolean excluirVoo(int id) {
        verificarAberto();
        synchronized (ClienteDAO.TRAVA_DADOS) {
            if (!idsClientesDoVoo(id).isEmpty()) {
                throw new IllegalStateException("Este voo possui check-ins. Cancele-os no menu Check-in antes de excluir o voo.");
            }
            try {
                return arquivoVoos.delete(id);
            } catch (Exception e) {
                throw falha("excluir o voo", e);
            }
        }
    }

    public synchronized boolean excluirVoo(String codigo) {
        Voo voo = buscarVoo(codigo);
        return voo != null && excluirVoo(voo.getId());
    }

    public synchronized boolean associarClienteAoVoo(int idCliente, int idVoo) {
        verificarAberto();
        synchronized (ClienteDAO.TRAVA_DADOS) {
            validarEntidades(idCliente, idVoo);
            try {
                return associacoes.incluir(idCliente, idVoo);
            } catch (Exception e) {
                throw falha("fazer o check-in", e);
            }
        }
    }

    public synchronized boolean desassociarClienteDoVoo(int idCliente, int idVoo) {
        verificarAberto();
        synchronized (ClienteDAO.TRAVA_DADOS) {
            validarEntidades(idCliente, idVoo);
            try {
                return associacoes.excluir(idCliente, idVoo);
            } catch (Exception e) {
                throw falha("cancelar o check-in", e);
            }
        }
    }

    public synchronized List<Voo> listarVoosDoCliente(int idCliente) {
        verificarAberto();
        List<Voo> resultado = new ArrayList<>();
        for (int idVoo : idsVoosDoCliente(idCliente)) {
            Voo voo = buscarVoo(idVoo);
            if (voo == null) throw new IllegalStateException("Associação com voo inexistente: " + idVoo + ".");
            resultado.add(voo);
        }
        return resultado;
    }

    public synchronized List<Cliente> listarClientesDoVoo(int idVoo) {
        verificarAberto();
        List<Cliente> resultado = new ArrayList<>();
        for (int idCliente : idsClientesDoVoo(idVoo)) {
            Cliente cliente = buscarCliente(idCliente);
            if (cliente == null) throw new IllegalStateException("Associação com cliente inexistente: " + idCliente + ".");
            resultado.add(cliente);
        }
        return resultado;
    }

    public synchronized boolean possuiVoosDoCliente(int idCliente) {
        verificarAberto();
        return !idsVoosDoCliente(idCliente).isEmpty();
    }

    private List<Integer> idsVoosDoCliente(int idCliente) {
        try {
            return associacoes.listarVoosDoCliente(idCliente);
        } catch (Exception e) {
            throw falha("consultar os voos do passageiro", e);
        }
    }

    private List<Integer> idsClientesDoVoo(int idVoo) {
        try {
            return associacoes.listarClientesDoVoo(idVoo);
        } catch (Exception e) {
            throw falha("consultar os passageiros do voo", e);
        }
    }

    public synchronized File ordenarVoosPorDataHora() {
        verificarAberto();
        try {
            return arquivoVoos.ordenarExterno(
                    Comparator.comparing(Voo::getChaveOrdenacao).thenComparing(Voo::getCodigo),
                    3, "voos_ordenados_por_data_hora.db");
        } catch (Exception e) {
            throw falha("executar a ordenação externa", e);
        }
    }

    private void validarEntidades(int idCliente, int idVoo) {
        if (idCliente <= 0 || idVoo <= 0) throw new IllegalArgumentException("Selecione um cliente e um voo com IDs positivos.");
        if (buscarCliente(idCliente) == null) throw new IllegalArgumentException("O cliente informado não existe.");
        if (buscarVoo(idVoo) == null) throw new IllegalArgumentException("O voo informado não existe.");
    }

    private void conferirIntegridade() throws Exception {
        for (AssociacaoClienteVoo associacao : associacoes.listarTodas()) {
            if (clienteDAO.buscarCliente(associacao.getIdCliente()) == null
                    || arquivoVoos.read(associacao.getIdVoo()) == null) {
                throw new IOException("Associação inválida (cliente " + associacao.getIdCliente()
                        + ", voo " + associacao.getIdVoo() + "). Restaure os dados a partir do backup.");
            }
        }
    }

    private void migrarFaseII() throws Exception {
        Path pasta = Path.of("dados", "associacoes");
        Path marcador = pasta.resolve("migracao_fase_iii_v1.done");
        if (Files.exists(marcador)) return;
        Path backup = pasta.resolve("backup_fase_ii");
        Files.createDirectories(backup);
        copiarParaBackup(Path.of("dados", "clientes", "clientes.db"), backup.resolve("clientes.db"));
        copiarParaBackup(Path.of("dados", "voos", "voos.db"), backup.resolve("voos.db"));
        Path legado = Path.of("dados", "voos_clientes", "voos_clientes.db");
        copiarParaBackup(legado, backup.resolve("voos_clientes.db"));
        List<Voo> voos = arquivoVoos.readAll();
        for (Voo voo : voos) {
            if (voo.getIdCliente() > 0) importarAssociacao(voo.getIdCliente(), voo);
        }
        importarArquivoLegado(legado);
        conferirIntegridade();
        for (Voo voo : voos) {
            if (voo.getIdCliente() != -1) {
                voo.setIdCliente(-1);
                if (!arquivoVoos.update(voo)) throw new IOException("Não foi possível migrar o voo " + voo.getId() + ".");
            }
        }
        arquivoVoos.sincronizar();
        Path temporario = pasta.resolve("migracao_fase_iii_v1.tmp");
        try (RandomAccessFile saida = new RandomAccessFile(temporario.toFile(), "rw")) {
            saida.setLength(0);
            saida.writeUTF("FASE_III_V1");
            saida.getFD().sync();
        }
        moverArquivoSincronizado(temporario, marcador);
    }

    private void copiarParaBackup(Path origem, Path destino) throws IOException {
        if (!Files.exists(origem) || Files.exists(destino)) return;
        Path temporario = Files.createTempFile(destino.getParent(), destino.getFileName().toString(), ".tmp");
        try {
            Files.copy(origem, temporario, StandardCopyOption.REPLACE_EXISTING);
            try (FileChannel canal = FileChannel.open(temporario, StandardOpenOption.WRITE)) {
                canal.force(true);
            }
            moverArquivoSincronizado(temporario, destino);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private void moverArquivoSincronizado(Path origem, Path destino) throws IOException {
        try {
            Files.move(origem, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void verificarEstadoDaMigracao() throws IOException {
        Path pasta = Path.of("dados", "associacoes");
        Path marcador = pasta.resolve("migracao_fase_iii_v1.done");
        if (!Files.exists(marcador)) return;
        try (RandomAccessFile entrada = new RandomAccessFile(marcador.toFile(), "r")) {
            if (!"FASE_III_V1".equals(entrada.readUTF()) || entrada.getFilePointer() != entrada.length()) {
                throw new IOException("O marcador de migração é inválido.");
            }
        } catch (IOException e) {
            throw new IOException("Marcador de migração inválido ou incompleto. Restaure a base pelo backup; não apague o marcador para reimportar os vínculos.", e);
        }
        Path dados = pasta.resolve("clientes_voos.db");
        if (!Files.isRegularFile(dados) || Files.size(dados) < 8) {
            throw new IOException("O intermediário N:N está ausente ou incompleto após a migração. Restaure a pasta dados de um backup recente; ela não será recriada vazia.");
        }
    }

    private void importarAssociacao(int idCliente, Voo voo) throws Exception {
        if (clienteDAO.buscarCliente(idCliente) == null) {
            avisosInicializacao.add("Vínculo antigo ignorado: cliente " + idCliente
                    + " inexistente no voo " + voo.getCodigo() + ". Original preservado no backup.");
        } else {
            associacoes.incluir(idCliente, voo.getId());
        }
    }

    private void importarArquivoLegado(Path caminho) throws Exception {
        if (!Files.exists(caminho)) return;
        try (RandomAccessFile legado = new RandomAccessFile(caminho.toFile(), "r")) {
            if (legado.length() < 12) throw new IOException("O arquivo legado está incompleto; o original foi preservado.");
            legado.seek(12);
            while (legado.getFilePointer() < legado.length()) {
                if (legado.length() - legado.getFilePointer() < 3) throw new IOException("Cabeçalho de registro legado incompleto.");
                byte lapide = legado.readByte();
                int tamanho = legado.readShort();
                if (tamanho < 0 || legado.getFilePointer() + tamanho > legado.length()) {
                    throw new IOException("Tamanho inválido no arquivo legado de associações.");
                }
                byte[] dados = new byte[tamanho];
                legado.readFully(dados);
                if (lapide == ' ') {
                    try (DataInputStream entrada = new DataInputStream(new ByteArrayInputStream(dados))) {
                        entrada.readInt();
                        int idCliente = entrada.readInt();
                        Voo voo = buscarVoo(entrada.readUTF());
                        if (voo != null && idCliente > 0) importarAssociacao(idCliente, voo);
                        else avisosInicializacao.add("Vínculo legado inválido não importado. Original preservado no backup.");
                    }
                } else if (lapide != '*') {
                    throw new IOException("Lápide inválida no arquivo legado de associações.");
                }
            }
        }
    }

    private void verificarAberto() {
        if (fechado) throw new IllegalStateException("O serviço já foi fechado.");
    }

    private IllegalStateException falha(String operacao, Exception causa) {
        return new IllegalStateException("Não foi possível " + operacao + ": " + causa.getMessage(), causa);
    }

    private void fecharAposFalha(Arquivo<Voo> voos, ClienteDAO clientes, ClienteVooDAO vinculos, Exception causa) {
        if (voos != null) {
            try { voos.close(); } catch (Exception e) { causa.addSuppressed(e); }
        }
        if (clientes != null) {
            try { clientes.close(); } catch (Exception e) { causa.addSuppressed(e); }
        }
        if (vinculos != null) {
            try { vinculos.close(); } catch (Exception e) { causa.addSuppressed(e); }
        }
    }

    public synchronized void fechar() { close(); }

    @Override
    public synchronized void close() {
        if (fechado) return;
        Exception causa = null;
        try { arquivoVoos.close(); } catch (Exception e) { causa = e; }
        try { clienteDAO.close(); } catch (Exception e) {
            if (causa == null) causa = e; else causa.addSuppressed(e);
        }
        try { associacoes.close(); } catch (Exception e) {
            if (causa == null) causa = e; else causa.addSuppressed(e);
        }
        fechado = true;
        if (causa != null) throw falha("fechar os arquivos", causa);
    }

    private String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
    }
}
