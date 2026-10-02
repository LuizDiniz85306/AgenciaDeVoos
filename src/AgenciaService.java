import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class AgenciaService {

    private final Arquivo<Voo> arquivoVoos;
    private final ArvoreBMais indiceVoosPorCliente;

    public AgenciaService() {
        try {
            arquivoVoos = new Arquivo<>("voos", Voo.class.getConstructor());
            migrarRelacoesLegadas();
            indiceVoosPorCliente = new ArvoreBMais("./dados/voos/voos.id_cliente.bplus.db");
            // A B+ é persistida. Ela só é reconstruída ao ser criada ou se
            // uma conferência completa detectar divergência com os dados.
            if (!indiceVoosPorCliente.foiCarregadaDeArquivo()
                    || !indiceBMaisEstaConsistente()) {
                reconstruirIndicePorCliente();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível abrir os arquivos binários de voos.", e);
        }
    }

    public synchronized List<Voo> listarVoos() {
        try {
            return arquivoVoos.readAll();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível listar os voos.", e);
        }
    }

    /** Busca direta pela PK, atendida pelo Hash Extensível do Arquivo. */
    public synchronized Voo buscarVoo(int id) {
        try {
            return arquivoVoos.read(id);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível buscar o voo pelo ID.", e);
        }
    }

    public synchronized Voo buscarVoo(String codigo) {
        String codigoNormalizado = normalizarCodigo(codigo);
        for (Voo voo : listarVoos()) {
            if (normalizarCodigo(voo.getCodigo()).equals(codigoNormalizado)) {
                return voo;
            }
        }
        return null;
    }

    public synchronized void salvarVoo(Voo voo) {
        try {
            validarClienteDaFk(voo.getIdCliente());
            Voo existente = voo.getId() > 0 ? buscarVoo(voo.getId()) : buscarVoo(voo.getCodigo());
            Voo mesmoCodigo = buscarVoo(voo.getCodigo());
            if (mesmoCodigo != null && (existente == null || mesmoCodigo.getId() != existente.getId())) {
                throw new IllegalArgumentException("Já existe um voo com este código.");
            }

            if (existente == null) {
                arquivoVoos.create(voo);
                if (voo.getIdCliente() > 0) {
                    indiceVoosPorCliente.inserir(voo.getIdCliente(), voo.getId());
                }
            } else {
                voo.setId(existente.getId());
                if (!arquivoVoos.update(voo)) {
                    throw new IllegalStateException("O voo não pôde ser atualizado.");
                }
                if (existente.getIdCliente() > 0) {
                    indiceVoosPorCliente.remover(existente.getIdCliente(), existente.getId());
                }
                if (voo.getIdCliente() > 0) {
                    indiceVoosPorCliente.inserir(voo.getIdCliente(), voo.getId());
                }
            }
        } catch (Exception e) {
            recuperarIndicePorCliente(e);
            throw new IllegalStateException("Não foi possível salvar o voo no arquivo binário.", e);
        }
    }

    public synchronized boolean excluirVoo(int id) {
        Voo voo = buscarVoo(id);
        if (voo == null) return false;
        try {
            if (!arquivoVoos.delete(id)) return false;
            if (voo.getIdCliente() > 0) {
                indiceVoosPorCliente.remover(voo.getIdCliente(), id);
            }
            return true;
        } catch (Exception e) {
            recuperarIndicePorCliente(e);
            throw new IllegalStateException("Não foi possível excluir o voo.", e);
        }
    }

    public synchronized boolean excluirVoo(String codigo) {
        Voo voo = buscarVoo(codigo);
        return voo != null && excluirVoo(voo.getId());
    }

    /** Consulta 1:N: a B+ localiza todos os IDs de voos da FK informada. */
    public synchronized List<Voo> listarVoosDoCliente(int idCliente) {
        List<Voo> resultado = new ArrayList<>();
        for (Integer idVoo : indiceVoosPorCliente.buscar(idCliente)) {
            Voo voo = buscarVoo(idVoo);
            if (voo != null && voo.getIdCliente() == idCliente) {
                resultado.add(voo);
            }
        }
        return resultado;
    }

    public synchronized boolean possuiVoosDoCliente(int idCliente) {
        return !indiceVoosPorCliente.buscar(idCliente).isEmpty();
    }

    public synchronized File ordenarVoosPorDataHora() {
        try {
            return arquivoVoos.ordenarExterno(
                    Comparator.comparing(Voo::getChaveOrdenacao).thenComparing(Voo::getCodigo),
                    3,
                    "voos_ordenados_por_data_hora.db");
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível executar a ordenação externa.", e);
        }
    }

    private void reconstruirIndicePorCliente() throws Exception {
        indiceVoosPorCliente.reconstruir(mapaVoosPorCliente());
    }

    private Map<Integer, List<Integer>> mapaVoosPorCliente() throws Exception {
        Map<Integer, List<Integer>> dados = new TreeMap<>();
        for (Voo voo : arquivoVoos.readAll()) {
            if (voo.getIdCliente() > 0) {
                dados.computeIfAbsent(voo.getIdCliente(), ignorado -> new ArrayList<>()).add(voo.getId());
            }
        }
        for (List<Integer> ids : dados.values()) {
            Collections.sort(ids);
        }
        return dados;
    }

    private boolean indiceBMaisEstaConsistente() throws Exception {
        return mapaVoosPorCliente().equals(indiceVoosPorCliente.listarEntradas());
    }

    /**
     * Garante a integridade referencial também para chamadas que não passam
     * pela interface Swing. O valor -1 representa voo sem cliente associado.
     */
    private void validarClienteDaFk(int idCliente) throws Exception {
        if (idCliente == -1) {
            return;
        }
        if (idCliente <= 0) {
            throw new IllegalArgumentException("A FK do cliente deve ser positiva ou -1 para não associada.");
        }

        ClienteDAO clientes = new ClienteDAO();
        try {
            if (clientes.buscarCliente(idCliente) == null) {
                throw new IllegalArgumentException("O cliente informado na FK não existe.");
            }
        } finally {
            clientes.fechar();
        }
    }

    /** Recria a B+ a partir do arquivo de dados após qualquer falha no CRUD. */
    private void recuperarIndicePorCliente(Exception causa) {
        try {
            reconstruirIndicePorCliente();
        } catch (Exception falhaRecuperacao) {
            causa.addSuppressed(falhaRecuperacao);
        }
    }

    /**
     * Migra somente uma vez a associação antiga VooCliente para a FK direta
     * exigida nesta fase. Como o modelo antigo era N:N, o primeiro vínculo
     * ativo de cada voo é preservado; os demais continuam intactos no arquivo
     * legado, que não é mais usado pela aplicação.
     */
    private void migrarRelacoesLegadas() throws Exception {
        File arquivoLegado = new File("./dados/voos_clientes/voos_clientes.db");
        File marcador = new File("./dados/voos/migracao_1n_v1.done");
        if (!arquivoLegado.exists() || marcador.exists()) {
            return;
        }

        Map<String, Integer> clientePorCodigo = new HashMap<>();
        try (RandomAccessFile legado = new RandomAccessFile(arquivoLegado, "r")) {
            if (legado.length() >= 12) {
                legado.seek(12);
                while (legado.getFilePointer() < legado.length()) {
                    byte lapide = legado.readByte();
                    short tamanho = legado.readShort();
                    if (tamanho < 0 || legado.getFilePointer() + tamanho > legado.length()) {
                        break;
                    }
                    byte[] dados = new byte[tamanho];
                    legado.readFully(dados);
                    if (lapide == ' ') {
                        try (DataInputStream entrada = new DataInputStream(new ByteArrayInputStream(dados))) {
                            entrada.readInt(); // id da relação antiga
                            int idCliente = entrada.readInt();
                            String codigoVoo = normalizarCodigo(entrada.readUTF());
                            if (idCliente > 0) clientePorCodigo.putIfAbsent(codigoVoo, idCliente);
                        }
                    }
                }
            }
        }

        for (Voo voo : arquivoVoos.readAll()) {
            Integer idCliente = clientePorCodigo.get(normalizarCodigo(voo.getCodigo()));
            if (voo.getIdCliente() <= 0 && idCliente != null) {
                voo.setIdCliente(idCliente);
                arquivoVoos.update(voo);
            }
        }
        try (FileOutputStream saida = new FileOutputStream(marcador)) {
            saida.write('1');
        }
    }

    public synchronized void fechar() {
        try {
            arquivoVoos.close();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível fechar os arquivos de voos.", e);
        }
    }

    private String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
    }
}
