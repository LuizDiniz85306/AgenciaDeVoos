import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Árvore B+ persistida. As folhas guardam a FK e a lista de IDs dos registros
 * associados; os nós internos somente direcionam a busca até a folha correta.
 */
public class ArvoreBMais {

    private static final int MAGIC = 0x42504C31; // BPL1
    private static final int MAX_CHAVES = 3;

    private static class No {
        final boolean folha;
        final List<Integer> chaves = new ArrayList<>();
        final List<No> filhos = new ArrayList<>();
        final List<List<Integer>> valores = new ArrayList<>();
        No proximaFolha;

        No(boolean folha) {
            this.folha = folha;
        }
    }

    private static class NoGravado {
        No no;
        int[] filhos;
        int proximaFolha;
    }

    private final File arquivo;
    private No raiz;
    private final boolean carregadaDeArquivo;

    public ArvoreBMais(String caminho) throws IOException {
        arquivo = new File(caminho);
        boolean carregada = carregar();
        if (!carregada) {
            raiz = new No(true);
            persistir();
        }
        carregadaDeArquivo = carregada;
    }

    /** Indica se a árvore atual foi recuperada do seu arquivo binário. */
    public boolean foiCarregadaDeArquivo() {
        return carregadaDeArquivo;
    }

    public synchronized List<Integer> buscar(int chave) {
        No folha = localizarFolha(chave);
        int posicao = Collections.binarySearch(folha.chaves, chave);
        if (posicao < 0) {
            return Collections.emptyList();
        }
        return new ArrayList<>(folha.valores.get(posicao));
    }

    public synchronized void inserir(int chave, int idRegistro) throws IOException {
        TreeMap<Integer, TreeSet<Integer>> dados = extrairDados();
        dados.computeIfAbsent(chave, ignorado -> new TreeSet<>()).add(idRegistro);
        reconstruir(dados);
    }

    public synchronized void remover(int chave, int idRegistro) throws IOException {
        TreeMap<Integer, TreeSet<Integer>> dados = extrairDados();
        TreeSet<Integer> ids = dados.get(chave);
        if (ids == null || !ids.remove(idRegistro)) {
            return;
        }
        if (ids.isEmpty()) {
            dados.remove(chave);
        }
        reconstruir(dados);
    }

    public synchronized void reconstruir(Map<Integer, ? extends Iterable<Integer>> dados)
            throws IOException {
        raiz = new No(true);
        TreeMap<Integer, ? extends Iterable<Integer>> ordenados = new TreeMap<>(dados);
        for (Map.Entry<Integer, ? extends Iterable<Integer>> entrada : ordenados.entrySet()) {
            for (Integer idRegistro : entrada.getValue()) {
                inserirSemPersistir(entrada.getKey(), idRegistro);
            }
        }
        persistir();
    }

    /** Retorna uma cópia ordenada das entradas para auditoria de consistência. */
    public synchronized Map<Integer, List<Integer>> listarEntradas() {
        Map<Integer, List<Integer>> resultado = new TreeMap<>();
        for (Map.Entry<Integer, TreeSet<Integer>> entrada : extrairDados().entrySet()) {
            resultado.put(entrada.getKey(), new ArrayList<>(entrada.getValue()));
        }
        return resultado;
    }

    private void inserirSemPersistir(int chave, int idRegistro) {
        No folha = localizarFolha(chave);
        int posicao = Collections.binarySearch(folha.chaves, chave);
        if (posicao >= 0) {
            List<Integer> ids = folha.valores.get(posicao);
            if (!ids.contains(idRegistro)) {
                ids.add(idRegistro);
                Collections.sort(ids);
            }
            return;
        }

        posicao = -posicao - 1;
        folha.chaves.add(posicao, chave);
        List<Integer> ids = new ArrayList<>();
        ids.add(idRegistro);
        folha.valores.add(posicao, ids);
        if (folha.chaves.size() > MAX_CHAVES) {
            dividirFolha(folha);
        }
    }

    private No localizarFolha(int chave) {
        No atual = raiz;
        while (!atual.folha) {
            int filho = 0;
            while (filho < atual.chaves.size() && chave >= atual.chaves.get(filho)) {
                filho++;
            }
            atual = atual.filhos.get(filho);
        }
        return atual;
    }

    private void dividirFolha(No folha) {
        int meio = folha.chaves.size() / 2;
        No direita = new No(true);
        direita.chaves.addAll(folha.chaves.subList(meio, folha.chaves.size()));
        direita.valores.addAll(folha.valores.subList(meio, folha.valores.size()));
        folha.chaves.subList(meio, folha.chaves.size()).clear();
        folha.valores.subList(meio, folha.valores.size()).clear();
        direita.proximaFolha = folha.proximaFolha;
        folha.proximaFolha = direita;
        inserirNoPai(folha, direita.chaves.get(0), direita);
    }

    private void inserirNoPai(No esquerda, int separador, No direita) {
        No pai = encontrarPai(raiz, esquerda);
        if (pai == null) {
            No novaRaiz = new No(false);
            novaRaiz.chaves.add(separador);
            novaRaiz.filhos.add(esquerda);
            novaRaiz.filhos.add(direita);
            raiz = novaRaiz;
            return;
        }

        int posicaoFilho = pai.filhos.indexOf(esquerda);
        pai.chaves.add(posicaoFilho, separador);
        pai.filhos.add(posicaoFilho + 1, direita);
        if (pai.chaves.size() > MAX_CHAVES) {
            dividirInterno(pai);
        }
    }

