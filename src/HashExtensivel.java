import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Índice Hash Extensível persistido em arquivos binários. Cada chave aponta
 * para o endereço físico do registro no arquivo de dados.
 */
public class HashExtensivel {

    private static final int MAGIC = 0x48455831; // HEX1
    private static final int CAPACIDADE_BUCKET = 4;

    private static class Bucket {
        int profundidadeLocal;
        final Map<Integer, Long> pares = new HashMap<>();

        Bucket(int profundidadeLocal) {
            this.profundidadeLocal = profundidadeLocal;
        }
    }

    private final File arquivoDiretorio;
    private final File arquivoBuckets;
    private int profundidadeGlobal;
    private int[] diretorio;
    private final List<Bucket> buckets = new ArrayList<>();
    private final boolean carregadoDeArquivo;

    public HashExtensivel(String prefixo) throws IOException {
        arquivoDiretorio = new File(prefixo + ".dir.db");
        arquivoBuckets = new File(prefixo + ".buckets.db");
        boolean carregado = carregar();
        if (!carregado) {
            limpar();
            persistir();
        }
        carregadoDeArquivo = carregado;
    }

    /** Indica se o índice atual foi recuperado de arquivos binários existentes. */
    public boolean foiCarregadoDeArquivo() {
        return carregadoDeArquivo;
    }

    public synchronized Long read(int chave) {
        Bucket bucket = buckets.get(diretorio[indice(chave)]);
        return bucket.pares.get(chave);
    }

    public synchronized void create(int chave, long endereco) throws IOException {
        put(chave, endereco);
    }

    public synchronized void update(int chave, long endereco) throws IOException {
        put(chave, endereco);
    }

    public synchronized void delete(int chave) throws IOException {
        buckets.get(diretorio[indice(chave)]).pares.remove(chave);
        persistir();
    }

    public synchronized void limpar() {
        profundidadeGlobal = 1;
        diretorio = new int[] {0, 1};
        buckets.clear();
        buckets.add(new Bucket(1));
        buckets.add(new Bucket(1));
    }

    /** Quantidade de chaves atualmente mantidas no índice. */
    public synchronized int quantidadePares() {
        int quantidade = 0;
        for (Bucket bucket : buckets) {
            quantidade += bucket.pares.size();
        }
        return quantidade;
    }

    private void put(int chave, long endereco) throws IOException {
        while (true) {
            int indiceDiretorio = indice(chave);
            Bucket bucket = buckets.get(diretorio[indiceDiretorio]);

            if (bucket.pares.containsKey(chave)) {
                bucket.pares.put(chave, endereco);
                persistir();
                return;
            }

            if (bucket.pares.size() < CAPACIDADE_BUCKET) {
                bucket.pares.put(chave, endereco);
                persistir();
                return;
            }

            dividirBucket(diretorio[indiceDiretorio]);
        }
    }

    private void dividirBucket(int idBucket) {
        Bucket original = buckets.get(idBucket);
        if (original.profundidadeLocal == profundidadeGlobal) {
            int[] diretorioExpandido = new int[diretorio.length * 2];
            for (int i = 0; i < diretorio.length; i++) {
                diretorioExpandido[i] = diretorio[i];
                diretorioExpandido[i + diretorio.length] = diretorio[i];
            }
            diretorio = diretorioExpandido;
            profundidadeGlobal++;
        }

        int novaProfundidade = original.profundidadeLocal + 1;
        Bucket novo = new Bucket(novaProfundidade);
        int idNovo = buckets.size();
        buckets.add(novo);
        original.profundidadeLocal = novaProfundidade;

        for (int i = 0; i < diretorio.length; i++) {
            if (diretorio[i] == idBucket && ((i >>> (novaProfundidade - 1)) & 1) == 1) {
                diretorio[i] = idNovo;
            }
        }

        Map<Integer, Long> antigos = new HashMap<>(original.pares);
        original.pares.clear();
        for (Map.Entry<Integer, Long> par : antigos.entrySet()) {
            buckets.get(diretorio[indice(par.getKey())]).pares.put(par.getKey(), par.getValue());
        }
    }

    private int indice(int chave) {
        return (chave & 0x7fffffff) & ((1 << profundidadeGlobal) - 1);
    }

    private boolean carregar() throws IOException {
        if (!arquivoDiretorio.exists() || !arquivoBuckets.exists()) {
            return false;
        }

        try (DataInputStream entrada = new DataInputStream(new BufferedInputStream(
                new FileInputStream(arquivoDiretorio)))) {
            if (entrada.readInt() != MAGIC) {
                return false;
            }
            profundidadeGlobal = entrada.readInt();
            int quantidade = entrada.readInt();
            diretorio = new int[quantidade];
            for (int i = 0; i < quantidade; i++) {
                diretorio[i] = entrada.readInt();
            }
        } catch (EOFException e) {
            return false;
        }

        try (DataInputStream entrada = new DataInputStream(new BufferedInputStream(
                new FileInputStream(arquivoBuckets)))) {
            if (entrada.readInt() != MAGIC) {
                return false;
            }
            int quantidadeBuckets = entrada.readInt();
            buckets.clear();
            for (int i = 0; i < quantidadeBuckets; i++) {
                Bucket bucket = new Bucket(entrada.readInt());
                int quantidadePares = entrada.readInt();
                for (int j = 0; j < quantidadePares; j++) {
                    bucket.pares.put(entrada.readInt(), entrada.readLong());
                }
                buckets.add(bucket);
            }
        } catch (EOFException e) {
            return false;
        }

        for (int idBucket : diretorio) {
            if (idBucket < 0 || idBucket >= buckets.size()) {
                return false;
            }
        }
        return profundidadeGlobal > 0 && diretorio.length == (1 << profundidadeGlobal);
    }

    private void persistir() throws IOException {
        try (DataOutputStream saida = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(arquivoDiretorio)))) {
            saida.writeInt(MAGIC);
            saida.writeInt(profundidadeGlobal);
            saida.writeInt(diretorio.length);
            for (int idBucket : diretorio) {
                saida.writeInt(idBucket);
            }
        }

        try (DataOutputStream saida = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(arquivoBuckets)))) {
            saida.writeInt(MAGIC);
            saida.writeInt(buckets.size());
            for (Bucket bucket : buckets) {
                saida.writeInt(bucket.profundidadeLocal);
                saida.writeInt(bucket.pares.size());
                for (Map.Entry<Integer, Long> par : bucket.pares.entrySet()) {
                    saida.writeInt(par.getKey());
                    saida.writeLong(par.getValue());
                }
            }
        }
    }
}
