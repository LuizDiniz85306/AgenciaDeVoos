import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.CRC32;

public final class ClienteVooDAO implements AutoCloseable {

    private static final int MAGIC = 0x434E4E31;
    private static final int VERSAO = 1;
    private static final long TAMANHO_CABECALHO = 8;
    private static final int TAMANHO_REGISTRO = 13;
    private static final long POSICAO_BLOQUEIO = Long.MAX_VALUE - 1;
    private static final int INATIVO = 0;
    private static final int ATIVO = 1;
    private static final Path DIRETORIO = Path.of("dados", "associacoes");
    private static final Path CAMINHO_DADOS = DIRETORIO.resolve("clientes_voos.db");
    private static final Path CAMINHO_CLIENTE = DIRETORIO.resolve("clientes_voos.cliente.bplus.db");
    private static final Path CAMINHO_VOO = DIRETORIO.resolve("clientes_voos.voo.bplus.db");

    private final RandomAccessFile arquivo;
    private final FileLock bloqueio;
    private final TreeMap<Long, Long> enderecos = new TreeMap<>();
    private ArvoreBMais indicePorCliente;
    private ArvoreBMais indicePorVoo;
    private IOException falhaPersistencia;
    private boolean fechado;

    public ClienteVooDAO() throws Exception {
        Files.createDirectories(DIRETORIO);
        RandomAccessFile aberto = new RandomAccessFile(CAMINHO_DADOS.toFile(), "rw");
        FileLock adquirido = null;
        try {
            try {
                adquirido = aberto.getChannel().tryLock(POSICAO_BLOQUEIO, 1, false);
            } catch (OverlappingFileLockException e) {
                throw new IOException("O arquivo de associações já está aberto por outra instância.", e);
            }
            if (adquirido == null) {
                throw new IOException("O arquivo de associações está em uso por outra instância.");
            }
            arquivo = aberto;
            bloqueio = adquirido;
            if (arquivo.length() == 0) {
                arquivo.writeInt(MAGIC);
                arquivo.writeInt(VERSAO);
                arquivo.getFD().sync();
            }
            carregarAssociacoes();
            reconstruirIndices();
        } catch (Exception e) {
            if (adquirido != null) {
                try {
                    adquirido.release();
                } catch (IOException secundaria) {
                    e.addSuppressed(secundaria);
                }
            }
            try {
                aberto.close();
            } catch (IOException secundaria) {
                e.addSuppressed(secundaria);
            }
            throw e;
        }
    }

    public synchronized boolean incluir(int idCliente, int idVoo) throws Exception {
        verificarDisponivel();
        validarIds(idCliente, idVoo);
        long chave = chaveComposta(idCliente, idVoo);
        if (enderecos.containsKey(chave)) return false;
        try {
            long endereco = arquivo.length();
            arquivo.seek(endereco);
            arquivo.writeByte(INATIVO);
            arquivo.writeInt(idCliente);
            arquivo.writeInt(idVoo);
            arquivo.writeInt(verificacao(idCliente, idVoo));
            arquivo.getFD().sync();
            arquivo.seek(endereco);
            arquivo.writeByte(ATIVO);
            arquivo.getFD().sync();
            enderecos.put(chave, endereco);
            indicePorCliente.inserir(idCliente, idVoo);
            indicePorVoo.inserir(idVoo, idCliente);
            sincronizarIndice(CAMINHO_CLIENTE);
            sincronizarIndice(CAMINHO_VOO);
            return true;
        } catch (Exception e) {
            recuperarAposFalha(e);
            throw new IOException("Falha ao incluir a associação; consulte os vínculos antes de repetir a operação.", e);
        }
    }

    public synchronized boolean excluir(int idCliente, int idVoo) throws Exception {
        verificarDisponivel();
        validarIds(idCliente, idVoo);
        long chave = chaveComposta(idCliente, idVoo);
        Long endereco = enderecos.get(chave);
        if (endereco == null) return false;
        try {
            arquivo.seek(endereco);
            arquivo.writeByte(INATIVO);
            arquivo.getFD().sync();
            enderecos.remove(chave);
            indicePorCliente.remover(idCliente, idVoo);
            indicePorVoo.remover(idVoo, idCliente);
            sincronizarIndice(CAMINHO_CLIENTE);
            sincronizarIndice(CAMINHO_VOO);
            return true;
        } catch (Exception e) {
            recuperarAposFalha(e);
            throw new IOException("Falha ao excluir a associação; consulte os vínculos antes de repetir a operação.", e);
        }
    }