    private void dividirInterno(No no) {
        int meio = no.chaves.size() / 2;
        int separador = no.chaves.get(meio);
        No direita = new No(false);
        direita.chaves.addAll(no.chaves.subList(meio + 1, no.chaves.size()));
        direita.filhos.addAll(no.filhos.subList(meio + 1, no.filhos.size()));
        no.chaves.subList(meio, no.chaves.size()).clear();
        no.filhos.subList(meio + 1, no.filhos.size()).clear();
        inserirNoPai(no, separador, direita);
    }

    private No encontrarPai(No atual, No filho) {
        if (atual.folha) {
            return null;
        }
        for (No candidato : atual.filhos) {
            if (candidato == filho) {
                return atual;
            }
            No pai = encontrarPai(candidato, filho);
            if (pai != null) {
                return pai;
            }
        }
        return null;
    }

    private TreeMap<Integer, TreeSet<Integer>> extrairDados() {
        TreeMap<Integer, TreeSet<Integer>> resultado = new TreeMap<>();
        No folha = raiz;
        while (!folha.folha) {
            folha = folha.filhos.get(0);
        }
        while (folha != null) {
            for (int i = 0; i < folha.chaves.size(); i++) {
                resultado.put(folha.chaves.get(i), new TreeSet<>(folha.valores.get(i)));
            }
            folha = folha.proximaFolha;
        }
        return resultado;
    }

    private boolean carregar() throws IOException {
        if (!arquivo.exists()) {
            return false;
        }
        Map<Integer, NoGravado> nos = new LinkedHashMap<>();
        int idRaiz;
        try (DataInputStream entrada = new DataInputStream(new BufferedInputStream(
                new FileInputStream(arquivo)))) {
            if (entrada.readInt() != MAGIC) {
                return false;
            }
            idRaiz = entrada.readInt();
            int quantidadeNos = entrada.readInt();
            for (int i = 0; i < quantidadeNos; i++) {
                int id = entrada.readInt();
                NoGravado gravado = new NoGravado();
                gravado.no = new No(entrada.readBoolean());
                int quantidadeChaves = entrada.readInt();
                for (int j = 0; j < quantidadeChaves; j++) {
                    gravado.no.chaves.add(entrada.readInt());
                }
                if (gravado.no.folha) {
                    for (int j = 0; j < quantidadeChaves; j++) {
                        int quantidadeIds = entrada.readInt();
                        List<Integer> ids = new ArrayList<>();
                        for (int k = 0; k < quantidadeIds; k++) {
                            ids.add(entrada.readInt());
                        }
                        gravado.no.valores.add(ids);
                    }
                    gravado.proximaFolha = entrada.readInt();
                } else {
                    int quantidadeFilhos = entrada.readInt();
                    gravado.filhos = new int[quantidadeFilhos];
                    for (int j = 0; j < quantidadeFilhos; j++) {
                        gravado.filhos[j] = entrada.readInt();
                    }
                }
                nos.put(id, gravado);
            }
        } catch (EOFException e) {
            return false;
        }

        for (NoGravado gravado : nos.values()) {
            if (gravado.no.folha) {
                if (gravado.proximaFolha >= 0) {
                    NoGravado proxima = nos.get(gravado.proximaFolha);
                    if (proxima == null || !proxima.no.folha) return false;
                    gravado.no.proximaFolha = proxima.no;
                }
            } else {
                for (int idFilho : gravado.filhos) {
                    NoGravado filho = nos.get(idFilho);
                    if (filho == null) return false;
                    gravado.no.filhos.add(filho.no);
                }
            }
        }
        NoGravado gravadoRaiz = nos.get(idRaiz);
        if (gravadoRaiz == null) {
            return false;
        }
        raiz = gravadoRaiz.no;
        return true;
    }

    private void persistir() throws IOException {
        Map<No, Integer> ids = new LinkedHashMap<>();
        ArrayDeque<No> fila = new ArrayDeque<>();
        fila.add(raiz);
        while (!fila.isEmpty()) {
            No no = fila.remove();
            if (ids.containsKey(no)) continue;
            ids.put(no, ids.size());
            if (!no.folha) fila.addAll(no.filhos);
        }

        try (DataOutputStream saida = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(arquivo)))) {
            saida.writeInt(MAGIC);
            saida.writeInt(ids.get(raiz));
            saida.writeInt(ids.size());
            for (Map.Entry<No, Integer> entrada : ids.entrySet()) {
                No no = entrada.getKey();
                saida.writeInt(entrada.getValue());
                saida.writeBoolean(no.folha);
                saida.writeInt(no.chaves.size());
                for (Integer chave : no.chaves) saida.writeInt(chave);
                if (no.folha) {
                    for (List<Integer> valores : no.valores) {
                        saida.writeInt(valores.size());
                        for (Integer valor : valores) saida.writeInt(valor);
                    }
                    saida.writeInt(no.proximaFolha == null ? -1 : ids.get(no.proximaFolha));
                } else {
                    saida.writeInt(no.filhos.size());
                    for (No filho : no.filhos) saida.writeInt(ids.get(filho));
                }
            }
        }
    }
}
