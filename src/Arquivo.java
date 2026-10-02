import java.io.File;
import java.io.RandomAccessFile;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Arquivo<T extends Registro> {

    private static final int TAM_CABECALHO = 12;

    private RandomAccessFile arquivo;
    private File arquivoBits;
    private Constructor<T> construtor;
    private HashExtensivel indiceDireto;

    public Arquivo(String nomeArquivo, Constructor<T> construtor)
            throws Exception {

        File diretorio = new File("./dados");

        if (!diretorio.exists()) {
            diretorio.mkdir();
        }

        diretorio = new File("./dados/" + nomeArquivo);

        if (!diretorio.exists()) {
            diretorio.mkdir();
        }

        String caminho = "./dados/" + nomeArquivo + "/" + nomeArquivo + ".db";

        this.arquivo = new RandomAccessFile(caminho, "rw");
        this.arquivoBits = new File(caminho + ".bits.txt");
        this.construtor = construtor;
        this.indiceDireto = new HashExtensivel(caminho + ".hash");

        if (arquivo.length() < TAM_CABECALHO) {

            arquivo.seek(0);

            arquivo.writeInt(0);

            arquivo.writeLong(-1);
        }

        if (!indiceDireto.foiCarregadoDeArquivo() || !indiceDiretoEstaConsistente()) {
            reconstruirIndiceDireto();
        }
        atualizarArquivoBits();
    }

    public int create(T obj) throws Exception {

        arquivo.seek(0);

        int novoId = arquivo.readInt() + 1;

        arquivo.seek(0);
        arquivo.writeInt(novoId);

        obj.setId(novoId);

        byte[] dados = obj.toByteArray();

        long endereco = getDeleted(dados.length);

        if (endereco == -1) {

            arquivo.seek(arquivo.length());

            arquivo.writeByte(' ');
            arquivo.writeShort(dados.length);
            arquivo.write(dados);

        } else {

            arquivo.seek(endereco);

            arquivo.writeByte(' ');
            short tamanhoEspaco = arquivo.readShort();
            arquivo.seek(endereco + 1);
            arquivo.writeShort(tamanhoEspaco);
            arquivo.write(dados);
        }

        indiceDireto.create(novoId, endereco == -1 ? arquivo.length() - dados.length - 3 : endereco);
        atualizarArquivoBits();

        return novoId;
    }

    public T read(int id) throws Exception {

        Long endereco = indiceDireto.read(id);
        if (endereco == null) {
            return null;
        }
        return lerNoEndereco(endereco, id);
    }

    public List<T> readAll() throws Exception {
        List<T> registros = new ArrayList<>();
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            byte lapide = arquivo.readByte();
            short tamanho = arquivo.readShort();
            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (lapide == ' ') {
                T registro = construtor.newInstance();
                registro.fromByteArray(dados);
                registros.add(registro);
            }
        }

        return registros;
    }

    public boolean update(T novoObj) throws Exception {

        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {

            long posicao = arquivo.getFilePointer();

            byte lapide = arquivo.readByte();

            short tamanho = arquivo.readShort();

            byte[] dados = new byte[tamanho];

            arquivo.readFully(dados);

            if (lapide == ' ') {

                T obj = construtor.newInstance();

                obj.fromByteArray(dados);

                if (obj.getId() == novoObj.getId()) {

                    byte[] novosDados =
                            novoObj.toByteArray();

                    short novoTamanho =
                            (short) novosDados.length;

                    long enderecoFinal = posicao;
                    if (novoTamanho <= tamanho) {

                        arquivo.seek(posicao + 3);
                        arquivo.write(novosDados);

                    } else {

                        arquivo.seek(posicao);

                        arquivo.writeByte('*');

                        addDeleted(tamanho, posicao);

                        long novoEndereco =
                                getDeleted(novoTamanho);

                        if (novoEndereco == -1) {

                            arquivo.seek(arquivo.length());

                            arquivo.writeByte(' ');
                            arquivo.writeShort(novoTamanho);
                            arquivo.write(novosDados);

                        } else {

                            arquivo.seek(novoEndereco);

                            arquivo.writeByte(' ');
                            short tamanhoEspaco = arquivo.readShort();
                            arquivo.seek(novoEndereco + 1);
                            arquivo.writeShort(tamanhoEspaco);
                            arquivo.write(novosDados);
                        }
                        enderecoFinal = novoEndereco == -1
                                ? arquivo.length() - novosDados.length - 3
                                : novoEndereco;
                    }

                    indiceDireto.update(novoObj.getId(), enderecoFinal);
                    atualizarArquivoBits();

                    return true;
                }
            }
        }

        return false;
    }

    public boolean delete(int id) throws Exception {

        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {

            long posicao = arquivo.getFilePointer();

            byte lapide = arquivo.readByte();

            short tamanho = arquivo.readShort();

            byte[] dados = new byte[tamanho];

            arquivo.readFully(dados);

            if (lapide == ' ') {

                T obj = construtor.newInstance();

                obj.fromByteArray(dados);

                if (obj.getId() == id) {

                    arquivo.seek(posicao);

                    arquivo.writeByte('*');

                    addDeleted(tamanho, posicao);
                    indiceDireto.delete(id);
                    atualizarArquivoBits();

                    return true;
                }
            }
        }

        return false;
    }

    private void addDeleted(
            int tamanhoEspaco,
            long enderecoEspaco) throws Exception {

        arquivo.seek(4);

        long endereco =
                arquivo.readLong();

        if (endereco == -1) {

            arquivo.seek(4);

            arquivo.writeLong(enderecoEspaco);

            arquivo.seek(enderecoEspaco + 3);

            arquivo.writeLong(-1);

            return;
        }

        long posicaoAnterior = 4;

        while (endereco != -1) {

            arquivo.seek(endereco + 1);

            short tamanho =
                    arquivo.readShort();

            long proximo =
                    arquivo.readLong();

            if (tamanho > tamanhoEspaco) {

                if (posicaoAnterior == 4) {

                    arquivo.seek(4);

                } else {

                    arquivo.seek(posicaoAnterior + 3);
                }

                arquivo.writeLong(enderecoEspaco);

                arquivo.seek(enderecoEspaco + 3);

                arquivo.writeLong(endereco);

                return;
            }

            if (proximo == -1) {

                arquivo.seek(endereco + 3);

                arquivo.writeLong(enderecoEspaco);

                arquivo.seek(enderecoEspaco + 3);

                arquivo.writeLong(-1);

                return;
            }

            posicaoAnterior = endereco;

            endereco = proximo;
        }
    }

    private long getDeleted(int tamanhoNecessario)
            throws Exception {

        arquivo.seek(4);

        long endereco =
                arquivo.readLong();

        long anterior = 4;

        while (endereco != -1) {

            arquivo.seek(endereco + 1);

            short tamanho =
                    arquivo.readShort();

            long proximo =
                    arquivo.readLong();

            if (tamanho >= tamanhoNecessario) {

                if (anterior == 4) {

                    arquivo.seek(4);

                } else {

                    arquivo.seek(anterior + 3);
                }

                arquivo.writeLong(proximo);

                return endereco;
            }

            anterior = endereco;

            endereco = proximo;
        }

        return -1;
    }

    public void close() throws Exception {
        arquivo.close();
    }

    public synchronized File ordenarExterno(Comparator<T> comparador, int tamanhoBloco,
                                             String nomeArquivoOrdenado) throws Exception {
        if (tamanhoBloco < 2) {
            throw new IllegalArgumentException("O bloco de ordenação deve conter ao menos 2 registros.");
        }

        File pasta = arquivoBits.getParentFile();
        File temporarios = new File(pasta, "ordenacao_tmp");
        if (!temporarios.exists() && !temporarios.mkdir()) {
            throw new IOException("Não foi possível criar a pasta temporária de ordenação.");
        }

        List<File> runs = new ArrayList<>();
        List<T> bloco = new ArrayList<>(tamanhoBloco);
        arquivo.seek(TAM_CABECALHO);
        while (arquivo.getFilePointer() < arquivo.length()) {
            byte lapide = arquivo.readByte();
            short tamanho = arquivo.readShort();
            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);
            if (lapide == ' ') {
                T registro = construtor.newInstance();
                registro.fromByteArray(dados);
                bloco.add(registro);
                if (bloco.size() == tamanhoBloco) {
                    runs.add(gravarRun(bloco, comparador, temporarios, runs.size()));
                    bloco.clear();
                }
            }
        }
        if (!bloco.isEmpty()) {
            runs.add(gravarRun(bloco, comparador, temporarios, runs.size()));
        }

        int passo = 0;
        while (runs.size() > 1) {
            List<File> proximos = new ArrayList<>();
            for (int i = 0; i < runs.size(); i += 2) {
                if (i + 1 == runs.size()) {
                    proximos.add(runs.get(i));
                } else {
                    File destino = new File(temporarios, "passo_" + passo + "_" + (i / 2) + ".db");
                    intercalar(runs.get(i), runs.get(i + 1), destino, comparador);
                    runs.get(i).delete();
                    runs.get(i + 1).delete();
                    proximos.add(destino);
                }
            }
            runs = proximos;
            passo++;
        }

        File resultado = new File(pasta, nomeArquivoOrdenado);
        try (RandomAccessFile saida = new RandomAccessFile(resultado, "rw")) {
            saida.setLength(0);
            arquivo.seek(0);
            saida.writeInt(arquivo.readInt());
            saida.writeLong(-1);
            if (!runs.isEmpty()) {
                try (RandomAccessFile run = new RandomAccessFile(runs.get(0), "r")) {
                    while (run.getFilePointer() < run.length()) {
                        saida.writeByte(run.readByte());
                        short tamanho = run.readShort();
                        saida.writeShort(tamanho);
                        byte[] dados = new byte[tamanho];
                        run.readFully(dados);
                        saida.write(dados);
                    }
                }
                runs.get(0).delete();
            }
        }
        temporarios.delete();
        return resultado;
    }

    private T lerNoEndereco(long endereco, int idEsperado) throws Exception {
        if (endereco < TAM_CABECALHO || endereco >= arquivo.length()) {
            return null;
        }
        arquivo.seek(endereco);
        byte lapide = arquivo.readByte();
        short tamanho = arquivo.readShort();
        if (lapide != ' ' || tamanho < 0 || endereco + 3L + tamanho > arquivo.length()) {
            return null;
        }
        byte[] dados = new byte[tamanho];
        arquivo.readFully(dados);
        T registro = construtor.newInstance();
        registro.fromByteArray(dados);
        return registro.getId() == idEsperado ? registro : null;
    }

    private void reconstruirIndiceDireto() throws Exception {
        indiceDireto.limpar();
        arquivo.seek(TAM_CABECALHO);
        while (arquivo.getFilePointer() < arquivo.length()) {
            long endereco = arquivo.getFilePointer();
            byte lapide = arquivo.readByte();
            short tamanho = arquivo.readShort();
            if (tamanho < 0 || endereco + 3L + tamanho > arquivo.length()) {
                throw new IOException("Arquivo de dados corrompido em " + endereco + ".");
            }
            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);
            if (lapide == ' ') {
                T registro = construtor.newInstance();
                registro.fromByteArray(dados);
                indiceDireto.create(registro.getId(), endereco);
            }
        }
    }

    private boolean indiceDiretoEstaConsistente() throws Exception {
        long posicaoOriginal = arquivo.getFilePointer();
        int ativos = 0;
        try {
            arquivo.seek(TAM_CABECALHO);
            while (arquivo.getFilePointer() < arquivo.length()) {
                long endereco = arquivo.getFilePointer();
                byte lapide = arquivo.readByte();
                short tamanho = arquivo.readShort();
                if (tamanho < 0 || endereco + 3L + tamanho > arquivo.length()) {
                    return false;
                }
                byte[] dados = new byte[tamanho];
                arquivo.readFully(dados);
                if (lapide == ' ') {
                    T registro = construtor.newInstance();
                    registro.fromByteArray(dados);
                    Long enderecoIndexado = indiceDireto.read(registro.getId());
                    if (enderecoIndexado == null || enderecoIndexado.longValue() != endereco) {
                        return false;
                    }
                    ativos++;
                }
            }
            return ativos == indiceDireto.quantidadePares();
        } finally {
            arquivo.seek(posicaoOriginal);
        }
    }

    private File gravarRun(List<T> bloco, Comparator<T> comparador, File pasta, int numero)
            throws Exception {
        bloco.sort(comparador);
        File run = new File(pasta, "run_" + numero + ".db");
        try (RandomAccessFile saida = new RandomAccessFile(run, "rw")) {
            saida.setLength(0);
            for (T registro : bloco) {
                byte[] dados = registro.toByteArray();
                saida.writeByte(' ');
                saida.writeShort(dados.length);
                saida.write(dados);
            }
        }
        return run;
    }

    private void intercalar(File primeiro, File segundo, File destino, Comparator<T> comparador)
            throws Exception {
        try (RandomAccessFile a = new RandomAccessFile(primeiro, "r");
             RandomAccessFile b = new RandomAccessFile(segundo, "r");
             RandomAccessFile saida = new RandomAccessFile(destino, "rw")) {
            saida.setLength(0);
            T registroA = lerProximoRun(a);
            T registroB = lerProximoRun(b);
            while (registroA != null || registroB != null) {
                if (registroB == null || (registroA != null && comparador.compare(registroA, registroB) <= 0)) {
                    gravarRegistroRun(saida, registroA);
                    registroA = lerProximoRun(a);
                } else {
                    gravarRegistroRun(saida, registroB);
                    registroB = lerProximoRun(b);
                }
            }
        }
    }

    private T lerProximoRun(RandomAccessFile run) throws Exception {
        if (run.getFilePointer() >= run.length()) {
            return null;
        }
        run.readByte();
        short tamanho = run.readShort();
        byte[] dados = new byte[tamanho];
        run.readFully(dados);
        T registro = construtor.newInstance();
        registro.fromByteArray(dados);
        return registro;
    }

    private void gravarRegistroRun(RandomAccessFile saida, T registro) throws Exception {
        byte[] dados = registro.toByteArray();
        saida.writeByte(' ');
        saida.writeShort(dados.length);
        saida.write(dados);
    }

    private void atualizarArquivoBits() throws IOException {
        long posicaoOriginal = arquivo.getFilePointer();
        try (BufferedWriter escritor = Files.newBufferedWriter(
                arquivoBits.toPath(), StandardCharsets.US_ASCII)) {
            for (long posicaoByte = 0; posicaoByte < arquivo.length(); posicaoByte++) {
                arquivo.seek(posicaoByte);
                int valorByte = arquivo.readUnsignedByte();
                for (int posicaoBit = 7; posicaoBit >= 0; posicaoBit--) {
                    escritor.write((valorByte >> posicaoBit & 1) == 0 ? '0' : '1');
                    if (posicaoBit == 4) {
                        escritor.write(' ');
                    }
                }
                if (posicaoByte % 16 == 15 || posicaoByte == arquivo.length() - 1) {
                    escritor.newLine();
                } else {
                    escritor.write(' ');
                }
            }
        } finally {
            arquivo.seek(posicaoOriginal);
        }
    }
}