    public synchronized List<Integer> listarVoosDoCliente(int idCliente) throws Exception {
        verificarDisponivel();
        validarId(idCliente);
        List<Integer> resultado = indicePorCliente.buscar(idCliente);
        for (int idVoo : resultado) {
            if (!enderecos.containsKey(chaveComposta(idCliente, idVoo))) {
                throw new IOException("O índice por cliente contém uma associação inválida.");
            }
        }
        return resultado;
    }

    public synchronized List<Integer> listarClientesDoVoo(int idVoo) throws Exception {
        verificarDisponivel();
        validarId(idVoo);
        List<Integer> resultado = indicePorVoo.buscar(idVoo);
        for (int idCliente : resultado) {
            if (!enderecos.containsKey(chaveComposta(idCliente, idVoo))) {
                throw new IOException("O índice por voo contém uma associação inválida.");
            }
        }
        return resultado;
    }

    public synchronized List<AssociacaoClienteVoo> listarTodas() throws Exception {
        verificarDisponivel();
        List<AssociacaoClienteVoo> resultado = new ArrayList<>();
        for (long chave : enderecos.keySet()) {
            resultado.add(new AssociacaoClienteVoo((int) (chave >>> 32), (int) chave));
        }
        return resultado;
    }

    public static boolean possuiAssociacoesPersistidasDoCliente(int idCliente) throws Exception {
        validarId(idCliente);
        return possuiAssociacaoPersistida(idCliente, true);
    }

    public static boolean possuiAssociacoesPersistidasDoVoo(int idVoo) throws Exception {
        validarId(idVoo);
        return possuiAssociacaoPersistida(idVoo, false);
    }

    private static boolean possuiAssociacaoPersistida(int id, boolean porCliente) throws IOException {
        if (!Files.exists(CAMINHO_DADOS)) return false;
        try (RandomAccessFile leitura = new RandomAccessFile(CAMINHO_DADOS.toFile(), "r")) {
            validarCabecalho(leitura);
            long tamanho = leitura.length();
            for (long endereco = TAMANHO_CABECALHO; endereco < tamanho; endereco += TAMANHO_REGISTRO) {
                leitura.seek(endereco);
                int estado = leitura.readUnsignedByte();
                validarEstado(estado);
                if (tamanho - endereco < TAMANHO_REGISTRO) {
                    if (estado == INATIVO) return false;
                    throw new IOException("Há uma associação ativa incompleta no arquivo binário.");
                }
                if (estado == INATIVO) continue;
                int idCliente = leitura.readInt();
                int idVoo = leitura.readInt();
                validarRegistro(idCliente, idVoo, leitura.readInt());
                if ((porCliente ? idCliente : idVoo) == id) return true;
            }
        }
        return false;
    }

    private void carregarAssociacoes() throws IOException {
        validarCabecalho(arquivo);
        enderecos.clear();
        long tamanho = arquivo.length();
        for (long endereco = TAMANHO_CABECALHO; endereco < tamanho; endereco += TAMANHO_REGISTRO) {
            arquivo.seek(endereco);
            int estado = arquivo.readUnsignedByte();
            validarEstado(estado);
            if (tamanho - endereco < TAMANHO_REGISTRO) {
                if (estado != INATIVO) {
                    throw new IOException("Há uma associação ativa incompleta no arquivo binário.");
                }
                arquivo.setLength(endereco);
                arquivo.getFD().sync();
                break;
            }
            if (estado == INATIVO) continue;
            int idCliente = arquivo.readInt();
            int idVoo = arquivo.readInt();
            validarRegistro(idCliente, idVoo, arquivo.readInt());
            long chave = chaveComposta(idCliente, idVoo);
            if (enderecos.putIfAbsent(chave, endereco) != null) {
                throw new IOException("O arquivo binário contém uma chave composta duplicada.");
            }
        }
    }

