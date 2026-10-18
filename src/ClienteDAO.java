import java.util.List;

public class ClienteDAO implements AutoCloseable {

    static final Object TRAVA_DADOS = new Object();
    private final Arquivo<Cliente> arquivoClientes;

    public ClienteDAO() throws Exception {
        arquivoClientes = new Arquivo<>("clientes", Cliente.class.getConstructor());
    }

    public Cliente buscarCliente(int id) throws Exception {
        synchronized (TRAVA_DADOS) {
            return arquivoClientes.read(id);
        }
    }

    public List<Cliente> listarClientes() throws Exception {
        synchronized (TRAVA_DADOS) {
            return arquivoClientes.readAll();
        }
    }

    public boolean incluirCliente(Cliente cliente) throws Exception {
        synchronized (TRAVA_DADOS) {
            validarCpfUnico(cliente, -1);
            return arquivoClientes.create(cliente) > 0;
        }
    }

    public boolean alterarCliente(Cliente cliente) throws Exception {
        synchronized (TRAVA_DADOS) {
            validarCpfUnico(cliente, cliente.getId());
            return arquivoClientes.update(cliente);
        }
    }

    public boolean excluirCliente(int id) throws Exception {
        synchronized (TRAVA_DADOS) {
            if (ClienteVooDAO.possuiAssociacoesPersistidasDoCliente(id)) {
                throw new IllegalStateException("O cliente possui voos associados. Remova as associações antes de excluí-lo.");
            }
            return arquivoClientes.delete(id);
        }
    }

    private void validarCpfUnico(Cliente cliente, int idIgnorado) throws Exception {
        if (cliente == null || cliente.getCpf() == null || cliente.getCpf().isBlank()) {
            throw new IllegalArgumentException("Informe o CPF do cliente.");
        }
        for (Cliente existente : arquivoClientes.readAll()) {
            if (existente.getId() != idIgnorado && existente.getCpf().equals(cliente.getCpf())) {
                throw new IllegalArgumentException("Já existe um cliente cadastrado com esse CPF.");
            }
        }
    }

    public void fechar() throws Exception { close(); }

    @Override
    public void close() throws Exception {
        synchronized (TRAVA_DADOS) {
            arquivoClientes.close();
        }
    }
}
