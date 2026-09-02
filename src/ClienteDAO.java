import java.util.List;

public class ClienteDAO {

    private Arquivo<Cliente> arquivoClientes;

    public ClienteDAO() throws Exception {

        arquivoClientes =
                new Arquivo<>(
                    "clientes",
                    Cliente.class.getConstructor()
                );
    }

    public Cliente buscarCliente(int id)
            throws Exception {

        return arquivoClientes.read(id);
    }

    public boolean incluirCliente(Cliente cliente)
            throws Exception {

        return arquivoClientes.create(cliente) > 0;
    }

    public boolean alterarCliente(Cliente cliente)
            throws Exception {

        return arquivoClientes.update(cliente);
    }

    public List<Cliente> listarClientes() throws Exception {
        return arquivoClientes.readAll();
    }

    public boolean excluirCliente(int id)
            throws Exception {

        return arquivoClientes.delete(id);
    }

    public void fechar() throws Exception {
        arquivoClientes.close();
    }
}