    private void reconstruirIndices() throws IOException {
        Map<Integer, TreeSet<Integer>> porCliente = new TreeMap<>();
        Map<Integer, TreeSet<Integer>> porVoo = new TreeMap<>();
        for (long chave : enderecos.keySet()) {
            int idCliente = (int) (chave >>> 32);
            int idVoo = (int) chave;
            porCliente.computeIfAbsent(idCliente, ignorado -> new TreeSet<>()).add(idVoo);
            porVoo.computeIfAbsent(idVoo, ignorado -> new TreeSet<>()).add(idCliente);
        }
        substituirIndice(CAMINHO_CLIENTE, porCliente);
        substituirIndice(CAMINHO_VOO, porVoo);
        indicePorCliente = new ArvoreBMais(CAMINHO_CLIENTE.toString());
        indicePorVoo = new ArvoreBMais(CAMINHO_VOO.toString());
    }

    private static void substituirIndice(Path destino, Map<Integer, TreeSet<Integer>> dados)
            throws IOException {
        Path temporario = Files.createTempFile(DIRETORIO, destino.getFileName().toString() + ".", ".tmp");
        try {
            ArvoreBMais indice = new ArvoreBMais(temporario.toString());
            indice.reconstruir(dados);
            sincronizarIndice(temporario);
            try {
                Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private static void sincronizarIndice(Path caminho) throws IOException {
        try (FileChannel canal = FileChannel.open(caminho, StandardOpenOption.WRITE)) {
            canal.force(true);
        }
    }

    private void recuperarAposFalha(Exception original) {
        try {
            carregarAssociacoes();
            reconstruirIndices();
        } catch (Exception recuperacao) {
            original.addSuppressed(recuperacao);
            falhaPersistencia = new IOException(
                    "Não foi possível recuperar as associações. Feche e reabra o sistema.", recuperacao);
        }
    }

    private void verificarDisponivel() throws IOException {
        if (fechado) throw new IOException("O arquivo de associações está fechado.");
        if (falhaPersistencia != null) throw new IOException(falhaPersistencia.getMessage(), falhaPersistencia);
    }

    private static void validarCabecalho(RandomAccessFile arquivo) throws IOException {
        if (arquivo.length() < TAMANHO_CABECALHO) {
            throw new IOException("O cabeçalho do arquivo de associações está incompleto.");
        }
        arquivo.seek(0);
        if (arquivo.readInt() != MAGIC || arquivo.readInt() != VERSAO) {
            throw new IOException("O formato do arquivo de associações não é reconhecido.");
        }
    }

    private static void validarEstado(int estado) throws IOException {
        if (estado != ATIVO && estado != INATIVO) {
            throw new IOException("O arquivo de associações contém uma lápide inválida.");
        }
    }

    private static void validarRegistro(int idCliente, int idVoo, int checksum) throws IOException {
        if (idCliente <= 0 || idVoo <= 0 || checksum != verificacao(idCliente, idVoo)) {
            throw new IOException("O arquivo de associações contém um registro ativo corrompido.");
        }
    }

    private static void validarIds(int idCliente, int idVoo) {
        validarId(idCliente);
        validarId(idVoo);
    }

    private static void validarId(int id) {
        if (id <= 0) throw new IllegalArgumentException("O ID deve ser um número positivo.");
    }

    private static long chaveComposta(int idCliente, int idVoo) {
        return ((long) idCliente << 32) | (idVoo & 0xFFFFFFFFL);
    }

    private static int verificacao(int idCliente, int idVoo) {
        CRC32 checksum = new CRC32();
        checksum.update(ByteBuffer.allocate(8).putInt(idCliente).putInt(idVoo).array());
        return (int) checksum.getValue();
    }

    @Override
    public synchronized void close() throws Exception {
        if (fechado) return;
        fechado = true;
        IOException erro = null;
        try {
            arquivo.getFD().sync();
        } catch (IOException e) {
            erro = e;
        }
        try {
            bloqueio.release();
        } catch (IOException e) {
            if (erro == null) erro = e;
            else erro.addSuppressed(e);
        }
        try {
            arquivo.close();
        } catch (IOException e) {
            if (erro == null) erro = e;
            else erro.addSuppressed(e);
        }
        if (erro != null) throw erro;
    }
}
