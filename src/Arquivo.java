import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class Arquivo<T extends Registro> {

    private static final int TAM_CABECALHO = 12;
    private static final byte ATIVO = ' ';
    private static final byte EXCLUIDO = '*';

    private final RandomAccessFile arquivo;
    private final Constructor<T> construtor;

    public Arquivo(String nomeArquivo, Constructor<T> construtor) throws Exception {
        File diretorio = new File("dados", nomeArquivo);
        if (!diretorio.exists() && !diretorio.mkdirs()) {
            throw new Exception("Não foi possível criar o diretório de dados.");
        }

        File arquivoDados = new File(diretorio, nomeArquivo + ".db");
        this.arquivo = new RandomAccessFile(arquivoDados, "rw");
        this.construtor = construtor;

        if (arquivo.length() == 0) {
            arquivo.seek(0);
            arquivo.writeInt(0);
            arquivo.writeLong(-1L);
        } else if (arquivo.length() < TAM_CABECALHO) {
            throw new Exception("Arquivo binário corrompido: cabeçalho incompleto.");
        }
    }

    public int create(T obj) throws Exception {
        byte[] dados = obj.toByteArray();
        validarTamanho(dados.length);

        arquivo.seek(0);
        int novoId = arquivo.readInt() + 1;
        arquivo.seek(0);
        arquivo.writeInt(novoId);

        obj.setId(novoId);
        dados = obj.toByteArray();
        validarTamanho(dados.length);

        long endereco = getDeleted(dados.length);
        if (endereco >= 0) {
            escreverNoEspaco(endereco, dados);
        } else {
            arquivo.seek(arquivo.length());
            escreverRegistro(ATIVO, dados.length, dados);
        }

        return novoId;
    }

    public T read(int id) throws Exception {
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long posicao = arquivo.getFilePointer();
            byte lapide = arquivo.readByte();
            int tamanho = arquivo.readUnsignedShort();

            validarRegistro(posicao, tamanho);
            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (lapide == ATIVO) {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                if (obj.getId() == id) return obj;
            }
        }
        return null;
    }

    public List<T> readAll() throws Exception {
        List<T> registros = new ArrayList<>();
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long posicao = arquivo.getFilePointer();
            byte lapide = arquivo.readByte();
            int tamanho = arquivo.readUnsignedShort();

            validarRegistro(posicao, tamanho);
            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (lapide == ATIVO) {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                registros.add(obj);
            }
        }
        return registros;
    }

    public boolean update(T novoObj) throws Exception {
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long posicao = arquivo.getFilePointer();
            byte lapide = arquivo.readByte();
            int tamanhoAntigo = arquivo.readUnsignedShort();
            validarRegistro(posicao, tamanhoAntigo);

            byte[] dadosAntigos = new byte[tamanhoAntigo];
            arquivo.readFully(dadosAntigos);

            if (lapide != ATIVO) continue;

            T obj = construtor.newInstance();
            obj.fromByteArray(dadosAntigos);

            if (obj.getId() != novoObj.getId()) continue;

            byte[] novosDados = novoObj.toByteArray();
            validarTamanho(novosDados.length);

            if (novosDados.length <= tamanhoAntigo) {
                arquivo.seek(posicao + 3);
                arquivo.write(novosDados);
                return true;
            }

            marcarComoExcluido(posicao, tamanhoAntigo);

            long novoEndereco = getDeleted(novosDados.length);
            if (novoEndereco >= 0) {
                escreverNoEspaco(novoEndereco, novosDados);
            } else {
                arquivo.seek(arquivo.length());
                escreverRegistro(ATIVO, novosDados.length, novosDados);
            }
            return true;
        }
        return false;
    }

    public boolean delete(int id) throws Exception {
        arquivo.seek(TAM_CABECALHO);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long posicao = arquivo.getFilePointer();
            byte lapide = arquivo.readByte();
            int tamanho = arquivo.readUnsignedShort();
            validarRegistro(posicao, tamanho);

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (lapide != ATIVO) continue;

            T obj = construtor.newInstance();
            obj.fromByteArray(dados);
            if (obj.getId() == id) {
                marcarComoExcluido(posicao, tamanho);
                return true;
            }
        }
        return false;
    }

    private void escreverRegistro(byte lapide, int tamanho, byte[] dados) throws Exception {
        arquivo.writeByte(lapide);
        arquivo.writeShort(tamanho);
        arquivo.write(dados);
    }

    private void escreverNoEspaco(long endereco, byte[] dados) throws Exception {
        arquivo.seek(endereco);
        arquivo.writeByte(ATIVO);
        arquivo.readUnsignedShort();
        arquivo.seek(endereco + 3);
        arquivo.write(dados);
    }

    private void marcarComoExcluido(long endereco, int tamanho) throws Exception {
        arquivo.seek(endereco);
        arquivo.writeByte(EXCLUIDO);
        addDeleted(tamanho, endereco);
    }

    private void addDeleted(int tamanhoEspaco, long enderecoEspaco) throws Exception {
        if (tamanhoEspaco < Long.BYTES) return;

        arquivo.seek(4);
        long primeiro = arquivo.readLong();

        if (primeiro == -1L) {
            arquivo.seek(4);
            arquivo.writeLong(enderecoEspaco);
            arquivo.seek(enderecoEspaco + 3);
            arquivo.writeLong(-1L);
            return;
        }

        long anterior = -1L;
        long atual = primeiro;

        while (atual != -1L) {
            arquivo.seek(atual + 1);
            int tamanhoAtual = arquivo.readUnsignedShort();
            arquivo.seek(atual + 3);
            long proximo = arquivo.readLong();

            if (tamanhoAtual >= tamanhoEspaco) {
                if (anterior == -1L) {
                    arquivo.seek(4);
                    arquivo.writeLong(enderecoEspaco);
                } else {
                    arquivo.seek(anterior + 3);
                    arquivo.writeLong(enderecoEspaco);
                }
                arquivo.seek(enderecoEspaco + 3);
                arquivo.writeLong(atual);
                return;
            }

            anterior = atual;
            atual = proximo;
        }

        arquivo.seek(anterior + 3);
        arquivo.writeLong(enderecoEspaco);
        arquivo.seek(enderecoEspaco + 3);
        arquivo.writeLong(-1L);
    }

    private long getDeleted(int tamanhoNecessario) throws Exception {
        arquivo.seek(4);
        long atual = arquivo.readLong();
        long anterior = -1L;

        while (atual != -1L) {
            arquivo.seek(atual + 1);
            int tamanho = arquivo.readUnsignedShort();
            arquivo.seek(atual + 3);
            long proximo = arquivo.readLong();

            if (tamanho >= tamanhoNecessario) {
                if (anterior == -1L) {
                    arquivo.seek(4);
                    arquivo.writeLong(proximo);
                } else {
                    arquivo.seek(anterior + 3);
                    arquivo.writeLong(proximo);
                }
                return atual;
            }

            anterior = atual;
            atual = proximo;
        }
        return -1L;
    }

    private void validarTamanho(int tamanho) throws Exception {
        if (tamanho < 0 || tamanho > 65535) {
            throw new Exception("Registro excede o tamanho máximo suportado pelo arquivo.");
        }
    }

    private void validarRegistro(long posicao, int tamanho) throws Exception {
        if (tamanho < 0 || posicao + 3L + tamanho > arquivo.length()) {
            throw new Exception("Arquivo binário corrompido próximo à posição " + posicao + ".");
        }
    }

    public void close() throws Exception {
        arquivo.close();
    }
}
